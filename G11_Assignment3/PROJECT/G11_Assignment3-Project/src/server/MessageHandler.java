package server;

import java.io.IOException;
import java.util.ArrayList;
import common.*;
import common.worker.GeneralParkWorker;
import DB.DatabaseController;
import ocsf.server.ConnectionToClient;

public class MessageHandler {

    public static void handle(ClientServerMessage msg, ConnectionToClient client,
                               DatabaseController db, BackEndServer server) {
        try {
            switch (msg.getCommand()) {
                case CLIENT_CONNECT:
                    String hostName = (String) msg.getData();
                    client.setInfo("HostName", hostName);
                    String connectIp = (String) client.getInfo("IP");
                    server.log("Client Connected - IP: " + connectIp + " | Host: " + hostName);
                    server.updateClientInUI(connectIp, hostName);
                    break;

                case TRAVELER_LOGIN:
                    String travelerId = (String) msg.getData();
                    Subscriber sub = db.getSubscriberByIdNumber(travelerId);
                    Traveler traveler = new Traveler();
                    traveler.setIdNumber(travelerId);
                    if (sub != null) {
                        traveler.setFirstName(sub.getFirstName());
                        traveler.setLastName(sub.getLastName());
                        traveler.setEmail(sub.getEmail());
                        traveler.setPhone(sub.getPhone());
                        traveler.setSubscriberId(sub.getSubscriberId());
                    } else {
                        // Not a subscriber - look up email from previous orders
                        java.util.ArrayList<Order> prevOrders = db.getOrdersByTravelerId(travelerId);
                        if (!prevOrders.isEmpty()) {
                            Order latest = prevOrders.get(0);
                            traveler.setEmail(latest.getEmail());
                            traveler.setPhone(latest.getPhone());
                        }
                    }
                    // Check if traveler is a registered guide
                    boolean isGuide = db.isRegisteredGuide(travelerId);
                    traveler.setGuide(isGuide);
                    respond(client, Command.SUCCESS, traveler);
                    break;

                case WORKER_LOGIN:
                    ArrayList<String> creds = msg.getDataAsArrayList();
                    GeneralParkWorker worker = db.workerLogin(creds.get(0), creds.get(1));
                    if (worker != null) {
                        client.setInfo("LoggedInEmployeeId", worker.getEmployeeId());
                        respond(client, Command.SUCCESS, worker);
                    } else {
                        respond(client, Command.FAILURE, "Invalid credentials or already logged in.");
                    }
                    break;

                case WORKER_LOGOUT:
                    int empId = (int) msg.getData();
                    db.workerLogout(empId);
                    client.setInfo("LoggedInEmployeeId", null);
                    respond(client, Command.SUCCESS, null);
                    break;

                case GET_ALL_PARKS:
                    ArrayList<Park> parks = db.getAllParks();
                    respond(client, Command.DATA_RESPONSE, parks);
                    break;

                case GET_PARK_AVAILABILITY:
                    ArrayList<String> avParams = msg.getDataAsArrayList();
                    int available = db.checkAvailability(
                        Integer.parseInt(avParams.get(0)), avParams.get(1), avParams.get(2));
                    respond(client, Command.DATA_RESPONSE, available);
                    break;

                case CREATE_ORDER:
                    Order newOrder = (Order) msg.getData();
                    int avail = db.checkAvailability(newOrder.getParkId(), newOrder.getVisitDate(), newOrder.getVisitTime());
                    if (avail >= newOrder.getNumVisitors()) {
                        Park park = db.getParkById(newOrder.getParkId());
                        boolean isSub = newOrder.getSubscriberId() > 0;
                        double price = Pricing.calculatePrice(newOrder.getOrderType(),
                            newOrder.getNumVisitors(), park.getFullPrice(), isSub, newOrder.isPaidInAdvance());
                        newOrder.setTotalPrice(price);
                        db.createOrder(newOrder);
                        respond(client, Command.SUCCESS, newOrder);
                    } else {
                        // Send failure with available spots so client can offer waitlist
                        ClientServerMessage failMsg = new ClientServerMessage(Command.FAILURE, "No availability. Available spots: " + avail);
                        failMsg.setSuccess(false);
                        client.sendToClient(failMsg);
                    }
                    break;

                case ADD_TO_WAITLIST:
                    Order waitOrder = (Order) msg.getData();
                    Park wlPark = db.getParkById(waitOrder.getParkId());
                    boolean wlSub = waitOrder.getSubscriberId() > 0;
                    double wlPrice = Pricing.calculatePrice(waitOrder.getOrderType(),
                        waitOrder.getNumVisitors(), wlPark.getFullPrice(), wlSub, waitOrder.isPaidInAdvance());
                    waitOrder.setTotalPrice(wlPrice);
                    waitOrder.setStatus("waitlist");
                    int wlOrderId = db.createWaitlistOrder(waitOrder);
                    waitOrder.setOrderId(wlOrderId);
                    respond(client, Command.SUCCESS, waitOrder);
                    break;

                case GET_ALL_ORDERS_BY_TRAVELER:
                    String visId = (String) msg.getData();
                    ArrayList<Order> orders = db.getOrdersByTravelerId(visId);
                    respond(client, Command.DATA_RESPONSE, orders);
                    break;

                case CANCEL_ORDER:
                    int cancelId = (int) msg.getData();
                    db.updateOrderStatus(cancelId, "cancelled");
                    respond(client, Command.SUCCESS, cancelId);
                    break;

                case CONFIRM_ORDER:
                    int confirmId = (int) msg.getData();
                    db.updateOrderStatus(confirmId, "confirmed");
                    respond(client, Command.SUCCESS, confirmId);
                    break;

                case WALKIN_ORDER:
                    Order walkinOrder = (Order) msg.getData();
                    Park wPark = db.getParkById(walkinOrder.getParkId());
                    if (wPark.getCurrentVisitors() < wPark.getMaxVisitors()) {
                        db.createOrder(walkinOrder);
                        db.recordEntry(walkinOrder.getOrderId(), walkinOrder.getParkId(),
                            walkinOrder.getVisitorId(), walkinOrder.getNumVisitors());
                        db.updateOrderStatus(walkinOrder.getOrderId(), "completed");
                        respond(client, Command.SUCCESS, walkinOrder);
                    } else {
                        respond(client, Command.FAILURE, "Park is full. No walk-in entry possible.");
                    }
                    break;

                case PROCESS_ENTRY:
                    ArrayList<Object> entryData = msg.getDataAsArrayList();
                    int eOrderId = (int) entryData.get(0);
                    int eParkId = (int) entryData.get(1);
                    String eVisitorId = (String) entryData.get(2);
                    int eNum = (int) entryData.get(3);
                    db.recordEntry(eOrderId, eParkId, eVisitorId, eNum);
                    db.updateOrderStatus(eOrderId, "completed");
                    Park entryPark = db.getParkById(eParkId);
                    respond(client, Command.SUCCESS, entryPark);
                    break;

                case PROCESS_EXIT:
                    ArrayList<Object> exitData = msg.getDataAsArrayList();
                    int xParkId = (int) exitData.get(0);
                    String xVisitorId = (String) exitData.get(1);
                    int xNum = (int) exitData.get(2);
                    db.recordExit(xParkId, xVisitorId, xNum);
                    Park exitPark = db.getParkById(xParkId);
                    respond(client, Command.SUCCESS, exitPark);
                    break;

                case GET_PARK_DETAILS:
                    int pId = (int) msg.getData();
                    Park p = db.getParkById(pId);
                    respond(client, Command.DATA_RESPONSE, p);
                    break;

                case REGISTER_GUIDE:
                    ArrayList<String> guideData = msg.getDataAsArrayList();
                    db.registerGuide(guideData.get(0), guideData.get(1), guideData.get(2), guideData.get(3), guideData.get(4));
                    respond(client, Command.SUCCESS, "Guide registered");
                    break;

                case REGISTER_SUBSCRIBER:
                    Subscriber newSub = (Subscriber) msg.getData();
                    int subId = db.registerSubscriber(newSub);
                    newSub.setSubscriberId(subId);
                    respond(client, Command.SUCCESS, newSub);
                    break;

                case REGISTER_EXISTING_AS_GUIDE:
                    String existingId = (String) msg.getData();
                    Subscriber existingSub = db.getSubscriberByIdNumber(existingId);
                    if (existingSub != null) {
                        db.registerGuide(existingSub.getIdNumber(), existingSub.getFirstName(),
                            existingSub.getLastName(), existingSub.getEmail(), existingSub.getPhone());
                        respond(client, Command.SUCCESS, "Registered as guide");
                    } else {
                        respond(client, Command.FAILURE, "Traveler not found. Must be a registered subscriber.");
                    }
                    break;

                case REQUEST_PARAMETER_CHANGE:
                    ArrayList<Object> paramRequests = msg.getDataAsArrayList();
                    for (Object req : paramRequests) {
                        ArrayList<Object> r = (ArrayList<Object>) req;
                        db.createParameterRequest((int)r.get(0), (String)r.get(1), ((Number)r.get(2)).doubleValue(), ((Number)r.get(3)).doubleValue(), (int)r.get(4));
                    }
                    respond(client, Command.SUCCESS, "Parameter change requests submitted.");
                    break;

                case GET_CHANGE_REQUESTS:
                    ArrayList<ArrayList<ArrayList<String>>> allRequests = new ArrayList<>();
                    allRequests.add(db.getPendingParameterRequests());
                    allRequests.add(db.getPendingPromotions());
                    respond(client, Command.DATA_RESPONSE, allRequests);
                    break;

                case APPROVE_CHANGE:
                    ArrayList<Object> approveData = msg.getDataAsArrayList();
                    db.approveParameterRequest((int)approveData.get(0), (int)approveData.get(1));
                    respond(client, Command.SUCCESS, "Parameter change approved.");
                    break;

                case REJECT_CHANGE:
                    ArrayList<Object> rejectData = msg.getDataAsArrayList();
                    db.rejectParameterRequest((int)rejectData.get(0), (int)rejectData.get(1));
                    respond(client, Command.SUCCESS, "Parameter change rejected.");
                    break;

                case CREATE_PROMOTION:
                    ArrayList<Object> promoData = msg.getDataAsArrayList();
                    db.createPromotion((int)promoData.get(0), ((Number)promoData.get(1)).doubleValue(),
                        (String)promoData.get(2), (String)promoData.get(3), (String)promoData.get(4), (int)promoData.get(5));
                    respond(client, Command.SUCCESS, "Promotion submitted.");
                    break;

                case APPROVE_PROMOTION:
                    ArrayList<Object> apData = msg.getDataAsArrayList();
                    db.approvePromotion((int)apData.get(0), (int)apData.get(1));
                    respond(client, Command.SUCCESS, "Promotion approved.");
                    break;
                case GET_WALKINS_TODAY:
                    int wtParkId = (int) msg.getData();
                    respond(client, Command.DATA_RESPONSE, db.getWalkinsToday(wtParkId));
                    break;

                case REJECT_PROMOTION:
                    ArrayList<Object> rpData = msg.getDataAsArrayList();
                    db.rejectPromotion((int)rpData.get(0), (int)rpData.get(1));
                    respond(client, Command.SUCCESS, "Promotion rejected.");
                    break;
                case GET_MY_PROMOTIONS:
                    int myPromoParkId = (int) msg.getData();
                    respond(client, Command.DATA_RESPONSE, db.getPromotionsByPark(myPromoParkId));
                    break;

                case SAVE_REPORT:
                    ArrayList<Object> saveParams = msg.getDataAsArrayList();
                    int saveParkId = ((Number) saveParams.get(0)).intValue();
                    String saveType = (String) saveParams.get(1);
                    int saveEmpId = ((Number) saveParams.get(2)).intValue();
                    int saveMonth = ((Number) saveParams.get(3)).intValue();
                    int saveYear = ((Number) saveParams.get(4)).intValue();
                    String saveData = (String) saveParams.get(5);
                    db.saveReport(saveParkId, saveType, saveEmpId, saveMonth, saveYear, saveData);
                    respond(client, Command.SUCCESS, "Report saved.");
                    break;

                case GENERATE_VISITS_REPORT:
                case GENERATE_CANCELLATION_REPORT:
                case GENERATE_TOTAL_VISITORS_REPORT:
                case GENERATE_USAGE_REPORT:
                    ArrayList<Object> reportParams = msg.getDataAsArrayList();
                    server.log("[Report] Command: " + msg.getCommand().name());
                    server.log("[Report] Params: parkId=" + reportParams.get(0) + " month=" + reportParams.get(1) + " year=" + reportParams.get(2));
                    int rParkId = ((Number) reportParams.get(0)).intValue();
                    int rMonth = ((Number) reportParams.get(1)).intValue();
                    int rYear = ((Number) reportParams.get(2)).intValue();
                    int rEmpId = ((Number) reportParams.get(3)).intValue();
                    server.log("[Report] Parsed: parkId=" + rParkId + " month=" + rMonth + " year=" + rYear);
                    String reportResult = db.generateReport(msg.getCommand().name(), rParkId, rMonth, rYear, rEmpId);
                    server.log("[Report] Result: " + reportResult);
                    respond(client, Command.DATA_RESPONSE, reportResult);
                    break;
                case GET_ALL_REPORTS:
                    int reportsParkId = (int) msg.getData();
                    respond(client, Command.DATA_RESPONSE, db.getReportsByPark(reportsParkId));
                    break;
                    
                    
                    
                case GET_MY_PARAMETER_REQUESTS:
                    int reqParkId = (int) msg.getData();
                    respond(client, Command.DATA_RESPONSE, db.getParameterRequestsByPark(reqParkId));
                    break;

                default:
                    respond(client, Command.ERROR, "Unknown command: " + msg.getCommand());
                    break;
            }
        } catch (Exception e) {
            server.log("Error: " + e.getMessage());
            try { respond(client, Command.ERROR, e.getMessage()); } catch (Exception ex) {}
        }
    }

    private static void respond(ConnectionToClient client, Command cmd, Object data) throws IOException {
        client.sendToClient(ClientServerMessage.success(cmd, data));
    }
}
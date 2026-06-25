package server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import common.*;
import common.worker.GeneralParkWorker;
import DB.DatabaseController;
import ocsf.server.ConnectionToClient;

/**
 * Stateless dispatcher that handles all client messages on the GoNature server.
 * <p>
 * Every message received by {@link BackEndServer} is forwarded to
 * {@link #handle(common.ClientServerMessage, ocsf.server.ConnectionToClient,
 * DB.DatabaseController, BackEndServer)}.
 * The method runs inside a {@code synchronized(DB_LOCK)} block to guarantee
 * thread safety when multiple clients are connected simultaneously.
 * </p>
 *
 * <p><b>Handled operation groups:</b></p>
 * <ul>
 *   <li><b>Authentication:</b> {@code TRAVELER_LOGIN}, {@code WORKER_LOGIN},
 *       {@code TRAVELER_LOGOUT}, {@code WORKER_LOGOUT}</li>
 *   <li><b>Booking:</b> {@code CREATE_ORDER}, {@code CANCEL_ORDER},
 *       {@code CONFIRM_ORDER}, {@code WALKIN_ORDER}, {@code ADD_TO_WAITLIST}</li>
 *   <li><b>Entry/Exit:</b> {@code PROCESS_ENTRY}, {@code PROCESS_EXIT},
 *       {@code TRAVELER_EXIT_VISIT}, {@code GET_ORDER_BY_CODE}</li>
 *   <li><b>Park management:</b> {@code REQUEST_PARAMETER_CHANGE},
 *       {@code APPROVE_CHANGE}, {@code REJECT_CHANGE},
 *       {@code CREATE_PROMOTION}, {@code APPROVE_PROMOTION}</li>
 *   <li><b>Reports:</b> {@code GENERATE_VISITS_REPORT},
 *       {@code GENERATE_CANCELLATION_REPORT},
 *       {@code GENERATE_TOTAL_VISITORS_REPORT}, {@code GENERATE_USAGE_REPORT}</li>
 *   <li><b>Notifications:</b> {@code GET_MY_NOTIFICATIONS},
 *       {@code MARK_NOTIFICATION_READ}</li>
 *   <li><b>User management:</b> {@code REGISTER_SUBSCRIBER},
 *       {@code REGISTER_GUIDE}, {@code UPDATE_SUBSCRIBER_PROFILE},
 *       {@code LOOKUP_SUBSCRIBER}, {@code GET_ACTIVE_VISIT}</li>
 * </ul>
 *
 * @author Group 11
 */
public class MessageHandler {

    // Track active traveler sessions — prevent same ID logging in twice
    private static final java.util.Set<String> activeTravelerSessions =
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * Releases an active traveler session when the traveler disconnects or logs out.
     * This allows the same traveler ID to log in again from another client.
     *
     * @param travelerId the traveler ID number to release from the active sessions set
     */
    public static void releaseTravelerSession(String travelerId) {
        if (travelerId != null) activeTravelerSessions.remove(travelerId);
    }

    /**
     * Dispatches an incoming client message to the appropriate server-side logic.
     * The method handles authentication, booking, waitlist management, entry and exit,
     * park management, reports, notifications, and user management commands.
     * It runs inside a synchronized database lock to protect the shared database connection
     * when multiple clients send requests at the same time.
     *
     * @param msg the message received from the client
     * @param client the client connection used to send the response
     * @param db the database controller for this server session
     * @param server the server instance used for logging server actions
     */
    public static void handle(ClientServerMessage msg, ConnectionToClient client,
                               DatabaseController db, BackEndServer server) {
        // Synchronize on DB_LOCK: OCSF runs one thread per client.
        // A single shared Connection is not thread-safe — this prevents corruption
        // when multiple clients send requests simultaneously.
        synchronized (DB.MySqlConnector.DB_LOCK) {
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
                    if (activeTravelerSessions.contains(travelerId)) {
                        respond(client, Command.FAILURE, "This ID is already logged in on another device.");
                        break;
                    }
                    activeTravelerSessions.add(travelerId);
                    client.setInfo("travelerSession", travelerId);
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

                case TRAVELER_LOGOUT:
                    String tlId = (String) msg.getData();
                    activeTravelerSessions.remove(tlId);
                    respond(client, Command.SUCCESS, "Logged out.");
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

                    db.expireOldWaitlistOffers();

                    if (db.hasActiveWaitlistForSlot(newOrder.getParkId(), newOrder.getVisitDate(), newOrder.getVisitTime())) {
                        ClientServerMessage failMsg = new ClientServerMessage(Command.FAILURE,
                            "There is already a waiting list for this park, date and time. Please join the waiting list.");
                        failMsg.setSuccess(false);
                        client.sendToClient(failMsg);
                        break;
                    }

                    Park park = db.getParkById(newOrder.getParkId());

                    if (park == null) {
                        respond(client, Command.ERROR, "Park not found.");
                        break;
                    }

                    // For organized groups, the booker IS the guide -> store their numeric guide_id
                    if ("organized_group".equals(newOrder.getOrderType())) {
                        int gId = db.getGuideIdByIdNumber(newOrder.getVisitorId());
                        if (gId > 0) newOrder.setGuideId(gId);
                    }

                    boolean isSub = newOrder.getSubscriberId() > 0;
                    double price = Pricing.calculatePrice(newOrder.getOrderType(),
                        newOrder.getNumVisitors(), park.getFullPrice(), isSub, newOrder.isPaidInAdvance());
                    // Apply any approved, active promotion for this park on top
                    double promo = db.getActivePromotionDiscount(newOrder.getParkId(), newOrder.getVisitDate());
                    if (promo > 0) {
                        price = Math.round(price * (1.0 - promo / 100.0) * 100.0) / 100.0;
                    }
                    newOrder.setTotalPrice(price);

                    Order booked = db.bookOrderAtomic(newOrder);

                    if (booked != null) {
                        respond(client, Command.SUCCESS, booked);
                    } else {
                        int avail = db.checkAvailability(newOrder.getParkId(), newOrder.getVisitDate(), newOrder.getVisitTime());
                        ClientServerMessage failMsg = new ClientServerMessage(Command.FAILURE,
                            "No availability. Available spots: " + Math.max(0, avail));
                        failMsg.setSuccess(false);
                        client.sendToClient(failMsg);
                    }

                    break;

                case ADD_TO_WAITLIST:
                    Order waitOrder = (Order) msg.getData();
                    Park wlPark = db.getParkById(waitOrder.getParkId());
                    // For organized groups, set guide_id same as CREATE_ORDER
                    if ("organized_group".equals(waitOrder.getOrderType())) {
                        int wlGId = db.getGuideIdByIdNumber(waitOrder.getVisitorId());
                        if (wlGId > 0) waitOrder.setGuideId(wlGId);
                    }
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

                    int promoted = db.cancelOrderAndProcessWaitlist(cancelId);

                    if (promoted > 0) {
                        server.log("[Waitlist] Promoted " + promoted + " waitlist order(s) to pending.");
                    }

                    respond(client, Command.SUCCESS, cancelId);
                    break;

                case CONFIRM_ORDER:
                    int confirmId = (int) msg.getData();

                    // Try waitlist-offer confirmation first (pending -> confirmed)
                    if (db.confirmWaitlistOffer(confirmId)) {
                        respond(client, Command.SUCCESS, confirmId);
                    } else if (db.confirmReminder(confirmId)) {
                        // Otherwise this is a day-before reminder confirmation
                        respond(client, Command.SUCCESS, confirmId);
                    } else {
                        respond(client, Command.FAILURE, "Nothing to confirm for this order.");
                    }

                    break;

                case WALKIN_ORDER:
                    Order walkinOrder = (Order) msg.getData();
                    Park wPark = db.getParkById(walkinOrder.getParkId());
                    // Atomic walk-in so concurrent walk-ins can't exceed the gap
                    // Check duplicate before calling atomic (gives clearer message)
                    if (db.hasActiveWalkInToday(walkinOrder.getParkId(), walkinOrder.getVisitorId())) {
                        respond(client, Command.FAILURE, "This visitor already has a walk-in entry today at this park.");
                        break;
                    }
                    boolean walkinOk = db.walkInAtomic(walkinOrder, wPark);
                    if (walkinOk) {
                        respond(client, Command.SUCCESS, walkinOrder);
                    } else {
                        int used = db.getWalkinsToday(walkinOrder.getParkId());
                        int leftover = wPark.getGapForWalkins() - used;
                        if (walkinOrder.getNumVisitors() > leftover) {
                            respond(client, Command.FAILURE, "Not enough walk-in spots. Available: " + Math.max(0, leftover));
                        } else {
                            respond(client, Command.FAILURE, "Park is full. No walk-in entry possible.");
                        }
                    }
                    break;

                case PROCESS_ENTRY:
                    ArrayList<Object> entryData = msg.getDataAsArrayList();
                    int eOrderId = (int) entryData.get(0);
                    int eParkId = (int) entryData.get(1);
                    String eVisitorId = (String) entryData.get(2);
                    int eNum = (int) entryData.get(3);
                    db.recordEntry(eOrderId, eParkId, eVisitorId, eNum);
                    db.updateOrderStatus(eOrderId, "in_park");  // currently inside (not yet finished)
                    Park entryPark = db.getParkById(eParkId);
                    respond(client, Command.SUCCESS, entryPark);
                    break;

                case PROCESS_EXIT:
                    ArrayList<Object> exitData = msg.getDataAsArrayList();
                    int xParkId = (int) exitData.get(0);
                    String xVisitorId = (String) exitData.get(1);
                    int xNum = (int) exitData.get(2);
                    boolean exited = db.recordExit(xParkId, xVisitorId, xNum);
                    if (exited) {
                        Park exitPark = db.getParkById(xParkId);
                        respond(client, Command.SUCCESS, exitPark);
                    } else {
                        respond(client, Command.FAILURE, "No active visit found for this visitor. They may have already exited or never entered.");
                    }
                    break;

                case GET_PARK_DETAILS:
                    int pId = (int) msg.getData();
                    Park p = db.getParkById(pId);
                    respond(client, Command.DATA_RESPONSE, p);
                    break;

                case REGISTER_GUIDE:
                    ArrayList<String> guideData = msg.getDataAsArrayList();
                    try {
                        db.registerGuide(guideData.get(0), guideData.get(1), guideData.get(2), guideData.get(3), guideData.get(4));
                        respond(client, Command.SUCCESS, "Guide registered");
                    } catch (SQLException ex) {
                        respond(client, Command.FAILURE, ex.getMessage());
                    }
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
                    ArrayList<Object> paramData = msg.getDataAsArrayList();
                    // Single request:
                    // [parkId, parameterName, oldValue, newValue, requestedBy]
                    if (!paramData.isEmpty() && !(paramData.get(0) instanceof ArrayList)) {
                        int reqParkId   = ((Number) paramData.get(0)).intValue();
                        String reqParam = (String) paramData.get(1);
                        double reqNewVal = ((Number) paramData.get(3)).doubleValue();

                        // Validate new value against business rules
                        String validErr = db.validateParameterValue(reqParkId, reqParam, reqNewVal);
                        if (validErr != null) {
                            respond(client, Command.FAILURE, validErr);
                            break;
                        }

                        db.createParameterRequest(
                            reqParkId, reqParam,
                            ((Number) paramData.get(2)).doubleValue(),
                            reqNewVal,
                            ((Number) paramData.get(4)).intValue()
                        );
                        respond(client, Command.SUCCESS, "Parameter change request submitted.");
                        break;
                    }
                    // Multiple requests, in case another screen sends list of lists
                    for (Object req : paramData) {
                        ArrayList<Object> r = (ArrayList<Object>) req;
                        db.createParameterRequest(
                            ((Number) r.get(0)).intValue(),
                            (String) r.get(1),
                            ((Number) r.get(2)).doubleValue(),
                            ((Number) r.get(3)).doubleValue(),
                            ((Number) r.get(4)).intValue()
                        );
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
                    try {
                        db.approveParameterRequest((int)approveData.get(0), (int)approveData.get(1));
                        respond(client, Command.SUCCESS, "Parameter change approved.");
                    } catch (SQLException approveEx) {
                        respond(client, Command.FAILURE, approveEx.getMessage());
                    }
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
                case GET_ALTERNATIVE_SLOTS:
                    ArrayList<Object> altData = msg.getDataAsArrayList();
                    int altParkId = ((Number) altData.get(0)).intValue();
                    String altDate = (String) altData.get(1);
                    String altTime = (String) altData.get(2);
                    int altNum    = ((Number) altData.get(3)).intValue();
                    java.util.ArrayList<java.util.ArrayList<String>> altSlots =
                        db.getAlternativeSlots(altParkId, altDate, altTime, altNum);
                    server.log("[Server] GET_ALTERNATIVE_SLOTS returning " + altSlots.size() + " slots for park="
                        + altParkId + " date=" + altDate + " time=" + altTime);
                    // Use ALT_SLOTS_RESPONSE so pollingHandler doesn't intercept it
                    respond(client, Command.ALT_SLOTS_RESPONSE, altSlots);
                    break;

                case GET_ORDER_BY_CODE:
                    String confCode = (String) msg.getData();
                    Order orderByCode = db.getOrderByConfirmationCode(confCode);
                    if (orderByCode != null) respond(client, Command.DATA_RESPONSE, orderByCode);
                    else respond(client, Command.FAILURE, "No order found with code: " + confCode);
                    break;

                case GET_MY_NOTIFICATIONS:
                    String notifVisitorId = (String) msg.getData();
                    respond(client, Command.DATA_RESPONSE, db.getUnreadNotifications(notifVisitorId));
                    break;

                case MARK_NOTIFICATION_READ:
                    int notifId = (int) msg.getData();
                    db.markNotificationRead(notifId);
                    respond(client, Command.SUCCESS, notifId);
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

                case GET_ACTIVE_VISIT: {
                    String avId = (String) msg.getData();
                    Order activeOrder = db.getActiveVisit(avId);
                    if (activeOrder != null) {
                        respond(client, Command.DATA_RESPONSE, activeOrder);
                    } else {
                        respond(client, Command.FAILURE, "NO_ACTIVE_VISIT");
                    }
                    break;
                }

                case TRAVELER_EXIT_VISIT: {
                    String exitId = (String) msg.getData();
                    String result = db.travelerExit(exitId);
                    if ("SUCCESS".equals(result)) {
                        respond(client, Command.SUCCESS, "Exit recorded successfully.");
                    } else if ("ALREADY_EXITED".equals(result)) {
                        respond(client, Command.FAILURE, "Visit already exited.");
                    } else {
                        respond(client, Command.FAILURE, "No active visit found.");
                    }
                    break;
                }

                case LOOKUP_SUBSCRIBER: {
                    String lookupId = (String) msg.getData();
                    // Search subscribers first
                    Subscriber lookupResult = db.getSubscriberByIdNumber(lookupId);
                    if (lookupResult != null) {
                        respond(client, Command.SUCCESS, lookupResult);
                    } else {
                        // Try employees (by employee_id)
                        GeneralParkWorker lookupWorker = db.getEmployeeById(lookupId);
                        if (lookupWorker != null) {
                            respond(client, Command.SUCCESS, lookupWorker);
                        } else {
                            respond(client, Command.FAILURE, "No subscriber or employee found with this ID.");
                        }
                    }
                    break;
                }

                case UPDATE_SUBSCRIBER_PROFILE: {
                    java.util.ArrayList<Object> pd = msg.getDataAsArrayList();
                    db.updateSubscriberProfile((String)pd.get(0), (String)pd.get(1), (String)pd.get(2), (String)pd.get(3));
                    respond(client, Command.SUCCESS, "Profile updated");
                    break;
                }

                default:
                    respond(client, Command.ERROR, "Unknown command: " + msg.getCommand());
                    break;
            }
        } catch (Exception e) {
            server.log("Error: " + e.getMessage());
            try { respond(client, Command.ERROR, e.getMessage()); } catch (Exception ex) {}
        }
        } // end synchronized(DB_LOCK)
    }

    /**
     * Sends a response message to a connected client.
     * The response is wrapped as a successful ClientServerMessage with the given command and data.
     *
     * @param client the client connection that should receive the response
     * @param cmd the response command
     * @param data the response data
     * @throws IOException if the response cannot be sent to the client
     */
    private static void respond(ConnectionToClient client, Command cmd, Object data) throws IOException {
        client.sendToClient(ClientServerMessage.success(cmd, data));
    }
}
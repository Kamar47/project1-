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
                    server.log("Client identified: " + hostName);
                    if (server != null) {
                        String ip = (String) client.getInfo("IP");
                        server.log("Client Connected - IP: " + ip + " | Host: " + hostName);
                    }
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
                    }
                    respond(client, Command.SUCCESS, traveler);
                    break;

                case WORKER_LOGIN:
                    ArrayList<String> creds = msg.getDataAsArrayList();
                    GeneralParkWorker worker = db.workerLogin(creds.get(0), creds.get(1));
                    if (worker != null) {
                        respond(client, Command.SUCCESS, worker);
                    } else {
                        respond(client, Command.FAILURE, "Invalid credentials or already logged in.");
                    }
                    break;

                case WORKER_LOGOUT:
                    int empId = (int) msg.getData();
                    db.workerLogout(empId);
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
                        respond(client, Command.FAILURE, "No availability. Available spots: " + avail);
                    }
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

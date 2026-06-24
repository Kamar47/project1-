package DB;
import java.sql.Statement;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import common.*;
import common.worker.GeneralParkWorker;

public class DatabaseController {
    private Connection conn;

    public DatabaseController(Connection conn) { this.conn = conn; }

    // === PARKS ===
    public ArrayList<Park> getAllParks() throws SQLException {
        ArrayList<Park> parks = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM parks")) {
            while (rs.next()) {
                parks.add(new Park(rs.getInt("park_id"), rs.getString("park_name"),
                    rs.getInt("max_visitors"), rs.getInt("gap_for_walkins"),
                    rs.getDouble("estimated_visit_duration"), rs.getInt("current_visitors"),
                    rs.getDouble("full_price")));
            }
        }
        return parks;
    }

    public Park getParkById(int parkId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM parks WHERE park_id = ?");
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        Park park = null;
        if (rs.next()) {
            park = new Park(rs.getInt("park_id"), rs.getString("park_name"),
                rs.getInt("max_visitors"), rs.getInt("gap_for_walkins"),
                rs.getDouble("estimated_visit_duration"), rs.getInt("current_visitors"),
                rs.getDouble("full_price"));
        }
        rs.close(); ps.close();
        return park;
    }

    public void updateParkVisitors(int parkId, int change) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE parks SET current_visitors = current_visitors + ? WHERE park_id = ?");
        ps.setInt(1, change); ps.setInt(2, parkId);
        ps.executeUpdate(); ps.close();
    }

    // === ORDERS ===
    public int createOrder(Order order) throws SQLException {
        String code = "CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // If visit is today, mark reminder as already sent+confirmed — no reminder needed
        boolean isToday = order.getVisitDate().equals(java.time.LocalDate.now().toString());

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO orders (visitor_id, park_id, visit_date, visit_time, num_visitors, email, phone, " +
            "order_type, status, confirmation_code, guide_id, subscriber_id, is_paid_in_advance, total_price, " +
            "reminder_sent, reminder_confirmed) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, order.getVisitorId()); ps.setInt(2, order.getParkId());
        ps.setDate(3, Date.valueOf(order.getVisitDate())); ps.setString(4, order.getVisitTime());
        ps.setInt(5, order.getNumVisitors()); ps.setString(6, order.getEmail());
        ps.setString(7, order.getPhone()); ps.setString(8, order.getOrderType());
        ps.setString(9, "confirmed"); ps.setString(10, code);
        if (order.getGuideId() > 0) ps.setInt(11, order.getGuideId()); else ps.setNull(11, java.sql.Types.INTEGER);
        if (order.getSubscriberId() > 0) ps.setInt(12, order.getSubscriberId()); else ps.setNull(12, java.sql.Types.INTEGER);
        ps.setBoolean(13, order.isPaidInAdvance()); ps.setDouble(14, order.getTotalPrice());
        ps.setBoolean(15, isToday); // reminder_sent
        ps.setBoolean(16, isToday); // reminder_confirmed
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        int id = keys.next() ? keys.getInt(1) : -1;
        order.setOrderId(id); order.setConfirmationCode(code); order.setStatus("confirmed");
        order.setReminderSent(isToday); order.setReminderConfirmed(isToday);
        keys.close(); ps.close();
        return id;
    }

    public ArrayList<Order> getOrdersByTravelerId(String travelerId) throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT o.*, p.park_name FROM orders o JOIN parks p ON o.park_id = p.park_id WHERE o.visitor_id = ? ORDER BY o.visit_date DESC");
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Order o = extractOrder(rs);
            o.setParkName(rs.getString("park_name"));
            orders.add(o);
        }
        rs.close(); ps.close();
        return orders;
    }

    public boolean updateOrderStatus(int orderId, String status) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?");
        ps.setString(1, status); ps.setInt(2, orderId);
        int rows = ps.executeUpdate(); ps.close();
        return rows > 0;
    }
    
    public ArrayList<String> getAllReports() throws SQLException {
        ArrayList<String> reports = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT r.report_id, r.report_type, p.park_name, r.report_month, r.report_year, r.report_data, r.created_at " +
            "FROM reports r JOIN parks p ON r.park_id = p.park_id ORDER BY r.created_at DESC");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            reports.add("[#" + rs.getInt("report_id") + "] " + rs.getString("report_type") + " - " +
                rs.getString("park_name") + " | Month " + rs.getInt("report_month") + "/" + rs.getInt("report_year") +
                " | Created: " + rs.getTimestamp("created_at") + "\n" + rs.getString("report_data"));
        }
        rs.close(); ps.close();
        return reports;
    }
    
    public ArrayList<ArrayList<String>> getParameterRequestsByPark(int parkId) throws SQLException {
        ArrayList<ArrayList<String>> requests = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT parameter_name, old_value, new_value, status, created_at FROM parameter_requests WHERE park_id = ? ORDER BY created_at DESC");
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            row.add(rs.getString("parameter_name"));
            row.add(String.valueOf(rs.getDouble("old_value")));
            row.add(String.valueOf(rs.getDouble("new_value")));
            row.add(rs.getString("status"));
            row.add(rs.getTimestamp("created_at").toString());
            requests.add(row);
        }
        rs.close(); ps.close();
        return requests;
    }

    public int checkAvailability(int parkId, String date, String time) throws SQLException {
        Park park = getParkById(parkId);
        if (park == null) return 0;
        // Capacity is per time-slot using the park's estimated visit duration (default 4h).
        // Two bookings overlap if their [start, start+duration) windows intersect.
        // Overlap test: existing.start < requested.end AND existing.end > requested.start
        double durationHours = park.getEstimatedVisitDuration() > 0 ? park.getEstimatedVisitDuration() : 4;
        int durationSecs = (int) (durationHours * 3600);
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COALESCE(SUM(num_visitors), 0) AS booked FROM orders " +
            "WHERE park_id = ? AND visit_date = ? AND status IN ('confirmed', 'in_park', 'pending') " +
            "AND TIME(visit_time) < ADDTIME(TIME(?), SEC_TO_TIME(?)) " +
            "AND ADDTIME(TIME(visit_time), SEC_TO_TIME(?)) > TIME(?)");
        ps.setInt(1, parkId);
        ps.setDate(2, Date.valueOf(date));
        ps.setString(3, time);
        ps.setInt(4, durationSecs);
        ps.setInt(5, durationSecs);
        ps.setString(6, time);
        ResultSet rs = ps.executeQuery();
        int booked = rs.next() ? rs.getInt("booked") : 0;
        rs.close(); ps.close();
        return (park.getMaxVisitors() - park.getGapForWalkins()) - booked;
    }

    // === WORKERS ===
    public GeneralParkWorker workerLogin(String username, String password) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM employees WHERE username = ? AND password = ?");
        ps.setString(1, username); ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        GeneralParkWorker worker = null;
        if (rs.next()) {
            if (rs.getBoolean("is_logged_in")) { rs.close(); ps.close(); return null; }
            worker = extractWorker(rs);
            PreparedStatement up = conn.prepareStatement("UPDATE employees SET is_logged_in = TRUE WHERE employee_id = ?");
            up.setInt(1, worker.getEmployeeId()); up.executeUpdate(); up.close();
            worker.setLoggedIn(true);
        }
        rs.close(); ps.close();
        return worker;
    }

    public void workerLogout(int employeeId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE employees SET is_logged_in = FALSE WHERE employee_id = ?");
        ps.setInt(1, employeeId); ps.executeUpdate(); ps.close();
    }

    // === SUBSCRIBERS ===
    // === GUIDES ===
    public boolean isRegisteredGuide(String idNumber) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT guide_id FROM guides WHERE id_number = ?");
        ps.setString(1, idNumber);
        ResultSet rs = ps.executeQuery();
        boolean found = rs.next();
        rs.close(); ps.close();
        return found;
    }
    public ArrayList<ArrayList<String>> getPromotionsByPark(int parkId) throws SQLException {
        ArrayList<ArrayList<String>> promos = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT promo_id, discount_percentage, start_date, end_date, description, status " +
            "FROM promotions WHERE park_id = ? ORDER BY created_at DESC");
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            row.add(String.valueOf(rs.getInt("promo_id")));
            row.add(String.valueOf(rs.getDouble("discount_percentage")));
            row.add(rs.getDate("start_date").toString());
            row.add(rs.getDate("end_date").toString());
            row.add(rs.getString("description") != null ? rs.getString("description") : "");
            row.add(rs.getString("status"));
            promos.add(row);
        }
        rs.close(); ps.close();
        return promos;
    }
    
    
    
    public ArrayList<ArrayList<String>> getReportsByPark(int parkId) throws SQLException {
        ArrayList<ArrayList<String>> reports = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT report_id, report_type, report_month, report_year, created_at, report_data " +
            "FROM reports WHERE park_id = ? ORDER BY created_at DESC");
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            row.add(String.valueOf(rs.getInt("report_id")));
            row.add(rs.getString("report_type"));
            row.add(String.valueOf(rs.getInt("report_month")));
            row.add(String.valueOf(rs.getInt("report_year")));
            row.add(rs.getTimestamp("created_at").toString());
            row.add(rs.getString("report_data") != null ? rs.getString("report_data") : "");
            reports.add(row);
        }
        rs.close(); ps.close();
        return reports;
    }

    public void registerGuide(String idNumber, String firstName, String lastName, String email, String phone) throws SQLException {
        // Prevent duplicate guide registration
        PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM guides WHERE id_number = ?");
        check.setString(1, idNumber);
        ResultSet crs = check.executeQuery();
        boolean exists = crs.next() && crs.getInt(1) > 0;
        crs.close(); check.close();
        if (exists) throw new SQLException("Guide already exists with ID: " + idNumber);

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO guides (id_number, first_name, last_name, email, phone) VALUES (?,?,?,?,?)");
        ps.setString(1, idNumber); ps.setString(2, firstName); ps.setString(3, lastName);
        ps.setString(4, email); ps.setString(5, phone);
        ps.executeUpdate(); ps.close();
    }

    // === SUBSCRIBERS ===
    public int registerSubscriber(Subscriber sub) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO subscribers (id_number, first_name, last_name, phone, email, family_members, credit_card) VALUES (?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, sub.getIdNumber()); ps.setString(2, sub.getFirstName()); ps.setString(3, sub.getLastName());
        ps.setString(4, sub.getPhone()); ps.setString(5, sub.getEmail());
        ps.setInt(6, sub.getFamilyMembers()); ps.setString(7, sub.getCreditCard());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        int id = keys.next() ? keys.getInt(1) : -1;
        keys.close(); ps.close();
        return id;
    }

    public Subscriber getSubscriberByIdNumber(String idNumber) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM subscribers WHERE id_number = ?");
        ps.setString(1, idNumber);
        ResultSet rs = ps.executeQuery();
        Subscriber sub = null;
        if (rs.next()) {
            sub = new Subscriber();
            sub.setSubscriberId(rs.getInt("subscriber_id")); sub.setIdNumber(rs.getString("id_number"));
            sub.setFirstName(rs.getString("first_name")); sub.setLastName(rs.getString("last_name"));
            sub.setPhone(rs.getString("phone")); sub.setEmail(rs.getString("email"));
            sub.setFamilyMembers(rs.getInt("family_members")); sub.setCreditCard(rs.getString("credit_card"));
        }
        rs.close(); ps.close();
        return sub;
    }

    // === PARK VISITS (Entry/Exit) ===
    public void recordEntry(int orderId, int parkId, String visitorId, int numVisitors) throws SQLException {
        recordEntry(orderId, parkId, visitorId, numVisitors, orderId > 0 ? "reserved" : "walk_in");
    }

    public void recordEntry(int orderId, int parkId, String visitorId, int numVisitors, String visitType) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO park_visits (order_id, park_id, visitor_id, num_visitors, entry_time, visit_type) VALUES (?,?,?,?,NOW(),?)");
        ps.setInt(1, orderId); ps.setInt(2, parkId); ps.setString(3, visitorId);
        ps.setInt(4, numVisitors); ps.setString(5, visitType);
        ps.executeUpdate(); ps.close();
        updateParkVisitors(parkId, numVisitors);
    }

    public boolean recordExit(int parkId, String visitorId, int numVisitors) throws SQLException {
        // Get the actual num_visitors from the DB — do NOT trust the client-supplied number
        PreparedStatement psGet = conn.prepareStatement(
            "SELECT num_visitors FROM park_visits WHERE park_id = ? AND visitor_id = ? " +
            "AND exit_time IS NULL ORDER BY entry_time DESC LIMIT 1");
        psGet.setInt(1, parkId); psGet.setString(2, visitorId);
        ResultSet rsGet = psGet.executeQuery();
        int actualNum = rsGet.next() ? rsGet.getInt("num_visitors") : -1;
        rsGet.close(); psGet.close();

        if (actualNum < 0) return false; // no active visit

        // Close the visit
        PreparedStatement ps1 = conn.prepareStatement(
            "UPDATE park_visits SET exit_time = NOW() WHERE park_id = ? AND visitor_id = ? " +
            "AND exit_time IS NULL ORDER BY entry_time DESC LIMIT 1");
        ps1.setInt(1, parkId); ps1.setString(2, visitorId);
        int rows = ps1.executeUpdate(); ps1.close();
        if (rows == 0) return false;

        // Mark order completed
        PreparedStatement psC = conn.prepareStatement(
            "UPDATE orders o JOIN park_visits v ON o.order_id = v.order_id " +
            "SET o.status = 'completed' " +
            "WHERE v.park_id = ? AND v.visitor_id = ? AND o.status = 'in_park'");
        psC.setInt(1, parkId); psC.setString(2, visitorId);
        psC.executeUpdate(); psC.close();

        // Decrease by ACTUAL num_visitors from DB, not client-supplied
        PreparedStatement ps2 = conn.prepareStatement(
            "UPDATE parks SET current_visitors = GREATEST(0, current_visitors - ?) WHERE park_id = ?");
        ps2.setInt(1, actualNum); ps2.setInt(2, parkId);
        ps2.executeUpdate(); ps2.close();
        return true;
    }


    // === WAITLIST ===
    public int createWaitlistOrder(Order order) throws SQLException {
        // Create the order with waitlist status
        String code = "WL-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO orders (visitor_id, park_id, visit_date, visit_time, num_visitors, email, phone, " +
            "order_type, status, confirmation_code, guide_id, subscriber_id, is_paid_in_advance, total_price) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, order.getVisitorId()); ps.setInt(2, order.getParkId());
        ps.setDate(3, Date.valueOf(order.getVisitDate())); ps.setString(4, order.getVisitTime());
        ps.setInt(5, order.getNumVisitors()); ps.setString(6, order.getEmail());
        ps.setString(7, order.getPhone()); ps.setString(8, order.getOrderType());
        ps.setString(9, "waitlist"); ps.setString(10, code);
        if (order.getGuideId() > 0) ps.setInt(11, order.getGuideId()); else ps.setNull(11, java.sql.Types.INTEGER);
        if (order.getSubscriberId() > 0) ps.setInt(12, order.getSubscriberId()); else ps.setNull(12, java.sql.Types.INTEGER);
        ps.setBoolean(13, order.isPaidInAdvance());
        ps.setDouble(14, order.getTotalPrice());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        int orderId = keys.next() ? keys.getInt(1) : -1;
        keys.close(); ps.close();
        
        

        // Add to waitlist table
        PreparedStatement wps = conn.prepareStatement(
            "INSERT INTO waitlist (order_id, park_id, visit_date, visit_time, position, status) " +
            "VALUES (?, ?, ?, ?, (SELECT COALESCE(MAX(w2.position), 0) + 1 FROM waitlist w2 WHERE w2.park_id = ? AND w2.visit_date = ?), 'waiting')");
        wps.setInt(1, orderId); wps.setInt(2, order.getParkId());
        wps.setDate(3, Date.valueOf(order.getVisitDate())); wps.setString(4, order.getVisitTime());
        wps.setInt(5, order.getParkId()); wps.setDate(6, Date.valueOf(order.getVisitDate()));
        wps.executeUpdate(); wps.close();

        order.setOrderId(orderId); order.setConfirmationCode(code);
        return orderId;
    }
    private String normalizeTime(String time) {
        if (time == null) {
            return "";
        }

        time = time.trim();

        if (time.length() == 5) {
            return time + ":00";
        }

        return time;
    }
    public int getWaitingVisitorsForSlot(int parkId, String date, String time) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COALESCE(SUM(o.num_visitors), 0) AS total_waiting " +
            "FROM waitlist w " +
            "JOIN orders o ON w.order_id = o.order_id " +
            "WHERE w.park_id = ? " +
            "AND w.visit_date = ? " +
            "AND TIME(w.visit_time) = TIME(?) " +
            "AND w.status = 'waiting' " +
            "AND o.status = 'waitlist'"
        );

        ps.setInt(1, parkId);
        ps.setDate(2, Date.valueOf(date));
        ps.setString(3, normalizeTime(time));

        ResultSet rs = ps.executeQuery();

        int total = 0;
        if (rs.next()) {
            total = rs.getInt("total_waiting");
        }

        rs.close();
        ps.close();

        return total;
    }
    public boolean hasActiveWaitlistForSlot(int parkId, String date, String time) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM waitlist w " +
            "JOIN orders o ON w.order_id = o.order_id " +
            "WHERE w.park_id = ? " +
            "AND w.visit_date = ? " +
            "AND TIME(w.visit_time) = TIME(?) " +
            "AND w.status = 'waiting' " +
            "AND o.status = 'waitlist'"
        );

        ps.setInt(1, parkId);
        ps.setDate(2, Date.valueOf(date));
        ps.setString(3, normalizeTime(time));

        ResultSet rs = ps.executeQuery();

        boolean exists = rs.next() && rs.getInt(1) > 0;

        rs.close();
        ps.close();

        return exists;
    }
    public int expireOldWaitlistOffers() throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE orders o " +
            "JOIN waitlist w ON o.order_id = w.order_id " +
            "SET o.status = 'expired', w.status = 'expired' " +
            "WHERE o.status = 'pending' " +
            "AND w.status = 'notified' " +
            "AND w.expires_at IS NOT NULL " +
            "AND w.expires_at <= NOW()"
        );

        int rows = ps.executeUpdate();
        ps.close();

        return rows;
    }
    public int promoteNextWaitlistForSlot(int parkId, String date, String time) throws SQLException {
        int promoted = 0;
        String normalizedTime = normalizeTime(time);

        while (true) {
            int available = checkAvailability(parkId, date, normalizedTime);

            PreparedStatement ps = conn.prepareStatement(
                "SELECT w.waitlist_id, o.order_id, o.num_visitors " +
                "FROM waitlist w " +
                "JOIN orders o ON w.order_id = o.order_id " +
                "WHERE w.park_id = ? " +
                "AND w.visit_date = ? " +
                "AND TIME(w.visit_time) = TIME(?) " +
                "AND w.status = 'waiting' " +
                "AND o.status = 'waitlist' " +
                "ORDER BY w.position ASC " +
                "LIMIT 1"
            );

            ps.setInt(1, parkId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, normalizedTime);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                break;
            }

            int waitlistId = rs.getInt("waitlist_id");
            int orderId = rs.getInt("order_id");
            int numVisitors = rs.getInt("num_visitors");

            rs.close();
            ps.close();

            if (available < numVisitors) {
                break;
            }

            PreparedStatement updateOrder = conn.prepareStatement(
                "UPDATE orders SET status = 'pending' WHERE order_id = ? AND status = 'waitlist'"
            );
            updateOrder.setInt(1, orderId);
            updateOrder.executeUpdate();
            updateOrder.close();

            PreparedStatement updateWaitlist = conn.prepareStatement(
                "UPDATE waitlist " +
                "SET status = 'notified', notified_at = NOW(), expires_at = DATE_ADD(NOW(), INTERVAL 1 HOUR) " +
                "WHERE waitlist_id = ? AND status = 'waiting'"
            );
            updateWaitlist.setInt(1, waitlistId);
            updateWaitlist.executeUpdate();
            updateWaitlist.close();

            // Write notification so the client polling picks it up and shows the popup
            PreparedStatement notif = conn.prepareStatement(
                "INSERT INTO notifications (order_id, notification_type, message_text, is_read) " +
                "VALUES (?, 'waitlist_available', 'A spot opened for your waitlisted visit. You have 1 hour to confirm.', FALSE)");
            notif.setInt(1, orderId);
            notif.executeUpdate();
            notif.close();

            promoted++;
        }

        return promoted;
    }
    public int processWaitingLists() throws SQLException {
        int promoted = 0;

        PreparedStatement ps = conn.prepareStatement(
            "SELECT DISTINCT w.park_id, w.visit_date, w.visit_time " +
            "FROM waitlist w " +
            "JOIN orders o ON w.order_id = o.order_id " +
            "WHERE w.status = 'waiting' " +
            "AND o.status = 'waitlist' " +
            "AND w.visit_date >= CURDATE()"
        );

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            promoted += promoteNextWaitlistForSlot(
                rs.getInt("park_id"),
                rs.getDate("visit_date").toString(),
                rs.getString("visit_time")
            );
        }

        rs.close();
        ps.close();

        return promoted;
    }
    public int cancelOrderAndProcessWaitlist(int orderId) throws SQLException {
        PreparedStatement getOrder = conn.prepareStatement(
            "SELECT park_id, visit_date, visit_time FROM orders WHERE order_id = ?"
        );

        getOrder.setInt(1, orderId);
        ResultSet rs = getOrder.executeQuery();

        if (!rs.next()) {
            rs.close();
            getOrder.close();
            return 0;
        }

        int parkId = rs.getInt("park_id");
        String date = rs.getDate("visit_date").toString();
        String time = rs.getString("visit_time");

        rs.close();
        getOrder.close();

        updateOrderStatus(orderId, "cancelled");

        PreparedStatement updateWaitlist = conn.prepareStatement(
            "UPDATE waitlist " +
            "SET status = 'cancelled' " +
            "WHERE order_id = ? " +
            "AND status IN ('waiting', 'notified')"
        );
        updateWaitlist.setInt(1, orderId);
        updateWaitlist.executeUpdate();
        updateWaitlist.close();

        return promoteNextWaitlistForSlot(parkId, date, time);
    }
    public boolean confirmWaitlistOffer(int orderId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE orders o " +
            "JOIN waitlist w ON o.order_id = w.order_id " +
            "SET o.status = 'confirmed', w.status = 'confirmed' " +
            "WHERE o.order_id = ? " +
            "AND o.status = 'pending' " +
            "AND w.status = 'notified'"
        );

        ps.setInt(1, orderId);
        int rows = ps.executeUpdate();
        ps.close();

        return rows > 0;
    }
    public void cancelWaitlistRecord(int orderId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE waitlist " +
            "SET status = 'cancelled' " +
            "WHERE order_id = ? " +
            "AND status IN ('waiting', 'notified')"
        );

        ps.setInt(1, orderId);
        ps.executeUpdate();
        ps.close();
    }
    // === PARAMETER REQUESTS ===
    public void createParameterRequest(int parkId, String paramName, double oldVal, double newVal, int requestedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO parameter_requests (park_id, parameter_name, old_value, new_value, status, requested_by) VALUES (?,?,?,?,?,?)");
        ps.setInt(1, parkId); ps.setString(2, paramName); ps.setDouble(3, oldVal);
        ps.setDouble(4, newVal); ps.setString(5, "pending"); ps.setInt(6, requestedBy);
        ps.executeUpdate(); ps.close();
    }
    
    
    public int getWalkinsToday(int parkId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COALESCE(SUM(num_visitors), 0) AS total FROM park_visits " +
            "WHERE park_id = ? AND visit_type = 'walk_in' AND DATE(entry_time) = CURDATE()");
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        int total = 0;
        if (rs.next()) total = rs.getInt("total");
        rs.close(); ps.close();
        return total;
    }

    public ArrayList<ArrayList<String>> getPendingParameterRequests() throws SQLException {
        ArrayList<ArrayList<String>> requests = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT pr.*, p.park_name FROM parameter_requests pr JOIN parks p ON pr.park_id = p.park_id WHERE pr.status = 'pending'")) {
            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("request_id"))); row.add(rs.getString("park_name"));
                row.add(rs.getString("parameter_name")); row.add(String.valueOf(rs.getDouble("old_value")));
                row.add(String.valueOf(rs.getDouble("new_value"))); row.add(rs.getString("status"));
                requests.add(row);
            }
        }
        return requests;
    }

    public void approveParameterRequest(int requestId, int approvedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM parameter_requests WHERE request_id = ?");
        ps.setInt(1, requestId); ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String param = rs.getString("parameter_name"); double newVal = rs.getDouble("new_value"); int parkId = rs.getInt("park_id");
            java.util.Set<String> allowedParams = java.util.Set.of("max_visitors", "gap_for_walkins", "estimated_visit_duration");
            if (!allowedParams.contains(param)) { rs.close(); ps.close(); throw new SQLException("Invalid parameter name: " + param); }
            // Second line of defense: re-validate at approval time against current park values
            String validErr = validateParameterValue(parkId, param, newVal);
            if (validErr != null) { rs.close(); ps.close(); throw new SQLException(validErr); }
            PreparedStatement up = conn.prepareStatement("UPDATE parks SET " + param + " = ? WHERE park_id = ?");
            up.setDouble(1, newVal); up.setInt(2, parkId); up.executeUpdate(); up.close();
        }
        rs.close(); ps.close();
        PreparedStatement up2 = conn.prepareStatement("UPDATE parameter_requests SET status = 'approved', approved_by = ? WHERE request_id = ?");
        up2.setInt(1, approvedBy); up2.setInt(2, requestId); up2.executeUpdate(); up2.close();
    }

    /**
     * Validates a parameter change against business rules.
     * Reads current park values and checks the proposed new value.
     * Returns null if valid, or an error message if invalid.
     */
    public String validateParameterValue(int parkId, String param, double newVal) throws SQLException {
        Park park = getParkById(parkId);
        if (park == null) return "Park not found.";

        double currentMax  = park.getMaxVisitors();
        double currentGap  = park.getGapForWalkins();
        double currentDur  = park.getEstimatedVisitDuration();

        // Use new value for the field being changed, current value for others
        double effectiveMax = "max_visitors".equals(param)              ? newVal : currentMax;
        double effectiveGap = "gap_for_walkins".equals(param)           ? newVal : currentGap;
        double effectiveDur = "estimated_visit_duration".equals(param)  ? newVal : currentDur;

        if (effectiveMax <= 0)
            return "max_visitors must be greater than 0. Requested: " + (int)effectiveMax;
        if (effectiveGap < 0)
            return "gap_for_walkins cannot be negative. Requested: " + (int)effectiveGap;
        if (effectiveGap > effectiveMax)
            return "gap_for_walkins (" + (int)effectiveGap + ") cannot exceed max_visitors (" + (int)effectiveMax + ").";
        if (effectiveDur <= 0)
            return "estimated_visit_duration must be greater than 0. Requested: " + effectiveDur;

        return null; // valid
    }

    public void rejectParameterRequest(int requestId, int rejectedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE parameter_requests SET status = 'rejected', approved_by = ? WHERE request_id = ?");
        ps.setInt(1, rejectedBy); ps.setInt(2, requestId); ps.executeUpdate(); ps.close();
    }

    // === PROMOTIONS ===
    public void createPromotion(int parkId, double discount, String startDate, String endDate, String desc, int requestedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO promotions (park_id, discount_percentage, start_date, end_date, description, status, requested_by) VALUES (?,?,?,?,?,?,?)");
        ps.setInt(1, parkId); ps.setDouble(2, discount); ps.setDate(3, java.sql.Date.valueOf(startDate));
        ps.setDate(4, java.sql.Date.valueOf(endDate)); ps.setString(5, desc); ps.setString(6, "pending"); ps.setInt(7, requestedBy);
        ps.executeUpdate(); ps.close();
    }

    public ArrayList<ArrayList<String>> getPendingPromotions() throws SQLException {
        ArrayList<ArrayList<String>> promos = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT pr.*, p.park_name FROM promotions pr JOIN parks p ON pr.park_id = p.park_id WHERE pr.status = 'pending'")) {
            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("promo_id"))); row.add(rs.getString("park_name"));
                row.add(String.valueOf(rs.getDouble("discount_percentage")));
                row.add(rs.getDate("start_date").toString()); row.add(rs.getDate("end_date").toString());
                row.add(rs.getString("description")); row.add(rs.getString("status"));
                promos.add(row);
            }
        }
        return promos;
    }

    public void approvePromotion(int promoId, int approvedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE promotions SET status = 'approved', approved_by = ? WHERE promo_id = ?");
        ps.setInt(1, approvedBy); ps.setInt(2, promoId); ps.executeUpdate(); ps.close();
    }

    public void rejectPromotion(int promoId, int rejectedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE promotions SET status = 'rejected', approved_by = ? WHERE promo_id = ?");
        ps.setInt(1, rejectedBy); ps.setInt(2, promoId); ps.executeUpdate(); ps.close();
    }

    // === REPORTS ===
    public String generateReport(String reportType, int parkId, int month, int year, int generatedBy) throws SQLException {
        StringBuilder sb = new StringBuilder();
        java.sql.Date startDate = java.sql.Date.valueOf(String.format("%d-%02d-01", year, month));
        java.time.LocalDate lastDay = java.time.LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        java.sql.Date endDate = java.sql.Date.valueOf(lastDay);
        Park park = getParkById(parkId);
        String parkName = park != null ? park.getParkName() : "Unknown";
        
        System.out.println("[DB Report] type=" + reportType + " parkId=" + parkId + " range=" + startDate + " to " + endDate);

        switch (reportType) {
            case "GENERATE_VISITS_REPORT":
                sb.append("=== VISITS REPORT - ").append(parkName).append(" ===\n\n");
                // Individual visitors
                sb.append("--- Individual / Family Visitors ---\n");
                PreparedStatement ps1a = conn.prepareStatement(
                    "SELECT pv.entry_time, pv.exit_time, pv.num_visitors, " +
                    "TIMESTAMPDIFF(MINUTE, pv.entry_time, pv.exit_time) as stay_minutes " +
                    "FROM park_visits pv LEFT JOIN orders o ON pv.order_id = o.order_id " +
                    "WHERE pv.park_id = ? AND DATE(pv.entry_time) BETWEEN ? AND ? " +
                    "AND (o.order_type IN ('individual','family','walk_in') OR pv.visit_type = 'walk_in') " +
                    "ORDER BY pv.entry_time");
                ps1a.setInt(1, parkId); ps1a.setDate(2, startDate); ps1a.setDate(3, endDate);
                ResultSet rs1a = ps1a.executeQuery();
                int indivCount = 0; int indivVisitors = 0; double indivStayTotal = 0;
                while (rs1a.next()) {
                    String entry = rs1a.getTimestamp("entry_time").toString();
                    String exit = rs1a.getTimestamp("exit_time") != null ? rs1a.getTimestamp("exit_time").toString() : "Still in park";
                    int stay = rs1a.getInt("stay_minutes");
                    int num = rs1a.getInt("num_visitors");
                    sb.append("  Entry: ").append(entry).append(" | Exit: ").append(exit)
                      .append(" | Stay: ").append(stay / 60).append("h ").append(stay % 60).append("m")
                      .append(" | Visitors: ").append(num).append("\n");
                    indivCount++; indivVisitors += num; indivStayTotal += stay;
                }
                if (indivCount == 0) sb.append("  No individual visits in this period.\n");
                else sb.append("  Total: ").append(indivCount).append(" visits, ").append(indivVisitors)
                    .append(" visitors, avg stay: ").append(Math.round(indivStayTotal / indivCount)).append(" min\n");
                rs1a.close(); ps1a.close();

                // Organized groups
                sb.append("\n--- Organized Groups ---\n");
                PreparedStatement ps1b = conn.prepareStatement(
                    "SELECT pv.entry_time, pv.exit_time, pv.num_visitors, " +
                    "TIMESTAMPDIFF(MINUTE, pv.entry_time, pv.exit_time) as stay_minutes " +
                    "FROM park_visits pv LEFT JOIN orders o ON pv.order_id = o.order_id " +
                    "WHERE pv.park_id = ? AND DATE(pv.entry_time) BETWEEN ? AND ? " +
                    "AND o.order_type IN ('organized_group','walk_in_group') " +
                    "ORDER BY pv.entry_time");
                ps1b.setInt(1, parkId); ps1b.setDate(2, startDate); ps1b.setDate(3, endDate);
                ResultSet rs1b = ps1b.executeQuery();
                int groupCount = 0; int groupVisitors = 0; double groupStayTotal = 0;
                while (rs1b.next()) {
                    String entry = rs1b.getTimestamp("entry_time").toString();
                    String exit = rs1b.getTimestamp("exit_time") != null ? rs1b.getTimestamp("exit_time").toString() : "Still in park";
                    int stay = rs1b.getInt("stay_minutes");
                    int num = rs1b.getInt("num_visitors");
                    sb.append("  Entry: ").append(entry).append(" | Exit: ").append(exit)
                      .append(" | Stay: ").append(stay / 60).append("h ").append(stay % 60).append("m")
                      .append(" | Visitors: ").append(num).append("\n");
                    groupCount++; groupVisitors += num; groupStayTotal += stay;
                }
                if (groupCount == 0) sb.append("  No organized group visits in this period.\n");
                else sb.append("  Total: ").append(groupCount).append(" visits, ").append(groupVisitors)
                    .append(" visitors, avg stay: ").append(Math.round(groupStayTotal / groupCount)).append(" min\n");
                rs1b.close(); ps1b.close();

                sb.append("\n--- Summary ---\n");
                sb.append("Total visits: ").append(indivCount + groupCount).append("\n");
                sb.append("Total visitors: ").append(indivVisitors + groupVisitors).append("\n");
                break;
            case "GENERATE_CANCELLATION_REPORT":
                sb.append("=== CANCELLATION REPORT - ").append(parkName).append(" ===\n\n");
                
                // Summary by status
                sb.append("--- Summary by Status ---\n");
                PreparedStatement ps2a = conn.prepareStatement(
                    "SELECT status, COUNT(*) as cnt, SUM(num_visitors) as total_visitors FROM orders " +
                    "WHERE park_id = ? AND visit_date BETWEEN ? AND ? AND status IN ('cancelled','no_show','expired') GROUP BY status");
                ps2a.setInt(1, parkId); ps2a.setDate(2, startDate); ps2a.setDate(3, endDate);
                ResultSet rs2a = ps2a.executeQuery();
                int totalCancelled = 0;
                while (rs2a.next()) {
                    int cnt = rs2a.getInt("cnt");
                    sb.append("  ").append(rs2a.getString("status")).append(": ")
                      .append(cnt).append(" orders, ").append(rs2a.getInt("total_visitors")).append(" visitors\n");
                    totalCancelled += cnt;
                }
                if (totalCancelled == 0) sb.append("  No cancellations in this period.\n");
                rs2a.close(); ps2a.close();

                // Distribution by day
                sb.append("\n--- Distribution by Day ---\n");
                PreparedStatement ps2b = conn.prepareStatement(
                    "SELECT visit_date, status, COUNT(*) as cnt FROM orders " +
                    "WHERE park_id = ? AND visit_date BETWEEN ? AND ? AND status IN ('cancelled','no_show','expired') " +
                    "GROUP BY visit_date, status ORDER BY visit_date");
                ps2b.setInt(1, parkId); ps2b.setDate(2, startDate); ps2b.setDate(3, endDate);
                ResultSet rs2b = ps2b.executeQuery();
                int dayCount = 0;
                while (rs2b.next()) {
                    sb.append("  ").append(rs2b.getDate("visit_date")).append(" | ")
                      .append(rs2b.getString("status")).append(": ").append(rs2b.getInt("cnt")).append(" orders\n");
                    dayCount++;
                }
                if (dayCount == 0) sb.append("  No data.\n");
                rs2b.close(); ps2b.close();

                // Average cancellations per work day
                sb.append("\n--- Average ---\n");
                PreparedStatement ps2c = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT visit_date) as work_days, COUNT(*) as total FROM orders " +
                    "WHERE park_id = ? AND visit_date BETWEEN ? AND ? AND status IN ('cancelled','no_show','expired')");
                ps2c.setInt(1, parkId); ps2c.setDate(2, startDate); ps2c.setDate(3, endDate);
                ResultSet rs2c = ps2c.executeQuery();
                if (rs2c.next()) {
                    int workDays = rs2c.getInt("work_days");
                    int total = rs2c.getInt("total");
                    double avg = workDays > 0 ? (double) total / workDays : 0;
                    sb.append("  Work days with cancellations: ").append(workDays).append("\n");
                    sb.append("  Total cancellations: ").append(total).append("\n");
                    sb.append("  Average per day: ").append(String.format("%.1f", avg)).append(" cancellations/day\n");
                }
                rs2c.close(); ps2c.close();
                break;
            case "GENERATE_TOTAL_VISITORS_REPORT":
                sb.append("=== TOTAL VISITORS - ").append(parkName).append(" ===\n");
                PreparedStatement ps3 = conn.prepareStatement(
                    "SELECT order_type, SUM(num_visitors) as total FROM orders WHERE park_id = ? AND visit_date BETWEEN ? AND ? AND status IN ('completed','confirmed') GROUP BY order_type");
                ps3.setInt(1, parkId); ps3.setDate(2, startDate); ps3.setDate(3, endDate);
                ResultSet rs3 = ps3.executeQuery(); int grand = 0;
                System.out.println("[DB Report] Executing total visitors query...");
                while (rs3.next()) { 
                    int t = rs3.getInt("total"); 
                    String type = rs3.getString("order_type");
                    System.out.println("[DB Report] Found: " + type + " = " + t);
                    sb.append(type).append(": ").append(t).append("\n"); 
                    grand += t; 
                }
                System.out.println("[DB Report] Grand total: " + grand);
                sb.append("TOTAL: ").append(grand).append(" visitors\n");
                rs3.close(); ps3.close();
                break;
            case "GENERATE_USAGE_REPORT":
                sb.append("=== USAGE REPORT - ").append(parkName).append(" ===\n");
                int maxCap = park != null ? park.getMaxVisitors() : 999;
                PreparedStatement ps4 = conn.prepareStatement(
                    "SELECT visit_date, SUM(num_visitors) as daily FROM orders WHERE park_id = ? AND visit_date BETWEEN ? AND ? AND status IN ('completed','confirmed') GROUP BY visit_date ORDER BY visit_date");
                ps4.setInt(1, parkId); ps4.setDate(2, startDate); ps4.setDate(3, endDate);
                ResultSet rs4 = ps4.executeQuery();
                while (rs4.next()) { int d = rs4.getInt("daily"); sb.append(rs4.getDate("visit_date")).append(": ").append(d).append(d >= maxCap ? " [FULL]" : " [NOT FULL]").append("\n"); }
                rs4.close(); ps4.close();
                break;
        }
        if (sb.length() < 40) sb.append("No data available for this period.\n");
        System.out.println("[DB Report] Final result: " + sb.toString());
        return sb.toString();
    }

    // === SAVE REPORT ===
    public void saveReport(int parkId, String reportType, int generatedBy, int month, int year, String reportData) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO reports (park_id, report_type, generated_by, report_month, report_year, report_data) VALUES (?,?,?,?,?,?)");
        ps.setInt(1, parkId); ps.setString(2, reportType); ps.setInt(3, generatedBy);
        ps.setInt(4, month); ps.setInt(5, year); ps.setString(6, reportData);
        ps.executeUpdate(); ps.close();
        System.out.println("[DB] Report saved: type=" + reportType + " park=" + parkId + " month=" + month + "/" + year);
    }

    // === HELPERS ===
    private Order extractOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id")); o.setVisitorId(rs.getString("visitor_id"));
        o.setParkId(rs.getInt("park_id"));
        o.setVisitDate(rs.getDate("visit_date") != null ? rs.getDate("visit_date").toString() : "");
        o.setVisitTime(rs.getString("visit_time")); o.setNumVisitors(rs.getInt("num_visitors"));
        o.setEmail(rs.getString("email")); o.setPhone(rs.getString("phone"));
        o.setOrderType(rs.getString("order_type")); o.setStatus(rs.getString("status"));
        o.setConfirmationCode(rs.getString("confirmation_code"));
        o.setTotalPrice(rs.getDouble("total_price"));
        // Read reminder fields for My Orders display labels
        try { o.setReminderSent(rs.getBoolean("reminder_sent")); } catch (SQLException ignored) {}
        try { o.setReminderConfirmed(rs.getBoolean("reminder_confirmed")); } catch (SQLException ignored) {}
        return o;
    }

    private GeneralParkWorker extractWorker(ResultSet rs) throws SQLException {
        GeneralParkWorker w = new GeneralParkWorker();
        w.setEmployeeId(rs.getInt("employee_id")); w.setFirstName(rs.getString("first_name"));
        w.setLastName(rs.getString("last_name")); w.setEmail(rs.getString("email"));
        w.setRole(rs.getString("role")); w.setParkId(rs.getInt("park_id"));
        w.setUsername(rs.getString("username")); w.setLoggedIn(rs.getBoolean("is_logged_in"));
        return w;
    }

    // Atomic booking: checks availability and creates the order as ONE locked operation
    // so two simultaneous clients cannot both book the last spot (prevents overbooking).
    public synchronized Order bookOrderAtomic(Order order) throws SQLException {
        int avail = checkAvailability(order.getParkId(), order.getVisitDate(), order.getVisitTime());
        if (avail >= order.getNumVisitors()) {
            createOrder(order);
            return order;
        }
        return null; // not enough room
    }

    // Atomic walk-in: same idea for walk-in entries against the gap.
    public synchronized boolean walkInAtomic(Order order, Park park) throws SQLException {
        // Check 1: prevent same visitor from doing multiple walk-ins today at same park
        if (hasActiveWalkInToday(order.getParkId(), order.getVisitorId())) {
            return false;
        }
        // Check 2: enough walk-in spots remaining
        int walkinsUsed = getWalkinsToday(order.getParkId());
        int walkinAvailable = park.getGapForWalkins() - walkinsUsed;
        boolean parkHasRoom = park.getCurrentVisitors() + order.getNumVisitors() <= park.getMaxVisitors();
        if (order.getNumVisitors() <= walkinAvailable && parkHasRoom) {
            createOrder(order);
            // Must use 'walk_in' visitType so getWalkinsToday() counts it correctly
            recordEntry(order.getOrderId(), order.getParkId(), order.getVisitorId(), order.getNumVisitors(), "walk_in");
            updateOrderStatus(order.getOrderId(), "completed");
            return true;
        }
        return false;
    }

    // Returns true if this visitor is currently inside the park (walk-in, no exit yet)
    // Only rejects if exit_time IS NULL — allows re-entry after exit
    public boolean hasActiveWalkInToday(int parkId, String visitorId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM park_visits " +
            "WHERE park_id = ? AND visitor_id = ? AND visit_type = 'walk_in' " +
            "AND DATE(entry_time) = CURDATE() AND exit_time IS NULL");
        ps.setInt(1, parkId); ps.setString(2, visitorId);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next() && rs.getInt(1) > 0;
        rs.close(); ps.close();
        return exists;
    }


    // Marks a day-before reminder as confirmed so the order is NOT auto-cancelled
    public boolean confirmReminder(int orderId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE orders SET reminder_confirmed = TRUE " +
            "WHERE order_id = ? AND status = 'confirmed' AND reminder_sent = TRUE AND reminder_confirmed = FALSE");
        ps.setInt(1, orderId);
        int rows = ps.executeUpdate();
        ps.close();
        return rows > 0;
    }


    // Returns the numeric guide_id for a guide's national ID, or -1 if not a guide
    public int getGuideIdByIdNumber(String idNumber) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT guide_id FROM guides WHERE id_number = ?");
        ps.setString(1, idNumber);
        ResultSet rs = ps.executeQuery();
        int gId = rs.next() ? rs.getInt("guide_id") : -1;
        rs.close(); ps.close();
        return gId;
    }


    // Returns the best APPROVED promotion discount % active on the visit date for a park (0 if none)
    public double getActivePromotionDiscount(int parkId, String visitDate) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT MAX(discount_percentage) AS best FROM promotions " +
            "WHERE park_id = ? AND status = 'approved' AND ? BETWEEN start_date AND end_date");
        ps.setInt(1, parkId);
        ps.setDate(2, Date.valueOf(visitDate));
        ResultSet rs = ps.executeQuery();
        double best = rs.next() ? rs.getDouble("best") : 0;
        rs.close(); ps.close();
        return best;
    }


    // Returns unread reminder + waitlist_available notifications for a traveler
    // Uses visitor_id via join to orders table
    public java.util.ArrayList<java.util.ArrayList<String>> getUnreadNotifications(String visitorId) throws SQLException {
        java.util.ArrayList<java.util.ArrayList<String>> list = new java.util.ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT n.notification_id, n.order_id, n.notification_type, n.message_text, " +
            "o.park_id, o.visit_date, o.visit_time, o.num_visitors, o.status, p.park_name " +
            "FROM notifications n " +
            "JOIN orders o ON n.order_id = o.order_id " +
            "JOIN parks p ON o.park_id = p.park_id " +
            "WHERE o.visitor_id = ? " +
            "AND n.notification_type IN ('reminder','waitlist_available') " +
            "AND n.is_read = FALSE " +
            "AND o.status NOT IN ('cancelled','expired','completed') " +
            "ORDER BY n.sent_at ASC");
        ps.setString(1, visitorId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            java.util.ArrayList<String> row = new java.util.ArrayList<>();
            row.add(String.valueOf(rs.getInt("notification_id")));
            row.add(String.valueOf(rs.getInt("order_id")));
            row.add(rs.getString("notification_type"));
            row.add(rs.getString("message_text") != null ? rs.getString("message_text") : "");
            row.add(rs.getString("park_name"));
            row.add(rs.getString("visit_date") != null ? rs.getString("visit_date") : "");
            row.add(rs.getString("visit_time") != null ? rs.getString("visit_time") : "");
            row.add(String.valueOf(rs.getInt("num_visitors")));
            row.add(rs.getString("status") != null ? rs.getString("status") : "");
            list.add(row);
        }
        rs.close(); ps.close();
        return list;
    }

    // Mark a specific notification as read (after user sees the popup)
    public void markNotificationRead(int notificationId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?");
        ps.setInt(1, notificationId);
        ps.executeUpdate();
        ps.close();
    }


    // Returns order by confirmation code (for park worker entrance using code instead of ID)
    public Order getOrderByConfirmationCode(String code) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT o.*, p.park_name FROM orders o JOIN parks p ON o.park_id = p.park_id " +
            "WHERE o.confirmation_code = ?");
        ps.setString(1, code.trim().toUpperCase());
        ResultSet rs = ps.executeQuery();
        Order order = null;
        if (rs.next()) {
            order = extractOrder(rs);
            order.setParkName(rs.getString("park_name"));
        }
        rs.close(); ps.close();
        return order;
    }


    /**
     * Returns up to 3 available time slots starting from the requested date/time
     * for a given park and group size.
     * Each result: [date, time, availableSpots]
     */
    public java.util.ArrayList<java.util.ArrayList<String>> getAlternativeSlots(
            int parkId, String fromDate, String fromTime, int numVisitors) throws SQLException {
        java.util.ArrayList<java.util.ArrayList<String>> results = new java.util.ArrayList<>();
        String[] hours = {"08:00:00","09:00:00","10:00:00","11:00:00","12:00:00","13:00:00","14:00:00","15:00:00","16:00:00"};
        java.time.LocalDate date = java.time.LocalDate.parse(fromDate);

        // Normalize fromTime to HH:mm:ss for comparison
        String normalizedFrom = fromTime.length() == 5 ? fromTime + ":00" : fromTime;

        for (int day = 0; day <= 14 && results.size() < 3; day++) {
            java.time.LocalDate checkDate = date.plusDays(day);
            String checkDateStr = checkDate.toString();
            for (String hour : hours) {
                if (results.size() >= 3) break;
                // Skip same date+time (that's the full slot) and past times on day 0
                if (day == 0 && hour.compareTo(normalizedFrom) <= 0) continue;
                // Skip the exact same slot that was full
                if (day == 0 && hour.equals(normalizedFrom)) continue;
                int avail = checkAvailability(parkId, checkDateStr, hour);
                if (avail >= numVisitors) {
                    java.util.ArrayList<String> slot = new java.util.ArrayList<>();
                    slot.add(checkDateStr);
                    slot.add(hour.substring(0, 5)); // HH:mm
                    slot.add(String.valueOf(avail));
                    results.add(slot);
                }
            }
        }
        return results;
    }


    /** Auto-complete in_park visits where entry_time > estimated_visit_duration hours ago */
    public int autoCompleteExpiredVisits() throws SQLException {
        // Find expired in-park visits
        PreparedStatement ps = conn.prepareStatement(
            "SELECT o.order_id, o.park_id, o.num_visitors " +
            "FROM orders o " +
            "JOIN park_visits v ON o.order_id = v.order_id " +
            "JOIN parks p ON o.park_id = p.park_id " +
            "WHERE o.status = 'in_park' " +
            "AND TIMESTAMPDIFF(HOUR, v.entry_time, NOW()) >= p.estimated_visit_duration " +
            "AND v.exit_time IS NULL");
        ResultSet rs = ps.executeQuery();
        int count = 0;
        while (rs.next()) {
            int orderId    = rs.getInt("order_id");
            int parkId     = rs.getInt("park_id");
            int numVisitors = rs.getInt("num_visitors");

            PreparedStatement upOrder = conn.prepareStatement(
                "UPDATE orders SET status = 'completed' WHERE order_id = ? AND status = 'in_park'");
            upOrder.setInt(1, orderId); upOrder.executeUpdate(); upOrder.close();

            PreparedStatement upVisit = conn.prepareStatement(
                "UPDATE park_visits v JOIN parks p ON v.park_id = p.park_id " +
                "SET v.exit_time = DATE_ADD(v.entry_time, INTERVAL p.estimated_visit_duration HOUR) " +
                "WHERE v.order_id = ? AND v.exit_time IS NULL");
            upVisit.setInt(1, orderId); upVisit.executeUpdate(); upVisit.close();

            PreparedStatement upPark = conn.prepareStatement(
                "UPDATE parks SET current_visitors = GREATEST(0, current_visitors - ?) WHERE park_id = ?");
            upPark.setInt(1, numVisitors); upPark.setInt(2, parkId);
            upPark.executeUpdate(); upPark.close();
            count++;
        }
        rs.close(); ps.close();
        return count;
    }

    public GeneralParkWorker getEmployeeById(String employeeId) throws SQLException {
        try {
            int empId = Integer.parseInt(employeeId);
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM employees WHERE employee_id = ?");
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            GeneralParkWorker w = null;
            if (rs.next()) w = extractWorker(rs);
            rs.close(); ps.close();
            return w;
        } catch (NumberFormatException e) {
            return null; // not a numeric employee ID
        }
    }

    public void updateSubscriberProfile(String idNumber, String firstName, String lastName, String email) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE subscribers SET first_name=?, last_name=?, email=? WHERE id_number=?");
        ps.setString(1, firstName);
        ps.setString(2, lastName);
        ps.setString(3, email);
        ps.setString(4, idNumber);
        ps.executeUpdate();
        ps.close();
    }

}
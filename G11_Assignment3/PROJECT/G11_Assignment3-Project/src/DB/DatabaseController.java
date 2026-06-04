package DB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import common.Order;
import common.Park;
import common.Subscriber;
import common.worker.GeneralParkWorker;

public class DatabaseController {
    private Connection conn;

    public DatabaseController(Connection conn) { this.conn = conn; }

    // === PARKS ===
    public ArrayList<Park> getAllParks() throws SQLException {
        ArrayList<Park> parks = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM parks");
        while (rs.next()) {
            parks.add(new Park(rs.getInt("park_id"), rs.getString("park_name"),
                rs.getInt("max_visitors"), rs.getInt("gap_for_walkins"),
                rs.getDouble("estimated_visit_duration"), rs.getInt("current_visitors"),
                rs.getDouble("full_price")));
        }
        rs.close();
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
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO orders (visitor_id, park_id, visit_date, visit_time, num_visitors, email, phone, " +
            "order_type, status, confirmation_code, guide_id, subscriber_id, is_paid_in_advance, total_price) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, order.getVisitorId()); ps.setInt(2, order.getParkId());
        ps.setDate(3, Date.valueOf(order.getVisitDate())); ps.setString(4, order.getVisitTime());
        ps.setInt(5, order.getNumVisitors()); ps.setString(6, order.getEmail());
        ps.setString(7, order.getPhone()); ps.setString(8, order.getOrderType());
        ps.setString(9, "confirmed"); ps.setString(10, code);
        if (order.getGuideId() > 0) ps.setInt(11, order.getGuideId()); else ps.setNull(11, java.sql.Types.INTEGER);
        if (order.getSubscriberId() > 0) ps.setInt(12, order.getSubscriberId()); else ps.setNull(12, java.sql.Types.INTEGER);
        ps.setBoolean(13, order.isPaidInAdvance()); ps.setDouble(14, order.getTotalPrice());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        int id = keys.next() ? keys.getInt(1) : -1;
        order.setOrderId(id); order.setConfirmationCode(code); order.setStatus("confirmed");
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

    public int checkAvailability(int parkId, String date, String time) throws SQLException {
        Park park = getParkById(parkId);
        if (park == null) return 0;
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COALESCE(SUM(num_visitors), 0) AS booked FROM orders WHERE park_id = ? AND visit_date = ? AND status IN ('confirmed', 'completed')");
        ps.setInt(1, parkId); ps.setDate(2, Date.valueOf(date));
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
    public void registerGuide(String idNumber, String firstName, String lastName, String email, String phone) throws SQLException {
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
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO park_visits (order_id, park_id, visitor_id, num_visitors, entry_time, visit_type) VALUES (?,?,?,?,NOW(),?)");
        ps.setInt(1, orderId); ps.setInt(2, parkId); ps.setString(3, visitorId);
        ps.setInt(4, numVisitors); ps.setString(5, orderId > 0 ? "reserved" : "walk_in");
        ps.executeUpdate(); ps.close();
        updateParkVisitors(parkId, numVisitors);
    }

    public void recordExit(int parkId, String visitorId, int numVisitors) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE park_visits SET exit_time = NOW() WHERE park_id = ? AND visitor_id = ? AND exit_time IS NULL");
        ps.setInt(1, parkId); ps.setString(2, visitorId);
        ps.executeUpdate(); ps.close();
        updateParkVisitors(parkId, -numVisitors);
    }



    // === WAITLIST ===
    public int createWaitlistOrder(Order order) throws SQLException {
        // Create the order with waitlist status
        String code = "WL-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO orders (visitor_id, park_id, visit_date, visit_time, num_visitors, email, phone, " +
            "order_type, status, confirmation_code, subscriber_id, total_price) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, order.getVisitorId()); ps.setInt(2, order.getParkId());
        ps.setDate(3, Date.valueOf(order.getVisitDate())); ps.setString(4, order.getVisitTime());
        ps.setInt(5, order.getNumVisitors()); ps.setString(6, order.getEmail());
        ps.setString(7, order.getPhone()); ps.setString(8, order.getOrderType());
        ps.setString(9, "waitlist"); ps.setString(10, code);
        if (order.getSubscriberId() > 0) ps.setInt(11, order.getSubscriberId()); else ps.setNull(11, java.sql.Types.INTEGER);
        ps.setDouble(12, order.getTotalPrice());
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
    // === PARAMETER REQUESTS ===
    public void createParameterRequest(int parkId, String paramName, double oldVal, double newVal, int requestedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO parameter_requests (park_id, parameter_name, old_value, new_value, status, requested_by) VALUES (?,?,?,?,?,?)");
        ps.setInt(1, parkId); ps.setString(2, paramName); ps.setDouble(3, oldVal);
        ps.setDouble(4, newVal); ps.setString(5, "pending"); ps.setInt(6, requestedBy);
        ps.executeUpdate(); ps.close();
    }

    public ArrayList<ArrayList<String>> getPendingParameterRequests() throws SQLException {
        ArrayList<ArrayList<String>> requests = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT pr.*, p.park_name FROM parameter_requests pr JOIN parks p ON pr.park_id = p.park_id WHERE pr.status = 'pending'");
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            row.add(String.valueOf(rs.getInt("request_id"))); row.add(rs.getString("park_name"));
            row.add(rs.getString("parameter_name")); row.add(String.valueOf(rs.getDouble("old_value")));
            row.add(String.valueOf(rs.getDouble("new_value"))); row.add(rs.getString("status"));
            requests.add(row);
        }
        rs.close(); return requests;
    }

    public void approveParameterRequest(int requestId, int approvedBy) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM parameter_requests WHERE request_id = ?");
        ps.setInt(1, requestId); ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String param = rs.getString("parameter_name"); double newVal = rs.getDouble("new_value"); int parkId = rs.getInt("park_id");
            PreparedStatement up = conn.prepareStatement("UPDATE parks SET " + param + " = ? WHERE park_id = ?");
            up.setDouble(1, newVal); up.setInt(2, parkId); up.executeUpdate(); up.close();
        }
        rs.close(); ps.close();
        PreparedStatement up2 = conn.prepareStatement("UPDATE parameter_requests SET status = 'approved', approved_by = ? WHERE request_id = ?");
        up2.setInt(1, approvedBy); up2.setInt(2, requestId); up2.executeUpdate(); up2.close();
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
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT pr.*, p.park_name FROM promotions pr JOIN parks p ON pr.park_id = p.park_id WHERE pr.status = 'pending'");
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            row.add(String.valueOf(rs.getInt("promo_id"))); row.add(rs.getString("park_name"));
            row.add(String.valueOf(rs.getDouble("discount_percentage")));
            row.add(rs.getDate("start_date").toString()); row.add(rs.getDate("end_date").toString());
            row.add(rs.getString("description")); row.add(rs.getString("status"));
            promos.add(row);
        }
        rs.close(); return promos;
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
}
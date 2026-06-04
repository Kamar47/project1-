package DB;

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
        ps.setInt(11, order.getGuideId()); ps.setInt(12, order.getSubscriberId());
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

package common;

import java.io.Serializable;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private int orderId;
    private String visitorId;
    private int parkId;
    private String parkName;
    private String visitDate;
    private String visitTime;
    private int numVisitors;
    private String email;
    private String phone;
    private String orderType;    // individual, family, organized_group, walk_in, walk_in_group
    private String status;       // pending, confirmed, waitlist, cancelled, completed, no_show, expired
    private String confirmationCode;
    private int guideId;
    private int subscriberId;
    private boolean paidInAdvance;
    private double totalPrice;
    private String createdAt;

    public Order() {}
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }
    public String getParkName() { return parkName; }
    public void setParkName(String parkName) { this.parkName = parkName; }
    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }
    public String getVisitTime() { return visitTime; }
    public void setVisitTime(String visitTime) { this.visitTime = visitTime; }
    public int getNumVisitors() { return numVisitors; }
    public void setNumVisitors(int numVisitors) { this.numVisitors = numVisitors; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    public int getGuideId() { return guideId; }
    public void setGuideId(int guideId) { this.guideId = guideId; }
    public int getSubscriberId() { return subscriberId; }
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }
    public boolean isPaidInAdvance() { return paidInAdvance; }
    public void setPaidInAdvance(boolean paidInAdvance) { this.paidInAdvance = paidInAdvance; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

package common;

import java.io.Serializable;

/**
 * Represents a visit reservation (booking) in the GoNature system.
 * <p>
 * An {@code Order} is created when a traveler books a visit to a nature park.
 * It tracks all details of the booking from creation through completion.
 * </p>
 *
 * <p><b>Order types:</b></p>
 * <ul>
 *   <li>{@code individual} — personal or family pre-booked visit</li>
 *   <li>{@code family} — subscriber family visit</li>
 *   <li>{@code organized_group} — guided group visit (max 15, guide is free)</li>
 *   <li>{@code walk_in} — unplanned individual visit if space is available</li>
 *   <li>{@code walk_in_group} — unplanned group walk-in</li>
 * </ul>
 *
 * <p><b>Status flow:</b>
 * {@code pending} → {@code confirmed} → {@code in_park} → {@code completed}<br>
 * or → {@code cancelled} / {@code expired} / {@code no_show} / {@code waitlist}
 * </p>
 *
 * <p>The {@code reminderSent} and {@code reminderConfirmed} flags support the
 * automated reminder flow: a reminder is sent one day before the visit,
 * and the traveler must confirm within 2 hours or the order is auto-cancelled.</p>
 *
 * @author Group 11
 */
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
    private String orderType;
    private String status;
    private String confirmationCode;
    private int guideId;
    private int subscriberId;
    private boolean paidInAdvance;
    private double totalPrice;
    private String createdAt;
    private boolean reminderSent;
    private boolean reminderConfirmed;

    /**
     * Creates an empty order object.
     */
    public Order() {}

    /**
     * Returns the order identifier.
     *
     * @return the order ID
     */
    public int getOrderId() { return orderId; }
    /**
     * Sets the order identifier.
     *
     * @param orderId the order ID to set
     */
    public void setOrderId(int orderId) { this.orderId = orderId; }
    /**
     * Returns the visitor ID number associated with the order.
     *
     * @return the visitor ID number
     */
    public String getVisitorId() { return visitorId; }
    /**
     * Sets the visitor ID number associated with the order.
     *
     * @param visitorId the visitor ID number to set
     */
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    /**
     * Returns the park identifier of the ordered visit.
     *
     * @return the park ID
     */
    public int getParkId() { return parkId; }
    
    /**
     * Sets the park identifier of the ordered visit.
     *
     * @param parkId the park ID to set
     */
    public void setParkId(int parkId) { this.parkId = parkId; }
    
    /**
     * Returns the park name of the ordered visit.
     *
     * @return the park name
     */
    public String getParkName() { return parkName; }
    
    /**
     * Sets the park name of the ordered visit.
     *
     * @param parkName the park name to set
     */
    public void setParkName(String parkName) { this.parkName = parkName; }
    /**
     * Returns the visit date of the order.
     *
     * @return the visit date
     */
    public String getVisitDate() { return visitDate; }
    /**
     * Sets the visit date of the order.
     *
     * @param visitDate the visit date to set
     */
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }
    /**
     * Returns the planned visit time.
     *
     * @return the visit time
     */
    public String getVisitTime() { return visitTime; }
    /**
     * Sets the planned visit time.
     *
     * @param visitTime the visit time to set
     */
    public void setVisitTime(String visitTime) { this.visitTime = visitTime; }
    /**
     * Returns the number of visitors included in the order.
     *
     * @return the number of visitors
     */
    public int getNumVisitors() { return numVisitors; }
    /**
     * Sets the number of visitors included in the order.
     *
     * @param numVisitors the number of visitors to set
     */
    public void setNumVisitors(int numVisitors) { this.numVisitors = numVisitors; }
    
    /**
     * Returns the traveler email address associated with the order.
     *
     * @return the traveler email address
     */
    public String getEmail() { return email; }
    
    /**
     * Sets the traveler email address associated with the order.
     *
     * @param email the traveler email address to set
     */
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns the traveler phone number associated with the order.
     *
     * @return the traveler phone number
     */
    public String getPhone() { return phone; }
    
    /**
     * Sets the traveler phone number associated with the order.
     *
     * @param phone the traveler phone number to set
     */
    public void setPhone(String phone) { this.phone = phone; }
    /**
     * Returns the order type.
     *
     * @return the order type
     */
    public String getOrderType() { return orderType; }
    /**
     * Sets the order type.
     *
     * @param orderType the order type to set
     */
    public void setOrderType(String orderType) { this.orderType = orderType; }
    /**
     * Returns the current status of the order.
     *
     * @return the order status
     */
    public String getStatus() { return status; }
    /**
     * Sets the current status of the order.
     *
     * @param status the order status to set
     */
    public void setStatus(String status) { this.status = status; }
    /**
     * Returns the confirmation code of the order.
     *
     * @return the confirmation code
     */
    public String getConfirmationCode() { return confirmationCode; }
    /**
     * Sets the confirmation code of the order.
     *
     * @param confirmationCode the confirmation code to set
     */
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    
    /**
     * Returns the guide identifier associated with the order.
     *
     * @return the guide ID, or 0 if no guide is associated with the order
     */
    public int getGuideId() { return guideId; }
    
    /**
     * Sets the guide identifier associated with the order.
     *
     * @param guideId the guide ID to set
     */
    public void setGuideId(int guideId) { this.guideId = guideId; }
    
    /**
     * Returns the subscriber identifier associated with the order.
     *
     * @return the subscriber ID, or 0 if the order is not linked to a subscriber
     */
    public int getSubscriberId() { return subscriberId; }
    
    /**
     * Sets the subscriber identifier associated with the order.
     *
     * @param subscriberId the subscriber ID to set
     */
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }
    
    /**
     * Returns whether the order was paid in advance.
     *
     * @return true if the order was paid in advance, otherwise false
     */
    public boolean isPaidInAdvance() { return paidInAdvance; }
    
    /**
     * Sets whether the order was paid in advance.
     *
     * @param paidInAdvance true if the order was paid in advance, otherwise false
     */
    public void setPaidInAdvance(boolean paidInAdvance) { this.paidInAdvance = paidInAdvance; }
   
    /**
     * Returns the total price calculated for the order.
     *
     * @return the total order price
     */
    public double getTotalPrice() { return totalPrice; }
    /**
     * Sets the total price calculated for the order.
     *
     * @param totalPrice the total order price to set
     */
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    
    /**
     * Returns the order creation date and time.
     *
     * @return the creation timestamp of the order
     */
    public String getCreatedAt() { return createdAt; }
    
    /**
     * Sets the order creation date and time.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    /**
     * Returns whether a reminder was sent for this order.
     *
     * @return true if a reminder was sent, otherwise false
     */
    public boolean isReminderSent() { return reminderSent; }
    
    /**
     * Sets whether a reminder was sent for this order.
     *
     * @param reminderSent true if a reminder was sent, otherwise false
     */
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    
    /**
     * Returns whether the traveler confirmed the reminder.
     *
     * @return true if the reminder was confirmed, otherwise false
     */
    public boolean isReminderConfirmed() { return reminderConfirmed; }
    
    /**
     * Sets whether the traveler confirmed the reminder.
     *
     * @param reminderConfirmed true if the reminder was confirmed, otherwise false
     */
    public void setReminderConfirmed(boolean reminderConfirmed) { this.reminderConfirmed = reminderConfirmed; }
}

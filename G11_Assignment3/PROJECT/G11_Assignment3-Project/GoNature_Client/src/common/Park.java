package common;

import java.io.Serializable;

/**
 * Represents a nature park managed by the GoNature department.
 * <p>
 * Each park has independently configurable capacity parameters that can be
 * updated by the park manager (subject to department manager approval).
 * </p>
 *
 * <p><b>Key parameters:</b></p>
 * <ul>
 *   <li>{@code maxVisitors} — total capacity of the park at any time</li>
 *   <li>{@code gapForWalkins} — number of spots reserved for walk-in visitors
 *       (subtracted from the bookable capacity)</li>
 *   <li>{@code estimatedVisitDuration} — default stay duration in hours (default: 4.0)</li>
 *   <li>{@code currentVisitors} — real-time count of visitors currently inside</li>
 *   <li>{@code fullPrice} — base price per visitor set by the tourism ministry</li>
 * </ul>
 *
 * @author Group 11
 */
public class Park implements Serializable {
    private static final long serialVersionUID = 1L;
    private int parkId;
    private String parkName;
    private int maxVisitors;
    private int gapForWalkins;
    private double estimatedVisitDuration;
    private int currentVisitors;
    private double fullPrice;

    /**
     * Creates an empty park object.
     */
    public Park() {}
    /**
     * Creates a park object with all park details and capacity parameters.
     *
     * @param parkId the park identifier
     * @param parkName the park name
     * @param maxVisitors the maximum number of visitors allowed in the park
     * @param gapForWalkins the number of spots reserved for walk-in visitors
     * @param estimatedVisitDuration the estimated visit duration in hours
     * @param currentVisitors the current number of visitors inside the park
     * @param fullPrice the full ticket price for one visitor
     */
    public Park(int parkId, String parkName, int maxVisitors, int gapForWalkins,
                double estimatedVisitDuration, int currentVisitors, double fullPrice) {
        this.parkId = parkId; this.parkName = parkName; this.maxVisitors = maxVisitors;
        this.gapForWalkins = gapForWalkins; this.estimatedVisitDuration = estimatedVisitDuration;
        this.currentVisitors = currentVisitors; this.fullPrice = fullPrice;
    }
    /**
     * Returns the park identifier.
     *
     * @return the park ID
     */
    public int getParkId() { return parkId; }
    /**
     * Sets the park identifier.
     *
     * @param parkId the park ID to set
     */
    public void setParkId(int parkId) { this.parkId = parkId; }
    /**
     * Returns the park name.
     *
     * @return the park name
     */
    public String getParkName() { return parkName; }
    /**
     * Sets the park name.
     *
     * @param parkName the park name to set
     */
    public void setParkName(String parkName) { this.parkName = parkName; }
    /**
     * Returns the maximum number of visitors allowed in the park.
     *
     * @return the maximum visitor capacity
     */
    public int getMaxVisitors() { return maxVisitors; }
    /**
     * Sets the maximum number of visitors allowed in the park.
     *
     * @param maxVisitors the maximum visitor capacity to set
     */
    public void setMaxVisitors(int maxVisitors) { this.maxVisitors = maxVisitors; }
    /**
     * Returns the number of visitor spots reserved for walk-in visitors.
     *
     * @return the walk-in visitor gap
     */
    public int getGapForWalkins() { return gapForWalkins; }
    /**
     * Sets the number of visitor spots reserved for walk-in visitors.
     *
     * @param gapForWalkins the walk-in visitor gap to set
     */
    public void setGapForWalkins(int gapForWalkins) { this.gapForWalkins = gapForWalkins; }
    /**
     * Returns the estimated visit duration for this park.
     *
     * @return the estimated visit duration in hours
     */
    public double getEstimatedVisitDuration() { return estimatedVisitDuration; }
    /**
     * Sets the estimated visit duration for this park.
     *
     * @param d the estimated visit duration in hours
     */
    public void setEstimatedVisitDuration(double d) { this.estimatedVisitDuration = d; }
    /**
     * Returns the current number of visitors inside the park.
     *
     * @return the current number of visitors
     */
    public int getCurrentVisitors() { return currentVisitors; }
    /**
     * Sets the current number of visitors inside the park.
     *
     * @param currentVisitors the current visitor count to set
     */
    public void setCurrentVisitors(int currentVisitors) { this.currentVisitors = currentVisitors; }
    /**
     * Returns the full ticket price for one visitor in this park.
     *
     * @return the full ticket price
     */
    public double getFullPrice() { return fullPrice; }
    /**
     * Sets the full ticket price for one visitor in this park.
     *
     * @param fullPrice the full ticket price to set
     */
    public void setFullPrice(double fullPrice) { this.fullPrice = fullPrice; }
    /**
     * Returns the number of spots currently available for new pre-booked reservations.
     * <p>
     * Calculated as: {@code maxVisitors - gapForWalkins - currentVisitors}.
     * Walk-in visitors use the {@code gapForWalkins} portion of capacity separately.
     * </p>
     *
     * @return available booking spots (may be negative if the park is over-capacity during transitions)
     */
    public int getAvailableSpots() { return maxVisitors - gapForWalkins - currentVisitors; }
}

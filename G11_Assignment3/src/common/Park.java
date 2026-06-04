package common;

import java.io.Serializable;

public class Park implements Serializable {
    private static final long serialVersionUID = 1L;
    private int parkId;
    private String parkName;
    private int maxVisitors;
    private int gapForWalkins;
    private double estimatedVisitDuration;
    private int currentVisitors;
    private double fullPrice;

    public Park() {}
    public Park(int parkId, String parkName, int maxVisitors, int gapForWalkins,
                double estimatedVisitDuration, int currentVisitors, double fullPrice) {
        this.parkId = parkId; this.parkName = parkName; this.maxVisitors = maxVisitors;
        this.gapForWalkins = gapForWalkins; this.estimatedVisitDuration = estimatedVisitDuration;
        this.currentVisitors = currentVisitors; this.fullPrice = fullPrice;
    }
    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }
    public String getParkName() { return parkName; }
    public void setParkName(String parkName) { this.parkName = parkName; }
    public int getMaxVisitors() { return maxVisitors; }
    public void setMaxVisitors(int maxVisitors) { this.maxVisitors = maxVisitors; }
    public int getGapForWalkins() { return gapForWalkins; }
    public void setGapForWalkins(int gapForWalkins) { this.gapForWalkins = gapForWalkins; }
    public double getEstimatedVisitDuration() { return estimatedVisitDuration; }
    public void setEstimatedVisitDuration(double d) { this.estimatedVisitDuration = d; }
    public int getCurrentVisitors() { return currentVisitors; }
    public void setCurrentVisitors(int currentVisitors) { this.currentVisitors = currentVisitors; }
    public double getFullPrice() { return fullPrice; }
    public void setFullPrice(double fullPrice) { this.fullPrice = fullPrice; }
    public int getAvailableSpots() { return maxVisitors - gapForWalkins - currentVisitors; }
}

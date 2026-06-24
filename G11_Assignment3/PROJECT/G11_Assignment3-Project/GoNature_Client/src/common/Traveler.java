package common;

import java.io.Serializable;

public class Traveler implements Serializable {
    private static final long serialVersionUID = 1L;
    private String idNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean isGuide;
    private int subscriberId;  // 0 if not subscriber

    public Traveler() {}
    public Traveler(String idNumber, String firstName, String lastName, String email, String phone) {
        this.idNumber = idNumber; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.phone = phone;
    }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isGuide() { return isGuide; }
    public void setGuide(boolean guide) { isGuide = guide; }
    public int getSubscriberId() { return subscriberId; }
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }
    public String getFullName() { return firstName + " " + lastName; }
}

package common;

import java.io.Serializable;

/**
 * Represents a traveler (visitor/tourist) logged into the GoNature client.
 * <p>
 * A traveler is identified by their national ID number ({@code idNumber}).
 * If the traveler is also a registered subscriber, {@code subscriberId} holds
 * their member number (otherwise 0). If registered as a tour guide,
 * {@code isGuide} is {@code true}.
 * </p>
 * <p>
 * The {@code familySize} field (set at login from the subscribers table) is used
 * client-side to enforce the family booking size limit.
 * </p>
 *
 * @author Group 11
 */
public class Traveler implements Serializable {
    private static final long serialVersionUID = 1L;
    private String idNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean isGuide;
    private int subscriberId;  // 0 if not subscriber

    /**
     * Creates an empty traveler object.
     */
    public Traveler() {}
    /**
     * Creates a traveler object with personal contact details.
     *
     * @param idNumber the traveler national ID number
     * @param firstName the traveler first name
     * @param lastName the traveler last name
     * @param email the traveler email address
     * @param phone the traveler phone number
     */
    public Traveler(String idNumber, String firstName, String lastName, String email, String phone) {
        this.idNumber = idNumber; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.phone = phone;
    }
    /**
     * Returns the traveler national ID number.
     *
     * @return the traveler ID number
     */
    public String getIdNumber() { return idNumber; }
    
    /**
     * Sets the traveler national ID number.
     *
     * @param idNumber the traveler ID number to set
     */
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    
    /**
     * Returns the traveler first name.
     *
     * @return the traveler first name
     */
    public String getFirstName() { return firstName; }
    
    /**
     * Sets the traveler first name.
     *
     * @param firstName the traveler first name to set
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    /**
     * Returns the traveler last name.
     *
     * @return the traveler last name
     */
    public String getLastName() { return lastName; }
    
    /**
     * Sets the traveler last name.
     *
     * @param lastName the traveler last name to set
     */
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    /**
     * Returns the traveler email address.
     *
     * @return the traveler email address
     */
    public String getEmail() { return email; }
    
    /**
     * Sets the traveler email address.
     *
     * @param email the traveler email address to set
     */
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns the traveler phone number.
     *
     * @return the traveler phone number
     */
    public String getPhone() { return phone; }
    
    /**
     * Sets the traveler phone number.
     *
     * @param phone the traveler phone number to set
     */
    public void setPhone(String phone) { this.phone = phone; }
    
    /**
     * Returns whether the traveler is registered as a tour guide.
     *
     * @return true if the traveler is a guide, otherwise false
     */
    public boolean isGuide() { return isGuide; }
    
    /**
     * Sets whether the traveler is registered as a tour guide.
     *
     * @param guide true if the traveler is a guide, otherwise false
     */
    public void setGuide(boolean guide) { isGuide = guide; }
    
    /**
     * Returns the subscriber identifier of the traveler.
     *
     * @return the subscriber ID, or 0 if the traveler is not a subscriber
     */
    public int getSubscriberId() { return subscriberId; }
    
    /**
     * Sets the subscriber identifier of the traveler.
     *
     * @param subscriberId the subscriber ID to set
     */
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }
    /**
     * Returns the traveler's full name.
     *
     * @return the full name built from first name and last name
     */
    public String getFullName() { return firstName + " " + lastName; }
}

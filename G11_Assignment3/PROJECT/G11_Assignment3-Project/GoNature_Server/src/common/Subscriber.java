package common;

import java.io.Serializable;

/**
 * Represents a family-club subscriber in the GoNature system.
 * <p>
 * Subscribers are registered by a service representative and receive
 * a unique {@code subscriberId} (member number). They enjoy an additional
 * 10% discount on top of the standard booking discount.
 * </p>
 * <p>
 * The {@code familyMembers} field defines the maximum group size allowed
 * for a family-type booking using this subscription.
 * </p>
 *
 * @author Group 11
 */
public class Subscriber implements Serializable {
    private static final long serialVersionUID = 1L;
    private int subscriberId;
    private String idNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private int familyMembers;
    private String creditCard;

    /**
     * Creates an empty subscriber object.
     */
    public Subscriber() {}
    
    /**
     * Returns the subscriber membership identifier.
     *
     * @return the subscriber ID
     */
    public int getSubscriberId() { return subscriberId; }
    
    /**
     * Sets the subscriber membership identifier.
     *
     * @param subscriberId the subscriber ID to set
     */
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }
    
    /**
     * Returns the subscriber national ID number.
     *
     * @return the subscriber ID number
     */
    public String getIdNumber() { return idNumber; }
    
    /**
     * Sets the subscriber national ID number.
     *
     * @param idNumber the subscriber ID number to set
     */
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    
    /**
     * Returns the subscriber first name.
     *
     * @return the subscriber first name
     */
    public String getFirstName() { return firstName; }
    
    /**
     * Sets the subscriber first name.
     *
     * @param firstName the subscriber first name to set
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    /**
     * Returns the subscriber last name.
     *
     * @return the subscriber last name
     */
    public String getLastName() { return lastName; }
    
    /**
     * Sets the subscriber last name.
     *
     * @param lastName the subscriber last name to set
     */
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    /**
     * Returns the subscriber phone number.
     *
     * @return the subscriber phone number
     */
    public String getPhone() { return phone; }
    
    /**
     * Sets the subscriber phone number.
     *
     * @param phone the subscriber phone number to set
     */
    public void setPhone(String phone) { this.phone = phone; }
    
    /**
     * Returns the subscriber email address.
     *
     * @return the subscriber email address
     */
    public String getEmail() { return email; }
    
    /**
     * Sets the subscriber email address.
     *
     * @param email the subscriber email address to set
     */
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns the number of family members included in the subscription.
     *
     * @return the number of family members
     */
    public int getFamilyMembers() { return familyMembers; }
    
    /**
     * Sets the number of family members included in the subscription.
     *
     * @param familyMembers the number of family members to set
     */
    public void setFamilyMembers(int familyMembers) { this.familyMembers = familyMembers; }
    
    /**
     * Returns the subscriber credit card number.
     *
     * @return the credit card number, or null if no card was provided
     */
    public String getCreditCard() { return creditCard; }
    
    /**
     * Sets the subscriber credit card number.
     *
     * @param creditCard the credit card number to set
     */
    public void setCreditCard(String creditCard) { this.creditCard = creditCard; }
}

package common.worker;

import java.io.Serializable;

/**
 * Represents an employee of the GoNature department.
 * <p>
 * All system employees — park workers, park managers, the department manager,
 * and service representatives — are modelled by this class.
 * </p>
 *
 * <p><b>Roles:</b></p>
 * <ul>
 *   <li>{@code park_worker} — handles park entry/exit and walk-in visits</li>
 *   <li>{@code park_manager} — manages park parameters and promotions</li>
 *   <li>{@code department_manager} — approves parameter changes and promotions;
 *       accesses all reports</li>
 *   <li>{@code service_rep} — registers subscribers and guides</li>
 * </ul>
 *
 * <p>The {@code isLoggedIn} flag, stored in the database, prevents the same
 * employee account from being used on two machines simultaneously.</p>
 *
 * @author Group 11
 */
public class GeneralParkWorker implements Serializable {
    private static final long serialVersionUID = 1L;
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;  // park_worker, park_manager, department_manager, service_rep
    private int parkId;   // 0 for department_manager and service_rep
    private String username;
    private String password;
    private boolean isLoggedIn;

    /**
     * Creates an empty park worker object.
     */
    public GeneralParkWorker() {}
    
    /**
     * Returns the employee identifier.
     *
     * @return the employee ID
     */
    public int getEmployeeId() { return employeeId; }
    
    /**
     * Sets the employee identifier.
     *
     * @param employeeId the employee ID to set
     */
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    /**
     * Returns the employee first name.
     *
     * @return the employee first name
     */
    public String getFirstName() { return firstName; }
    
    /**
     * Sets the employee first name.
     *
     * @param firstName the employee first name to set
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    /**
     * Returns the employee last name.
     *
     * @return the employee last name
     */
    public String getLastName() { return lastName; }
    
    /**
     * Sets the employee last name.
     *
     * @param lastName the employee last name to set
     */
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    /**
     * Returns the employee email address.
     *
     * @return the employee email address
     */
    public String getEmail() { return email; }
    
    /**
     * Sets the employee email address.
     *
     * @param email the employee email address to set
     */
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns the employee role.
     *
     * @return the employee role
     */
    public String getRole() { return role; }
    
    /**
     * Sets the employee role.
     *
     * @param role the employee role to set
     */
    public void setRole(String role) { this.role = role; }
    
    /**
     * Returns the park assigned to the employee.
     *
     * @return the assigned park ID
     */
    public int getParkId() { return parkId; }
    
    /**
     * Sets the park assigned to the employee.
     *
     * @param parkId the assigned park ID to set
     */
    public void setParkId(int parkId) { this.parkId = parkId; }
    
    /**
     * Returns the employee username.
     *
     * @return the employee username
     */
    public String getUsername() { return username; }
    
    /**
     * Sets the employee username.
     *
     * @param username the employee username to set
     */
    public void setUsername(String username) { this.username = username; }
    
    /**
     * Returns the employee password.
     *
     * @return the employee password
     */
    public String getPassword() { return password; }
    
    /**
     * Sets the employee password.
     *
     * @param password the employee password to set
     */
    public void setPassword(String password) { this.password = password; }
    
    /**
     * Returns whether the employee is currently logged in.
     *
     * @return true if the employee is logged in, otherwise false
     */
    public boolean isLoggedIn() { return isLoggedIn; }
    
    /**
     * Sets whether the employee is currently logged in.
     *
     * @param loggedIn true if the employee is logged in, otherwise false
     */
    public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }
    
    /**
     * Returns the employee full name.
     *
     * @return the full name built from first name and last name
     */
    public String getFullName() { return firstName + " " + lastName; }
}


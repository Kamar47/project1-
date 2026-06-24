package common.worker;

import java.io.Serializable;

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

    public GeneralParkWorker() {}
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isLoggedIn() { return isLoggedIn; }
    public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }
    public String getFullName() { return firstName + " " + lastName; }
}

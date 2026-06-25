package common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a message exchanged between the GoNature client and server over TCP.
 * <p>
 * Every request from the client and every response from the server is wrapped in
 * a {@code ClientServerMessage}. The message carries a {@link Command} that identifies
 * the operation, and an optional {@code data} payload whose type depends on the command.
 * </p>
 * <p>
 * This class is {@link java.io.Serializable} and is transmitted using the OCSF framework.
 * Both client and server share an identical copy of this class (and all common model classes)
 * to ensure consistent serialization.
 * </p>
 *
 * @author Group 11
 */
public class ClientServerMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Command command;
    private Object data;
    private boolean success;
    private String errorMessage;

    /**
     * Creates a successful client-server message with a command and attached data.
     *
     * @param command the command that identifies the requested operation
     * @param data the data attached to the message
     */
    public ClientServerMessage(Command command, Object data) {
        this.command = command;
        this.data = data;
        this.success = true;
    }
    /**
     * Creates a successful client-server message with a command and no attached data.
     *
     * @param command the command that identifies the requested operation
     */
    public ClientServerMessage(Command command) { this(command, null); }

    /**
     * Creates a successful response message.
     *
     * @param cmd the response command
     * @param data the response data
     * @return a successful client-server message
     */
    public static ClientServerMessage success(Command cmd, Object data) {
        ClientServerMessage msg = new ClientServerMessage(cmd, data);
        msg.setSuccess(true);
        return msg;
    }
    /**
     * Creates a failure response message with an error description.
     *
     * @param error the error message to send to the client
     * @return a failure client-server message
     */
    public static ClientServerMessage failure(String error) {
        ClientServerMessage msg = new ClientServerMessage(Command.FAILURE, null);
        msg.setSuccess(false);
        msg.setErrorMessage(error);
        return msg;
    }
    /**
     * Returns the message data as an ArrayList of the requested type.
     *
     * @param <T> the element type stored in the list
     * @return the message data cast to an ArrayList
     */
    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getDataAsArrayList() { return (ArrayList<T>) data; }
    /**
     * Packs several values into an ArrayList so they can be sent as message data.
     *
     * @param <T> the type of the packed values
     * @param items the values to pack
     * @return an ArrayList containing the given values
     */
    @SafeVarargs
    public static <T> ArrayList<T> packData(T... items) {
        ArrayList<T> list = new ArrayList<>();
        for (T item : items) list.add(item);
        return list;
    }
    /**
     * Returns the command stored in this message.
     *
     * @return the message command
     */
    public Command getCommand() { return command; }
    /**
     * Sets the command of this message.
     *
     * @param command the command to set
     */
    public void setCommand(Command command) { this.command = command; }
    /**
     * Returns the data attached to this message.
     *
     * @return the attached data object
     */
    public Object getData() { return data; }
    /**
     * Sets the data attached to this message.
     *
     * @param data the data object to attach
     */
    public void setData(Object data) { this.data = data; }
    /**
     * Returns whether the message represents a successful operation.
     *
     * @return true if the operation succeeded, otherwise false
     */
    public boolean isSuccess() { return success; }
    /**
     * Sets the success status of this message.
     *
     * @param success true if the operation succeeded, otherwise false
     */
    public void setSuccess(boolean success) { this.success = success; }
    /**
     * Returns the error message stored in this message.
     *
     * @return the error message, or null if no error exists
     */
    public String getErrorMessage() { return errorMessage; }
    /**
     * Sets the error message of this message.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

package common;

import java.io.Serializable;
import java.util.ArrayList;

public class ClientServerMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Command command;
    private Object data;
    private boolean success;
    private String errorMessage;

    public ClientServerMessage(Command command, Object data) {
        this.command = command;
        this.data = data;
        this.success = true;
    }
    public ClientServerMessage(Command command) { this(command, null); }

    public static ClientServerMessage success(Command cmd, Object data) {
        ClientServerMessage msg = new ClientServerMessage(cmd, data);
        msg.setSuccess(true);
        return msg;
    }
    public static ClientServerMessage failure(String error) {
        ClientServerMessage msg = new ClientServerMessage(Command.FAILURE, null);
        msg.setSuccess(false);
        msg.setErrorMessage(error);
        return msg;
    }
    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getDataAsArrayList() { return (ArrayList<T>) data; }
    @SafeVarargs
    public static <T> ArrayList<T> packData(T... items) {
        ArrayList<T> list = new ArrayList<>();
        for (T item : items) list.add(item);
        return list;
    }
    public Command getCommand() { return command; }
    public void setCommand(Command command) { this.command = command; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

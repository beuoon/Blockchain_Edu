package event;

public class MessageEventArgs extends EventArgs {
    private byte[] message;

    public MessageEventArgs(byte[] message) {
        this.message = message;
    }

    public byte[] getMessage() {
        return message;
    }
}

package node.event;

public interface EventListener {
    public void onEvent(String from, byte[] data);
}
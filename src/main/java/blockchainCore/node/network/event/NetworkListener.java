package blockchainCore.node.network.event;

public interface NetworkListener {
    public void Listen(String from, byte[] data);
}
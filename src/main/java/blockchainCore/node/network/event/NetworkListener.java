package blockchainCore.node.network.event;

public interface NetworkListener {
    void Listen(String from, byte[] data);
}
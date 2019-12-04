package blockchainCore.blockchain.event;

public interface SignalListener {
    void onEvent(SignalType type, Object... arvg);
}
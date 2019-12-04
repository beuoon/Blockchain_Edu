package blockchainCore.blockchain.event;


public final class SignalHandler {

    private static SignalListener listener;

    public static synchronized void setListener(SignalListener eventListener) {
        listener=eventListener;
    }

    public static synchronized void removeListener() {
        listener = null;
    }

    public static synchronized void callEvent(SignalType type, Object... arvg) {
        if (listener == null) return ;
        listener.onEvent(type, arvg);
    }
}
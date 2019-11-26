package blockchainCore.node.network.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetworkHandler {

    private static final int MAX_THREAD_POOL = 5;

    private static ConcurrentHashMap<String, NetworkListener> listeners = new ConcurrentHashMap<>();

    private static synchronized ConcurrentHashMap<String,NetworkListener> getListeners() {
        return listeners;
    }

    public static synchronized void addListener(String nodeId, NetworkListener eventListener) {
        if (!listeners.containsKey(nodeId))
            listeners.put(nodeId, eventListener);
    }

    public static synchronized void removeListener(String nodeId) {
        listeners.remove(nodeId);
    }

    public static synchronized boolean callEvent( String from, String to, byte[] data) {
        return callEvent(from, to, data,true);
    }

    public static synchronized boolean callEvent( String from, String to, byte[] data, boolean doAsynch) {
        if (!listeners.containsKey(to)) return false;

        if (doAsynch)   callEventByAsynch(from, to, data);
        else            callEventBySynch(from, to, data);
        return true;
    }

    private static synchronized void callEventByAsynch(String from, String to, final byte[] data) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_POOL);

        NetworkListener listener = listeners.get(to);

        executorService.execute(new Runnable() {
            public void run() {
                listener.Listen(from, data);
            }
        });
        executorService.shutdown();
    }

    private static synchronized void callEventBySynch(String from, String to, final byte[] data) {
        NetworkListener listener = listeners.get(to);
        listener.Listen(from, data);
    }
}
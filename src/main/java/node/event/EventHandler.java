package node.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class EventHandler {

    private static final int MAX_THREAD_POOL = 5;

    private static ConcurrentHashMap<String, EventListener> listeners = new ConcurrentHashMap<>();

    private static synchronized ConcurrentHashMap<String,EventListener> getListeners() {
        return listeners;
    }

    public static synchronized void addListener(String nodeId, EventListener eventListener) {
        if (!listeners.containsKey(nodeId))
            listeners.put(nodeId, eventListener);
    }

    public static synchronized void removeListener(String nodeId) {
        listeners.remove(nodeId);
    }

    public static synchronized boolean callEvent(final Class<?> caller, String from, String to, byte[] data) {
        return callEvent(caller, from, to, data,true);
    }

    public static synchronized boolean callEvent(final Class<?> caller, String from, String to, byte[] data, boolean doAsynch) {
        if (!listeners.containsKey(to)) return false;

        if (doAsynch)   callEventByAsynch(caller, from, to, data);
        else            callEventBySynch(caller, from, to, data);
        return true;
    }

    private static synchronized void callEventByAsynch(final Class<?> caller, String from, String to, final byte[] data) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_POOL);

        EventListener listener = listeners.get(to);
        executorService.execute(new Runnable() {
            public void run() {
                listener.onEvent(from, data);
            }
        });
        executorService.shutdown();
    }

    private static synchronized void callEventBySynch(final Class<?> caller, String from, String to, final byte[] data) {
        EventListener listener = listeners.get(to);
        listener.onEvent(from, data);
    }
}
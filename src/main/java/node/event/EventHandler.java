package node.event;

import node.Mempool;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class EventHandler {

    private static final int MAX_THREAD_POOL = 5;

    private static final Logger LOGGER = Logger.getGlobal();

    /**
     * Note : ArrayList may occur ConcurrentModificationException so using
     * CopyOnWriteArrayList for prevent Exception based on multi thread. Do not
     * use below source code. private static List<EventListener> listeners = new
     * ArrayList<EventListener>();
     */
    private static Mempool<String, EventListener> listeners = new Mempool<>();

    private static synchronized Mempool<String,EventListener> getListeners() {
        return listeners;
    }

    public static synchronized void addListener(String nodeId, EventListener eventListener) {
        if(listeners.get(nodeId) == null) {
            listeners.put(nodeId, eventListener);
        }
    }

    public static synchronized void removeListener(String nodeId, EventListener eventListener) {
        if(listeners.get(nodeId) == null) {
            listeners.remove(nodeId);
        }
    }

    public static synchronized void callEvent(final Class<?> caller, String from, String to, byte[] data) {
        callEvent(caller, from, to, data,true);
    }

    public static synchronized void callEvent(final Class<?> caller, String from, String to, byte[] data, boolean doAsynch) {
        if (doAsynch) {
            callEventByAsynch(caller, from, to, data);
        } else {
            callEventBySynch(caller, from, to, data);
        }
    }

    private static synchronized void callEventByAsynch(final Class<?> caller, String from, String to, final byte[] data) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_POOL);

        EventListener listener = listeners.get(to);
            executorService.execute(new Runnable() {
                public void run() {
                    if (!listener.getClass().getName().equals(caller.getName())) listener.onEvent(from, data);

                }
            });
        executorService.shutdown();
    }

    private static synchronized void callEventBySynch(final Class<?> caller, String from, String to, final byte[] data) {
            EventListener listener = listeners.get(to);
            if (!listener.getClass().getName().equals(caller.getName())) listener.onEvent(from, data);

    }
}
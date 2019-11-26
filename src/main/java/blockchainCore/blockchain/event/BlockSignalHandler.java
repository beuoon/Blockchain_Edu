package blockchainCore.blockchain.event;

import blockchainCore.blockchain.Block;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//blockchainCore.blockchain.event for block generate and transfer.
//if 'to' parameter equals null, that means generate block from 'from'
//if all parameters are filled , that means transfer block from 'from' to 'to'
public final class BlockSignalHandler {

    private static final int MAX_THREAD_POOL = 5;

    private static BlockSignalListener listener;

    public static synchronized void setListener(String nodeId, BlockSignalListener eventListener) {
        if (listener != null)
            listener=eventListener;
    }

    public static synchronized void removeListener(String nodeId) {
        listener = null;
    }

    public static synchronized boolean callEvent( String from, String to, Block block) {
        return callEvent(from, to, block);
    }

    public static synchronized boolean callEvent( String from, String to, Block block, boolean doAsynch) {
        if (listener == null) return false;

        if (doAsynch)   callEventByAsynch(from, to, block);
        else            callEventBySynch(from, to, block);
        return true;
    }

    private static synchronized void callEventByAsynch(String from, String to, Block block) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_POOL);

        executorService.execute(new Runnable() {
            public void run() {
                listener.onEvent(from, to, block);
            }
        });
        executorService.shutdown();
    }

    private static synchronized void callEventBySynch(String from, String to, Block block) {
        listener.onEvent(from, to, block);
    }
}
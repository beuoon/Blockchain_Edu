package blockchainCore.blockchain.event;

import blockchainCore.blockchain.Block;

public interface BlockSignalListener {
    public void onEvent(String from, String to, Block block);
}
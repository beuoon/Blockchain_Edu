package GUI;

import blockchainCore.blockchain.Block;
import blockchainCore.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockTree {
    private final HashMap<String, ArrayList<Block>> tree = new HashMap<>();

    public BlockTree() {
        tree.put("", new ArrayList<>());
    }

    public void put(Block block) {
        tree.get(Utils.toHexString(block.getPrevBlockHash())).add(block);
        tree.put(Utils.toHexString(block.getHash()), new ArrayList<>());
    }

    public boolean contains(Block block) {
        return tree.containsKey(Utils.toHexString(block.getHash()));
    }

    public HashMap<String, ArrayList<Block>> getTree() {
        return tree;
    }
}

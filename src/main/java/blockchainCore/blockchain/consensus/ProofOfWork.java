package blockchainCore.blockchain.consensus;

import blockchainCore.blockchain.Block;
import blockchainCore.utils.Utils;

import java.math.BigInteger;

public class ProofOfWork {
    static final int targetBits = 18;
    static final BigInteger target = new BigInteger("1").shiftLeft(256-targetBits);
    private int lastHeight = -1;

    private static byte[] prepareData(Block block, int nonce) {
        return Utils.bytesConcat(block.getBytesExceptHash(), Integer.toString(targetBits).getBytes(), Integer.toString(nonce).getBytes());
    }

    public boolean mine(Block block) {
        //System.out.println("Mining the block containg," + block.getData());
        byte[] hash = new byte[0];
        int nonce = 0;

        while(nonce < Integer.MAX_VALUE) {
            // 새로운 블록이 오거나 최장길이 갱신시.
            if (block.getHeight() <= lastHeight) return false;

            byte[] data = prepareData(block, nonce);
            hash = Utils.sha256(data);
            // System.out.printf("\r%s", Utils.toHexString(hash));

            BigInteger bihash = new BigInteger(1, hash);
            if( bihash.compareTo(target) == -1 ) break;
            else nonce++;
        }
        lastHeight = block.getHeight();

        block.setHash(hash);
        block.setNonce(nonce);
        // System.out.println();System.out.println();
        return true;
    }

    public void renewLastHeight(int height) { lastHeight = height; }

    public static boolean Validate(Block block){
        BigInteger bihash = new BigInteger(1, block.getHash());
        byte[] data = prepareData(block, block.getNonce());
        byte[] hash = Utils.sha256(data);

        if( bihash.compareTo(target) == -1 ) return true;
        return false;
    }
}

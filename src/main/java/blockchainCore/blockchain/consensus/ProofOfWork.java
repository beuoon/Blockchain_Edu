package blockchainCore.blockchain.consensus;

import blockchainCore.blockchain.Block;
import blockchainCore.utils.Utils;

import java.math.BigInteger;
import java.util.Random;

public class ProofOfWork {
    static final int targetBits = 18;
    static final BigInteger target = new BigInteger("1").shiftLeft(256-targetBits);

    private static byte[] prepareData(Block block, int nonce) {
        return Utils.bytesConcat(block.getBytesExceptHash(), Integer.toString(targetBits).getBytes(), Integer.toString(nonce).getBytes());
    }

    public static void Mine(Block block) {
        int nonce = new Random().nextInt();

        byte[] data = prepareData(block, nonce);
        byte[] hash = Utils.sha256(data);
        System.out.print(Utils.toHexString(hash)+"\n");

        block.setHash(hash);
        block.setNonce(nonce);
    }

    public static boolean Validate(Block block){
        BigInteger bihash = new BigInteger(1, block.getHash());
        byte[] data = prepareData(block, block.getNonce());
        byte[] hash = Utils.sha256(data);

        if( bihash.compareTo(target) == -1 ) return true;
        return false;
    }
}

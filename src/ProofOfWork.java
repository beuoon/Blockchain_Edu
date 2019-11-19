import java.math.BigInteger;

public class ProofOfWork {
    static final int targetBits = 18;
    static final BigInteger target = new BigInteger("1").shiftLeft(256-targetBits);

    private static byte[] prepareData(Block block, int nonce) throws Exception{
        return Utils.bytesConcat(block.getBytesExceptHash(), new Integer(nonce).toString().getBytes());
    }

    public static void mine(Block block) throws Exception {
        System.out.println("Mining the block containg," + block.getData());
        byte[] hash = new byte[0];
        int nonce = 0;

        while(nonce < Integer.MAX_VALUE) {
            byte[] data = prepareData(block, nonce);
            hash = Utils.sha256(data);
            System.out.printf("\r%s", Utils.byteArrayToHexString(hash));

            BigInteger bihash = new BigInteger(1, hash);
            if( bihash.compareTo(target) == -1 ) break;
            else nonce++;
        }

        block.setHash(hash);
        block.setNonce(nonce);
        System.out.println();System.out.println();
    }

    public static boolean validate(Block block) throws Exception {
        BigInteger bihash = new BigInteger(1, block.getHash());
        byte[] data = prepareData(block, block.getNonce());
        byte[] hash = Utils.sha256(data);

        if( bihash.compareTo(target) == -1 ) return true;
        return false;
    }

}

public class Main {
    public static void main(String args[]) throws Exception{
        Blockchain bc = new Blockchain();
        bc.addBlock("Send 1 BTC to Minsung");
        bc.addBlock("Send 2 more BTC to Minsung");

        for(Block b : bc.getBlocks()) {
            System.out.printf("Prev. hash %s\n", Utils.byteArrayToHexString(b.getPrevBlockHash()));
            System.out.printf("Data %s\n", b.getData());
            System.out.printf("Hash %s\n", Utils.byteArrayToHexString(b.getHash()));
            System.out.printf("TimeStamp %d\n", b.getTimestamp());
            System.out.printf("PoW %s\n", ProofOfWork.validate(b));
            System.out.println();
        }

    }
}

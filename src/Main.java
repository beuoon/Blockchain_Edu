import java.util.ArrayList;
import java.util.Iterator;

public class Main {
    public static void main(String args[]) throws Exception{
        /*
        Db db = new Db();
        Blockchain bc = new Blockchain("Minsung", db);
        //bc.MineBlock("Send 1 BTC to Minsung");
        //bc.addBlock("Send 2 more BTC to Minsung");

        Iterator<Block> itr = bc.iterator();
        while(itr.hasNext()) {
            Block b = itr.next();
            System.out.printf("Prev. hash %s\n", Utils.byteArrayToHexString(b.getPrevBlockHash()));
       //     System.out.printf("Data %s\n", b.getData());
            System.out.printf("Hash %s\n", Utils.byteArrayToHexString(b.getHash()));
            System.out.printf("TimeStamp %d\n", b.getTimestamp());
            System.out.printf("PoW %s\n", ProofOfWork.validate(b));
            System.out.println();
        }

        Functions.getBalance("Minsung", bc);
        Functions.send("Minsung", "Hyeon", 40, bc);

        Functions.getBalance("Minsung", bc);
        Functions.getBalance("Hyeon", bc);

         */
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Node node = new Node("Hyeon" + i);
            node.start();
            nodes.add(node);
        }

        while (true) {
            Thread.sleep(1000);
            // TODO: 커맨드 입력
        }

        // for (Node node : nodes) node.close();
    }
}

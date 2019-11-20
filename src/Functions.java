import java.util.ArrayList;

public class Functions {
    public static void getBalance(String address, Blockchain bc) {
        ArrayList<TxOutput> UTOXs = bc.findUTXO(address);

        int balance = 0;

        for(TxOutput out : UTOXs) {
            balance += out.getValue();
        }

        System.out.printf("Balance of '%s' : %d\n", address, balance);
    }

    public static void send(String from, String to, int amount, Blockchain bc) throws Exception {
        Transaction tx = bc.newUTXOTransaction(from, to, amount);
        bc.MineBlock(new Transaction[]{tx});
        System.out.println("Success!");
    }
}

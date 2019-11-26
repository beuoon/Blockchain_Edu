package blockchainCore.blockchain.wallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Wallets {
    private HashMap<String, Wallet> Wallets = new HashMap<String, Wallet>();

    public String createWallet() {
        Wallet wallet = new Wallet();
        String address = wallet.getAddress();

        Wallets.put(address, wallet);

        return address;
    }

    public ArrayList<String> getAddresses() {
        ArrayList<String> addresses = new ArrayList<String>();

        Iterator<String> itr = Wallets.keySet().iterator();

        while(itr.hasNext()){
            addresses.add(itr.next());
        }

        return addresses;
    }

    public Wallet getWallet(String address) {
        return Wallets.get(address);
    }
}

package blockchainCore;

import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.network.Node;
import blockchainCore.utils.Utils;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        /*
            Wallet Challenge!!
            블록체인 지갑을 만들어 보아요!

            blockchainCore.blockchain.wallet.Wallet.java 파일을 수정하여 올바른 지갑을 만들 수 있도록 도와주세요!
            Hint! : ECGenParameterSpec 함수를 사용할 수 있습니다!
         */

        Results results = new Results();
        Wallet wallet = new Wallet();
        for(int i=0; i<results.pubkeys.length; i++){
            if(!wallet.getAddress(Utils.hexToBytes(results.pubkeys[i])).equals(results.result[i])) {
                throw new Exception("YOUR CODE IS NOT VALID!!");
            }
        }

        for(int i=0; i<100; i++) {
            if(!results.validate(wallet.getAddress(Utils.hexToBytes(results.pubkeys[0])))) {
                throw new Exception("YOUR CODE IS NOT VALID!!");
            }
        }

        System.out.println("CONGRATURATION!!! YOU SOLVED IT!!");
    }
}

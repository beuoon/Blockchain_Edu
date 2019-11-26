package blockchainCore;

import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.consensus.ProofOfWork;
import blockchainCore.blockchain.transaction.Transaction;

public class Main {
    public static void main(String[] args) {
        /*
        Prob. Proof Of Work
        blockchain.consensus.ProofOfWork.java 파일의 Mine 함수를 구현하여
        총 10개의 블록이 ProofOfWork.Validate 를 만족하게 하세요.
         */
        final int BLOCK_NUM = 10;

        boolean bClear = true;
        for (int i = 0; i < BLOCK_NUM; i++) {
            Block block = new Block(new Transaction("Hyeon", ""));
            ProofOfWork.Mine(block);
            if (!ProofOfWork.Validate(block)) {
                bClear = false;
                break;
            }
        }

        if (bClear)
            System.out.println("축하합니다!! 문제를 푸셨어요~~");
        else
            System.out.println("안타깝습니다. 좀 더 노력해봐요.");
    }
}

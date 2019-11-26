package blockchainCore.blockchain.wallet;

import org.bitcoinj.core.Base58;
import blockchainCore.utils.Utils;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public class Wallet {
    final byte[] version = new byte[]{0x00};
    final int addressChecksumLen = 4;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Wallet() {
        // implement me
    }


    public String getAddress() {
        return getAddress(publicKey.getEncoded());
    }

    public String getAddress(byte[] publicKey) {
        //implement me
        String s = "implement me";
        return s;
    }


    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}

import org.bitcoinj.core.Base58;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public class Wallet {
    final byte[] version = new byte[]{0x00};
    final int addressChecksumLen = 4;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Wallet() {
        try {
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            KeyPair keyPair = g.generateKeyPair();

            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAddress() {
        byte[] pubkeyHash = Utils.ripemd160(Utils.sha256(publicKey.getEncoded()));
        byte[] versionedPayload = Utils.bytesConcat(version, pubkeyHash);
        byte[] checksum = checksum(versionedPayload);

        byte[] fullPayload = Utils.bytesConcat(versionedPayload, checksum);
        String address = Base58.encode(fullPayload);

        return address;
    }

    private byte[] checksum(byte[] payload) {
        byte[] firstSha256 = Utils.sha256(payload);
        byte[] secondSha256 = Utils.sha256(firstSha256);
        return Arrays.copyOfRange(secondSha256,0, addressChecksumLen);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}

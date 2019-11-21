import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static byte[] sha256(byte[] msg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(msg);

        return md.digest();
    }

    public static byte[] ripemd160(byte[] msg) {
        RIPEMD160Digest d = new RIPEMD160Digest();
        d.update(msg, 0, msg.length);
        byte[] o = new byte[d.getDigestSize()];
        d.doFinal(o, 0);
        return o;
    }

    public static byte[] bytesConcat(byte[]... bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte b[] : bytes) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputStream.toByteArray();
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xff));
        }

        return sb.toString();
    }

    public static byte[] toBytes(Object... o) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static byte[] hashPubKey(byte[] pubkey) {
        return new byte[]{};

    }
}
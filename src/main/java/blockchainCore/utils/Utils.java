package blockchainCore.utils;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.io.*;
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

    public static byte[] bytesConcat(byte[]... bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte[] b : bytes) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputStream.toByteArray();
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xff));
        }

        return sb.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] toBytes(Object o) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;

        byte[] bytes = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            bytes = bos.toByteArray();
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (bos != null) bos.close(); } catch (IOException ignored) {}
            try { if (oos != null) oos.close(); } catch (IOException ignored) {}
        }

        return bytes;
    }
}

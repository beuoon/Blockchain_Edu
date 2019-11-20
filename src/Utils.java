import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static byte[] sha256(byte[] msg){
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

        for(byte b[] : bytes ) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputStream.toByteArray();
    }

    public static String byteArrayToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();

        for(byte b : bytes){
            sb.append(String.format("%02X", b&0xff));
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
        } catch( Exception e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

}

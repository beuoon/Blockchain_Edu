import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static byte[] sha256(byte[] msg) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(msg);

        return md.digest();
    }

    public static byte[] bytesConcat(byte[]... bytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for(byte b[] : bytes ) {
            outputStream.write(b);
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
}

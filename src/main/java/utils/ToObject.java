package utils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

public class ToObject<T> {

    private T obj = null;

    public ToObject(byte[] b) {
        ByteArrayInputStream bis =null;
        ObjectInput in = null;
        try {
            bis = new ByteArrayInputStream(b);
            in = new ObjectInputStream(bis);
            obj = (T)in.readObject();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public T get() {
        return obj;
    }

}

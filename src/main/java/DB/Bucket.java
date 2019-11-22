package DB;

import java.util.HashMap;
import java.util.Iterator;

public class Bucket {
    private HashMap<String, byte[]> db = new HashMap<>();

    public void put(String key, byte[] value){
        db.put(key ,value);
    }
    public byte[] get(String key) {
        return db.get(key);
    }
    public void clear() { db.clear(); }
    public void delete(String key) {
        db.remove(key);
    }
    public Cursor Cursor() {
        return new Cursor(db);
    }

}
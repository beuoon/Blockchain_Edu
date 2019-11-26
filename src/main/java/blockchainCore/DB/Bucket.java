package blockchainCore.DB;

import java.util.concurrent.ConcurrentHashMap;

public class Bucket {
    private ConcurrentHashMap<String, byte[]> db = new ConcurrentHashMap<>();

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
package blockchain;

import java.util.HashMap;

public class Db {
    class Bucket {
        HashMap<String, byte[]> db = new HashMap();
        public void put(String key, byte[] value){
            db.put(key ,value);
        }

        public byte[] get(String key) {
            return db.get(key);
        }
    }

    HashMap<String, Bucket> bucket = new HashMap();

    public Bucket getBucket(String bucket) {
        Bucket db = this.bucket.get(bucket);
        if(db == null) {
            db = new Bucket();
            this.bucket.put(bucket, db);
        }

        return db;
    }
}

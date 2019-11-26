package blockchainCore.DB;

import java.util.concurrent.ConcurrentHashMap;

public class Db {

    private ConcurrentHashMap<String, Bucket> bucket = new ConcurrentHashMap();

    public Bucket getBucket(String bucket) {
        Bucket b = this.bucket.get(bucket);
        if(b == null) {
            b = new Bucket();
            this.bucket.put(bucket, b);
        }
        return b;
    }
}

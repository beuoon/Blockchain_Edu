package blockchainCore.DB;

import java.util.HashMap;

public class Db {

   private  HashMap<String, Bucket> bucket = new HashMap();

    public Bucket getBucket(String bucket) {
        Bucket b = this.bucket.get(bucket);
        if(b == null) {
            b = new Bucket();
            this.bucket.put(bucket, b);
        }
        return b;
    }
}

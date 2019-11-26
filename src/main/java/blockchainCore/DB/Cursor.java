package blockchainCore.DB;

import blockchainCore.utils.Pair;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class Cursor {
    ConcurrentHashMap<String, byte[]> db;
    ArrayList<String> keys;
    int idx = -1;

    public Cursor(ConcurrentHashMap<String, byte[]> db) {
        this.db = db;
        keys = new ArrayList<String>();
        keys.addAll(db.keySet());
    }

    public Pair<String, byte[]> first() {
        idx = 0;
        return new Pair<>(keys.get(idx), db.get(keys.get(idx)));
    }

    public Boolean hasNext() {
        if(idx+1 >= keys.size()) return false;
        return true;
    }


    public Pair<String, byte[]> tail() {
        idx = db.size()-1;
        return new Pair<>(keys.get(idx), db.get(keys.get(idx)));
    }

    public Pair<String, byte[]> get(int idx){
        return new Pair<>(keys.get(idx), db.get(keys.get(idx)));
    }

    public Pair<String, byte[]> next() {
        idx++;
        return new Pair<>(keys.get(idx), db.get(keys.get(idx)));
    }
}

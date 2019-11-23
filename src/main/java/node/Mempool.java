package node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Mempool<K, V>  {
    private Semaphore semaphore = new Semaphore(1);
    private HashMap<K, V> mempool = new HashMap<>();

    public void put(K key, V value) {
        try {
            semaphore.acquire();
            mempool.put(key, value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    public void remove(K key) {
        try {
            semaphore.acquire();
            mempool.remove(key);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    public V get(K key) {
        V value = null;
        try {
            semaphore.acquire();
            value = mempool.get(key);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }

        return value;
    }

    public int size() {
        int size = 0;
        try {
            semaphore.acquire();
            size = mempool.size();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }

        return size;
    }

    public boolean containsKey(K key) {
        boolean bool = false;

        try {
            semaphore.acquire();
            bool = mempool.containsKey(key);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }

        return bool;
    }

    public Collection<V> values() {
        Collection<V> values = null;
        try {
            semaphore.acquire();
            HashMap<K, V> copy = (HashMap<K,V>)mempool.clone();
            values = copy.values();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
        return values;
    }

}

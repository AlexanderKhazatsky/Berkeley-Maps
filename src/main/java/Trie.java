// ADAPTED FROM PRINCETON'S IMPLEMENTATION OF THE TRIEST

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Map;

public class Trie {
    private static final int R = 128;        // extended ASCII
    private Node root;      // root of trie

    // R-way trie node
    private static class Node {
        private TreeSet<String> prefixSet;
        private ArrayList<Map<String, Object>> locations;
        private Node[] next;

        Node() {
            prefixSet = new TreeSet<>();
            locations = new ArrayList<>();
            next = new Node[R];
        }
    }

    /**
     * Initializes an empty string symbol table.
     */
    public Trie() {
    }


    /**
     * Returns the value associated with the given key.
     * @param key the key
     * @return the value associated with the given key if the key is in the symbol table
     *     and {@code null} if the key is not in the symbol table
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public TreeSet<String> getPrefixes(String key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }
        return get(root, key, 0).prefixSet;
    }

    public ArrayList<Map<String, Object>> getMatches(String key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }
        return get(root, key, 0).locations;
    }

    public boolean contains(String key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to contains() is null");
        }
        return getMatches(key) == null || getMatches(key).isEmpty();
    }

    private Node get(Node x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        char c = key.charAt(d);
        return get(x.next[c], key, d + 1);
    }

    /**
     * Inserts the key-value pair into the symbol table, overwriting the old value
     * with the new value if the key is already in the symbol table.
     * If the value is {@code null}, this effectively deletes the key from the symbol table.
     * @param key the key
     * @param val the value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public void put(String key, GraphDB.Node val) {
        if (key == null) {
            throw new IllegalArgumentException("first argument to put() is null");
        }
        root = put(root, key, val, 0);
    }

    private Node put(Node x, String key, GraphDB.Node val, int d) {
        if (x == null) {
            x = new Node();
        }
        x.prefixSet.add(val.name);
        if (d == key.length()) {
            TreeMap<String, Object> locationInfo = new TreeMap<>();
            locationInfo.put("lat", val.lat);
            locationInfo.put("lon", val.lon);
            locationInfo.put("name", val.name);
            locationInfo.put("id", val.v);
            x.locations.add(locationInfo);
            return x;
        }
        char c = key.charAt(d);
        x.next[c] = put(x.next[c], key, val, d + 1);
        return x;
    }

    public Iterable<String> keys() {
        return getPrefixes("");
    }
}

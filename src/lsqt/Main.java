package lsqt;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class Main {
    static private ArrayList<IntIndex> keys = new ArrayList<>();
    static private Skiplist<IntIndex, Character> sltest = new Skiplist<>();
    static private TreeMap<IntIndex, Character> control = new TreeMap<>();
    static private Random RNG = new Random();

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 243; i++) {
            keys.add(new IntIndex(RNG.nextInt()));
            add(keys.get(i), (char) (RNG.nextInt(26) + 'A'));
            //add(keys.get(i), (char) (i + '!'));
        }
        delete();
        for (IntIndex k : control.keySet()) {
            if (control.get(k).charValue() != sltest.get(k).charValue()) {
                //Happens because Treemap cannot have duplicate keys and we handle them by just inserting anyways for now
                throw new Exception("Control vs Skiplist mismatch! Key:" + k.toString() + ", Value: " + control.get(k));
            }
        }
        sltest.printlist();
        System.out.print("End");
    }

    static void add(Index index, char character) {
        control.put((IntIndex) index, character);
        sltest.add((IntIndex) index, character);
    }

    static void delete() {
        IntIndex randRemoveKey;
        int randRemove;
        for (int i = 0; i < 27; i++) {
            randRemove = RNG.nextInt(keys.size());
            //randRemoveKey = keys.get(8);
            randRemoveKey = keys.get(randRemove);
            keys.remove(randRemove);
            control.remove(randRemoveKey);
            sltest.remove(randRemoveKey);
        }
    }
}

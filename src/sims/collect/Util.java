package sims.collect;

import java.util.*;
import peersim.core.*;

public class Util {
    static private Random rand = new Random();

    static public Set<Node> PickupNeighbors(int k, Linkable linkable) {
        int[] peerids = pickup(k, linkable.degree());
        Set<Node> out = new HashSet<Node>();
        for (int i=0; i<peerids.length; i++) {
            out.add(linkable.getNeighbor(i));
        }
        return out;
    }
    
    static private int[] pickup(int k, int n) {
        int[] out =  new int[n];
        for (int i=0; i<n; i++) {
            out[i] = i;
        }
        k = k <= 0 ? 0 : k;
        k = k >= n ? n : k;
        for (int i=0; i<k; i++) {
            int j = i+rand.nextInt(n-i);
            int tmp = out[j];
            out[j] = out[i];
            out[i] = tmp;
        }
        return Arrays.copyOfRange(out, 0, k);
    }
}

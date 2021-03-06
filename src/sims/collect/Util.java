package sims.collect;

import java.util.*;
import java.lang.Class;
import peersim.core.*;

public class Util {

    static public Set<Node> pickupNeighbors(int k, Linkable linkable) {
        int[] peerids = pickup(k, linkable.degree());
        Set<Node> out = new HashSet<Node>();
        for (int i=0; i<peerids.length; i++) {
            out.add(linkable.getNeighbor(i));
        }
        return out;
    }
    
    static public int[] pickup(int k, int n) {
        int[] out =  new int[n];
        for (int i=0; i<n; i++) {
            out[i] = i;
        }
        k = k <= 0 ? 0 : k;
        k = k >= n ? n : k;
        for (int i=0; i<k; i++) {
            int j = i+CommonState.r.nextInt(n-i);
            int tmp = out[j];
            out[j] = out[i];
            out[i] = tmp;
        }
        return Arrays.copyOfRange(out, 0, k);
    }

    static public int[] pickupWithout(int k, int n, int[] without) {
        int[] out =  new int[n];
        for (int i=0; i<n; i++) {
            out[i] = i;
        }
        for (int i=0; i<without.length; i++) {
            int w = without[i];
            int v = n-1-i;
            int tmp = out[w];
            out[w] = out[v];
            out[v] = tmp;
        }
        n = n-without.length;
        k = k <= 0 ? 0 : k;
        k = k >= n ? n : k;
        for (int i=0; i<k; i++) {
            int j = i+CommonState.r.nextInt(n-i);
            int tmp = out[j];
            out[j] = out[i];
            out[i] = tmp;
        }
        return Arrays.copyOfRange(out, 0, k);
    }

    static Protocol GetNodeProtocol(Node node, Class<?> c) {
        for (int i =0; i<node.protocolSize(); i++) {
            Protocol cand = node.getProtocol(i);
            if (c.isInstance(cand)){
                return cand;
            }
        }
        return null;
    }

}

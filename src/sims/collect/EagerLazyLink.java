package sims.collect;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * EagerLazyLink is the protocol that manages links in plumtree.
 */
public class EagerLazyLink implements Protocol, Linkable {

/*============================================================================*/
// parameters
/*============================================================================*/

    private static int DEFAULT_EAGER_CAPACITY = 8;

    private static int DEFAULT_LAZY_CAPACITY = 2;

    private static final String PAR_EAGERCAP = "eagercapacity";

    private static final String PAR_LAZYCAP = "lazycapacity";

/*============================================================================*/
// fields
/*============================================================================*/

    private List<Node> eagerPeers, lazyPeers;

    private int eagerCap;

    private int lazyCap;

/*============================================================================*/
// initialization
/*============================================================================*/

    public EagerLazyLink(String s) {
        this.eagerCap = Configuration.getInt(s + "." + PAR_EAGERCAP, DEFAULT_EAGER_CAPACITY);
        this.lazyCap = Configuration.getInt(s + "." + PAR_LAZYCAP, DEFAULT_LAZY_CAPACITY);
        this.eagerPeers = new ArrayList<>();
        this.lazyPeers = new ArrayList<>();
    }

/*============================================================================*/
// methods
/*============================================================================*/


    public List<Node> getEagerPeers() {
        return eagerPeers;
    }

    public List<Node> getLazyPeers() {
        return lazyPeers;
    }

    public int getLazyCap() {
        return lazyCap;
    }

    public int getEagerCap() {
        return eagerCap;
    }

    public void graft(Node node) {
        eagerPeers.add(node);
        lazyPeers.remove(node);
    }

    public void prune(Node node) {
        eagerPeers.remove(node);
        lazyPeers.remove(node);
    }

    public void del(Node node) {
        eagerPeers.remove(node);
        lazyPeers.remove(node);
    }

    @Override
    public boolean contains(Node neighbor) {
        return lazyPeers.contains(neighbor) || eagerPeers.contains(neighbor);
    }

    @Override
    public void pack() {
        //nothing to compress
    }

    @Override
    public boolean addNeighbor(Node neighbour) {
        if (eagerPeers.contains(neighbour)) {
            return false;
        }
        //先都加到eager中，自适应到lazy中
        eagerPeers.add(neighbour);
        return true;
    }

    @Override
    public int degree() {
        return eagerPeers.size() + lazyPeers.size();
    }

    @Override
    public Node getNeighbor(int i) {
        return (i < eagerPeers.size() ? eagerPeers.get(i) : lazyPeers.get(i - eagerPeers.size()));
    }

    @Override
    public void onKill() {
        eagerCap = 0;
        lazyCap = 0;
        eagerPeers = null;
        lazyPeers = null;
    }

    @Override
    public String toString() {
        return "lazy peers : " + lazyPeers.toString() + " eager peers : " + eagerPeers.toString();
    }

    @Override
    public Object clone() {
        EagerLazyLink elp = null;
        try {
            elp = (EagerLazyLink) super.clone();
        } catch (CloneNotSupportedException e) {
        } // never happens
        elp.eagerCap = this.eagerCap;
        elp.eagerPeers = new ArrayList<>(this.eagerPeers);
        elp.lazyCap = this.lazyCap;
        elp.lazyPeers = new ArrayList<>(this.lazyPeers);
        return elp;
    }

}

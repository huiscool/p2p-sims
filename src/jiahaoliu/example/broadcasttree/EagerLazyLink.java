/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package jiahaoliu.example.broadcasttree;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * A protocol that stores links. It does nothing apart from that.
 * It is useful to model a static link-structure
 * (topology). The only function of this protocol is to serve as a source of
 * neighborhood information for other protocols.
 */
public class EagerLazyLink implements Protocol, Linkable {

// --------------------------------------------------------------------------
// Parameters
// --------------------------------------------------------------------------


    private static int DEFAULT_EAGER_CAPACITY = 8;

    private static int DEFAULT_LAZY_CAPACITY = 2;

    private static int DEFAULT_PASSIVE_CAPACITY = 10;

    public int passiveSize;

    private static final String PAR_EAGERCAP = "eagercapacity";

    private static final String PAR_LAZYCAP = "lazycapacity";

    private static final String PAR_PASSIVECAP = "passivecap";

    // --------------------------------------------------------------------------
// Fields
// --------------------------------------------------------------------------

    private List<Node> eagerPeers, lazyPeers;

    public List<Node> passiveNodes;

    private int eagerCap;

    private int lazyCap;


// --------------------------------------------------------------------------
// Initialization
// --------------------------------------------------------------------------

    public EagerLazyLink(String s) {
        this.eagerCap = Configuration.getInt(s + "." + PAR_EAGERCAP, DEFAULT_EAGER_CAPACITY);
        this.lazyCap = Configuration.getInt(s + "." + PAR_LAZYCAP, DEFAULT_LAZY_CAPACITY);
        this.passiveSize = Configuration.getInt(s + "." + PAR_PASSIVECAP, DEFAULT_PASSIVE_CAPACITY);
        this.eagerPeers = new ArrayList<>();
        this.lazyPeers = new ArrayList<>();
        this.passiveNodes = new ArrayList<>();
    }

//--------------------------------------------------------------------------

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
        elp.passiveNodes = new ArrayList<>(this.passiveNodes);
        return elp;
    }

// --------------------------------------------------------------------------
// Methods
// --------------------------------------------------------------------------

    public List<Node> getEagerPeers() {
        return eagerPeers;
    }

    public List<Node> getLazyPeers() {
        return lazyPeers;
    }

    // --------------------------------------------------------------------------
    @Override
    public boolean contains(Node neighbor) {
        return lazyPeers.contains(neighbor) || eagerPeers.contains(neighbor);
    }

    @Override
    public void pack() {
        //nothing to compress
    }

// --------------------------------------------------------------------------

    public int getLazyCap() {
        return lazyCap;
    }

    public int getEagerCap() {
        return eagerCap;
    }

    @Override
    public boolean addNeighbor(Node neighbour) {
        //先都加到eager中，自适应到lazy中
//        if (!eagerPeers.contains(neighbour) && eagerPeers.size() < eagerCap) {
//            eagerPeers.add(neighbour);
//        } else {
//            lazyPeers.add(neighbour);
//        }
        eagerPeers.add(neighbour);
        return true;
    }

    // --------------------------------------------------------------------------
    @Override
    public int degree() {
        return eagerPeers.size() + lazyPeers.size();
    }

    @Override
    public Node getNeighbor(int i) {
        return (i < eagerPeers.size() ? eagerPeers.get(i) : lazyPeers.get(i - eagerPeers.size()));
    }

// --------------------------------------------------------------------------

//    public void pack() {
//        if (len == neighbors.length)
//            return;
//        Node[] temp = new Node[len];
//        System.arraycopy(neighbors, 0, temp, 0, len);
//        neighbors = temp;
//    }

// --------------------------------------------------------------------------

    public String toString() {
        return "lazy peers : " + lazyPeers.toString() + " eager peers : " + eagerPeers.toString();
    }

// --------------------------------------------------------------------------

    public void onKill() {
        eagerCap = 0;
        lazyCap = 0;
        eagerPeers = null;
        lazyPeers = null;
    }

}

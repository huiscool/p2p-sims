package sims.pingpong;

import peersim.config.FastConfig;
import peersim.core.*;
import peersim.cdsim.CDProtocol;

/**
 * Pingpong
 */
public class Pingpong extends IdleProtocol implements CDProtocol {
    public boolean HasBall;
    
    public Pingpong(String prefix) {
        super(prefix);
        this.HasBall = false;
    }

    public void nextCycle(Node node, int protocolID) {
        // current node don't have any ball, just return
        if (!this.HasBall) {
            return;
        }
        // find a neighbor and send a pingpong ball to him
        // get a linkable protocol to get neighbors
        int linkID = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkID);
        Pingpong neigh = null;
        for (int i = 0; i<linkable.degree(); i++) {
            Node cand = linkable.getNeighbor(i);
            if (cand.isUp()) {
                continue;
            }
            neigh = (Pingpong) cand.getProtocol(protocolID);
            break;
        }
        if (neigh == null) {
            return;
        }
        // send balls
        this.HasBall = false;
        neigh.HasBall = true;
    }

}
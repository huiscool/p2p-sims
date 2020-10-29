package sims.collect;

import peersim.util.*;
import peersim.core.*;
import peersim.config.*;

public class PlumtreeObserver extends BroadcastObserver {

/*============================================================================*/
// fields
/*============================================================================*/

protected static IncrementalStats gossipStats = new IncrementalStats();
protected static IncrementalStats graftStats = new IncrementalStats();
protected static IncrementalStats pruneStats = new IncrementalStats();
protected static IncrementalStats ihaveStats = new IncrementalStats();

/*============================================================================*/
// constructor
/*============================================================================*/

public PlumtreeObserver(String prefix) {
    super(prefix);
}

public boolean execute() {
    super.execute();
    System.out.printf("gossip=%d,graft=%d,prune=%d,ihave=%d%n",
        gossipStats.getN(),
        graftStats.getN(),
        pruneStats.getN(),
        ihaveStats.getN()
    );
    // observe topology
   IncrementalStats eager = new IncrementalStats();
   IncrementalStats lazy = new IncrementalStats();
   IncrementalStats fanout = new IncrementalStats();

   for (int i=0; i<Network.size(); i++) {
      Node n = Network.get(i);
      // PlumtreeProtocol pp = (PlumtreeProtocol) n.getProtocol(protocolID);
      int linkableID = FastConfig.getLinkable(protocolID);
      EagerLazyLink ell = (EagerLazyLink) n.getProtocol(linkableID);
      eager.add(ell.getEagerPeers().size());
      lazy.add(ell.getLazyPeers().size());
      fanout.add(ell.degree());
   }

   System.out.printf("avgEager=%f,avgLazy=%f,avgFanout=%f%n",
      eager.getAverage(),
      lazy.getAverage(),
      fanout.getAverage()
   );
    return false;
}

public static void handleSendMsg(int protocolID, Node from, Node to, Message msg) {
    msgSendStats.add(msg.size);
    PlumtreeMessage pmsg = (PlumtreeMessage) msg;
    if (pmsg.isGossip) {
        gossipStats.add(1);
    }
    if (pmsg.isGraft) {
        graftStats.add(1);
    }
    if (pmsg.isPrune) {
        pruneStats.add(1);
    }
    if (pmsg.isIHave) {
        ihaveStats.add(1);
    }
 }
 
public static void handleRecvMsg(int protocolID, Node from, Node to, Message msg) {
    msgRecvStats.add(msg.size);
    msgHopStats.add(msg.hop);
 }

}

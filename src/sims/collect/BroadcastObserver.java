package sims.collect;


import peersim.core.Control;
import peersim.core.Network;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Node;
import peersim.util.*;

public class BroadcastObserver implements Control{

/*============================================================================*/
// parameters
/*============================================================================*/
private static final String PARAM_PROTOCOL = "protocol";

/*============================================================================*/
// fields
/*============================================================================*/

protected int protocolID;

protected static IncrementalStats msgSendStats = new IncrementalStats();
protected static IncrementalStats msgRecvStats = new IncrementalStats();
protected static IncrementalStats msgHopStats = new IncrementalStats();

/*============================================================================*/
// constructor
/*============================================================================*/

public BroadcastObserver(String prefix) {
   this.protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
}

/*============================================================================*/
// methods
/*============================================================================*/

public boolean execute() {
   // print stats
   System.out.printf("totalSent=%d,totalRecv=%d,avgHop=%f,maxHop=%f%n", 
      msgSendStats.getN(),
      msgRecvStats.getN(),
      msgHopStats.getAverage(),
      msgHopStats.getMax()
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
}

public static void handleRecvMsg(int protocolID, Node from, Node to, Message msg) {
   msgRecvStats.add(msg.size);
   msgHopStats.add(msg.hop);
}

}
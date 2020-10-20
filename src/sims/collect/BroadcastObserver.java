package sims.collect;

import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.Control;
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

private int protocolID;

static private IncrementalStats msgStats = new IncrementalStats();
static private HashMap<Integer, Integer> nodeRecvs = new HashMap<>();

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
   System.out.println("message sent:"+ msgStats.getN());
   System.out.println("node received:"+nodeRecvs.size());
   return false;
}

/*============================================================================*/
// static functions
// they are the callbacks to notify observer after messages have been sent.
/*============================================================================*/

static public void handleSendMsg(int protocolID, Node from, Node to, Message msg) {
   msgStats.add(msg.GetSize());
   // nodeStats.add(to.getIndex());
   Integer recv = (Integer)nodeRecvs.getOrDefault((int)to.getID(), 0);
   nodeRecvs.put( (int)to.getID(), recv+1);
}

}
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

static private IncrementalStats msgSizeStats = new IncrementalStats();
static private IncrementalStats msgHopStats = new IncrementalStats();
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
   System.out.printf("msgTotal=%d,", msgSizeStats.getN());
   System.out.printf("averageHop=%f,", msgHopStats.getAverage());
   System.out.printf("maxHop=%f%n", msgHopStats.getMax());
   return false;
}

/*============================================================================*/
// static functions
// they are the callbacks to notify observer after messages have been sent.
/*============================================================================*/

static public void handleSendMsg(int protocolID, Node from, Node to, Message msg) {
   msgSizeStats.add(msg.size);
   msgHopStats.add(msg.hop);
   Integer recv = (Integer)nodeRecvs.getOrDefault(to.getIndex(), 0);
   nodeRecvs.put(to.getIndex(), recv+1);
}

static public void handleRecvMsg(int protocolID, Node from, Node to, Message msg) {
   
}

}
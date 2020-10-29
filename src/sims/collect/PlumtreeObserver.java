package sims.collect;

import peersim.util.*;
import peersim.core.*;

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

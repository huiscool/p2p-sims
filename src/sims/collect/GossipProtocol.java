package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.IdleProtocol;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

// GossipProtocol: a simple implementation of gossip protocol
public class GossipProtocol extends IdleProtocol implements CDProtocol, Deliverable {

/*============================================================================*/
// parameters
/*============================================================================*/
private static final String PARAM_FANOUT = "fanout";


/*============================================================================*/
// fields
/*============================================================================*/
private int fanout; 
private Deque<Message> mailbox; // the messages in mailbox
private Set<Message> seen;

/*============================================================================*/
// constructor
/*============================================================================*/
public GossipProtocol(String prefix) {
    super(prefix);
    fanout = Configuration.getInt(prefix + "." + PARAM_FANOUT);
    mailbox = new LinkedList<>();
    seen = new HashSet<>();
}


/*============================================================================*/
// methods
/*============================================================================*/

@Override
public void deliver(Message msg) {
    mailbox.add(msg);
}

@Override
public void nextCycle(Node node, int protocolID) {

    // in each cycle we do:
    // 1. open mailbox and check any unhandle messages;
    // 2. if receive a seen message, drop it;
    // 3. if receive an unseen message, resend it to serveral neighbors.

    // get linkable
    int linkableID = FastConfig.getLinkable(protocolID);
    Linkable linkable = (Linkable) node.getProtocol(linkableID);

    // for each unhandle message
    while (!mailbox.isEmpty()) {
        
        Message msg = mailbox.pop();
        if (seen.contains(msg)) {
            // System.out.println(node.getID()+": msg seen");
            continue;
        }
        // System.out.println(node.getID()+": msg not seen");
        Set<Node> neighbors = Util.pickupNeighbors(fanout, linkable);
        for (Node neigh : neighbors) {
            sendMsg(protocolID, node, neigh, msg);
        }
    }
}

@Override
public Object clone() {
    GossipProtocol gp = (GossipProtocol) super.clone();
    gp.fanout = this.fanout;
    gp.mailbox = new LinkedList<>();
    gp.seen = new HashSet<>();
    return gp;
}

/*============================================================================*/
// helpers
/*============================================================================*/

static private void sendMsg(int protocolID, Node from, Node to, Message msg) {
    GossipProtocol pfrom = (GossipProtocol) from.getProtocol(protocolID);
    GossipProtocol pto = (GossipProtocol) to.getProtocol(protocolID);
    senderHandleSendMsg(pfrom, msg);
    receiverHandleSendMsg(pto, msg);
    // notify the observer
    notifyObserverSendMsg(protocolID, from, to, msg);
}

static private void senderHandleSendMsg(GossipProtocol from, Message msg) {
    // add message to seen
    if (!from.seen.contains(msg)) {
        from.seen.add(msg);
    }
}

static private void receiverHandleSendMsg(GossipProtocol to, Message msg) {
    // deliver the message
    to.deliver(msg);
}

static private void notifyObserverSendMsg(int protocolID, Node from, Node to, Message msg) {
    BroadcastObserver.handleSendMsg(protocolID, from, to, msg);
}

}

interface Deliverable {
    void deliver(Message msg);
}
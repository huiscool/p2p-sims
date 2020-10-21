package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.IdleProtocol;
import peersim.core.Linkable;
import peersim.core.Network;
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

    if (!node.isUp()) {
        // don't process message if node is down
        return;
    }

    // for each unhandle message
    for (Message incoming : mailbox) {

        // notify the observer
        Node from = Network.get(incoming.fromNodeIndex);
        BroadcastObserver.handleRecvMsg(protocolID, from, node, incoming);

        if (seen.contains(incoming)) {
            continue;
        }
        seen.add(incoming);

        // write an outgoing mail
        Message outgoing = (Message) incoming.clone();
        outgoing.hop += 1;
        outgoing.fromNodeIndex = node.getIndex();

        Set<Node> neighbors = Util.pickupNeighbors(fanout, linkable);
        for (Node neigh : neighbors) {

            // notify the observer
            BroadcastObserver.handleSendMsg(protocolID, node, neigh, outgoing);

            // deliver to neighs
            GossipProtocol to = (GossipProtocol) neigh.getProtocol(protocolID);
            to.deliver(outgoing);
        }
    }
    mailbox.clear();
}

@Override
public Object clone() {
    GossipProtocol that = (GossipProtocol) super.clone();
    that.fanout = this.fanout;
    that.mailbox = new LinkedList<>(this.mailbox);
    that.seen = new HashSet<>(this.seen);
    return that;
}

}

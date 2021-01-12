package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.IdleProtocol;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

// GossipQueryProtocol: a simple implementation of gossip protocol
public class GossipQueryProtocol extends IdleProtocol implements CDProtocol, Deliverable, CollectRoutable {

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

private Map<Integer, Node> fathers;
private RequestHandler reqHandler;

/*============================================================================*/
// constructor
/*============================================================================*/
public GossipQueryProtocol(String prefix) {
    super(prefix);
    fanout = Configuration.getInt(prefix + "." + PARAM_FANOUT);
    mailbox = new LinkedList<>();
    seen = new HashSet<>();

    fathers = new HashMap<>();
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

    // set request handler
    if (reqHandler == null) {
        reqHandler = (RequestHandler) Util.GetNodeProtocol(node, GossipCollectProtocol.class);
    }

    // for each unhandle message
    for (Message incoming : mailbox) {

        // notify the observer
        Node from = incoming.from;
        QueryObserver.handleRecvRequest(incoming, incoming.from, node);

        if (seen.contains(incoming)) {
            continue;
        }
        seen.add(incoming);

        fathers.put(incoming.id, from);

        reqHandler.handleRequest(node, incoming, linkable);

        // write an outgoing mail
        Message outgoing = (Message) incoming.hopFrom(node);

        Set<Node> neighbors = Util.pickupNeighbors(fanout, linkable);
        for (Node neigh : neighbors) {
            // deliver to neighs
            GossipQueryProtocol to = (GossipQueryProtocol) neigh.getProtocol(protocolID);
            to.deliver(outgoing);
            QueryObserver.handleSendRequest(outgoing, node, neigh);
        }
    }
    mailbox.clear();
}

@Override
public Node getFather(Message msg) {
    return fathers.get(msg.id);
}

@Override
public Object clone() {
    GossipQueryProtocol that = (GossipQueryProtocol) super.clone();
    that.fanout = this.fanout;
    that.mailbox = new LinkedList<>(this.mailbox);
    that.seen = new HashSet<>(this.seen);
    that.fathers = new HashMap<>(this.fathers);

    return that;
}

}

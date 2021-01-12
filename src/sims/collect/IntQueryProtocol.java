package sims.collect;

import java.util.*;
import peersim.core.*;
import peersim.config.*;
import peersim.cdsim.CDProtocol;

public class IntQueryProtocol extends IdleProtocol implements CDProtocol, Deliverable, CollectRoutable {
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
private PeerRecommender recommender;


/*============================================================================*/
// constructor
/*============================================================================*/
public IntQueryProtocol(String prefix) {
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
    // get linkable
    int linkableID = FastConfig.getLinkable(protocolID);
    Linkable linkable = (Linkable) node.getProtocol(linkableID);

    // set recommender
    if (this.recommender == null) {
        this.recommender = (PeerRecommender) Util.GetNodeProtocol(node, IntCollectProtocol.class);
    }
    if (this.reqHandler == null) {
        this.reqHandler = (RequestHandler) Util.GetNodeProtocol(node, IntCollectProtocol.class);
    }

    if (!node.isUp()) {
        // don't process message if node is down
        return;
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
        reqHandler.handleRequest(node, incoming);

        // write an outgoing mail
        Message outgoing = (Message) incoming.hopFrom(node);

        Set<Node> neighbors = recommender.GetRecommendations(incoming, fanout, linkable);
        for (Node neigh : neighbors) {
            // deliver to neighs
            IntQueryProtocol to = (IntQueryProtocol) neigh.getProtocol(protocolID);
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
    IntQueryProtocol that = (IntQueryProtocol) super.clone();
    that.fanout = this.fanout;
    that.mailbox = new LinkedList<>(this.mailbox);
    that.seen = new HashSet<>(this.seen);
    that.fathers = new HashMap<>(this.fathers);

    return that;
}
   
}

interface PeerRecommender {
    Set<Node> GetRecommendations(Message msg, int k, Linkable linkable);
}

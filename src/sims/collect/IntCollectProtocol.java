package sims.collect;

import java.util.*;

import peersim.cdsim.*;
import peersim.config.Configuration;
import peersim.core.*;

public class IntCollectProtocol implements CDProtocol, HitsConfigurable, Deliverable, RequestHandler, PeerRecommender {

/*============================================================================*/
// parameters
/*============================================================================*/
private static String PARAM_ROUTER = "router";


/*============================================================================*/
// fields
/*============================================================================*/
private List<Message> mailbox;

private HashMap<Node, Set<Message>> querys;

private int routerID;

private boolean isHit;

/*============================================================================*/
// constructor
/*============================================================================*/
public IntCollectProtocol(String prefix) {
    mailbox = new LinkedList<>();
    querys = new HashMap<>();
    routerID = Configuration.getPid(prefix + "." + PARAM_ROUTER);
    isHit = false;
   
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
    if (!node.isUp()) {
        return;
    }

    IntQueryProtocol router = (IntQueryProtocol) node.getProtocol(routerID);

    for(Message msg : mailbox) {
        switch (msg.type) {
            case Response:
                QueryObserver.handleRecvResponse(msg, msg.from, node);
                handleResponse(router, protocolID, msg.from, node, msg);
                break;
            case Control:
                QueryObserver.handleRecvControl(msg, msg.from, node);
                handleControl(router, protocolID, msg.from, node, msg);
            case Request:
            default:
        }
    }
    mailbox.clear();

}

@Override
public void handleRequest(Node node, Message msg) {
    assert(msg.type == MessageType.Request);
    if (isHit) {
        QueryObserver.handleHit(msg, node);

        CollectRoutable router = (CollectRoutable) node.getProtocol(routerID);
        Node to = router.getFather(msg);
        IntCollectProtocol pto = (IntCollectProtocol) Util.GetNodeProtocol(to, IntCollectProtocol.class);

        Message outgoing = (Message) msg.hopFrom(node);
        outgoing.type = MessageType.Response;
        outgoing.collectedHits = 1;
        pto.deliver(outgoing);
        QueryObserver.handleSendResponse(outgoing, node, to);

        // deliver control message to all peers
        Linkable linkable = (Linkable) Util.GetNodeProtocol(to, Linkable.class);
        Util.pickupNeighbors(linkable.degree(), linkable).
        forEach(neigh -> {
            if (neigh == to) {
                // we have send it a response
                return;
            }
            IntCollectProtocol pneigh = (IntCollectProtocol) Util.GetNodeProtocol(neigh, IntCollectProtocol.class);
            Message ctrl = (Message) msg.hopFrom(node);
            ctrl.type = MessageType.Control;
            pneigh.deliver(ctrl);
            QueryObserver.handleSendControl(ctrl, node, neigh);
        });
    }
}

@Override
public Set<Node> GetRecommendations(Message msg, int k, Linkable linkable) {
    if (k <= 0) {
        return new HashSet<Node>();
    }
    if (k >= linkable.degree()) {
        // all peers
        return Util.pickupNeighbors(k, linkable);
    }

    // sort 
    NodeQuery[] neighs = new NodeQuery[linkable.degree()];
    for(int i=0; i<linkable.degree(); i++) {
        Node neigh = linkable.getNeighbor(i);
        int score = this.querys.getOrDefault(neigh, new HashSet<>()).size();
        neighs[i] = new NodeQuery(neigh, score);
    }
    Arrays.sort(neighs);
    Set<Node> out = new HashSet<Node>();
    for (int i=0; i<k-1; i++) {
        out.add(neighs[i].node);
    }
    // pick up a random one
    int index = k-1 + Util.pickup(1, linkable.degree()-k+1)[0];
    out.add(neighs[index].node);
    return out;

}

@Override
public void SetHit() {
    isHit = true;
}

@Override
public Object clone() {
    try {
        IntCollectProtocol that = (IntCollectProtocol) super.clone();
        that.mailbox = new LinkedList<>(this.mailbox);
        that.querys = new HashMap<>();
        for(Map.Entry<Node, Set<Message>> e : this.querys.entrySet()) {
            that.querys.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        that.isHit = this.isHit;
        return that;
    }catch(CloneNotSupportedException e) {
        e.printStackTrace();
    }
    return null;
}

/*============================================================================*/
// helper
/*============================================================================*/
private void handleResponse(
    CollectRoutable router,
    int protocolID,
    Node from,
    Node node,
    Message msg
) {
    if (node == msg.root) {
        QueryObserver.handleQuerySuccess(msg, node);
        return;
    }

    // update querys
    addQueryInfo(msg);

    // deliver response
    Node to = router.getFather(msg);
    IntCollectProtocol pto = (IntCollectProtocol) to.getProtocol(protocolID);

    Message outgoing = msg.hopFrom(node);
    pto.deliver(outgoing);
    QueryObserver.handleSendResponse(outgoing, node, to);
}

private void handleControl(
    CollectRoutable router,
    int protocolID,
    Node from,
    Node node,
    Message msg
) {
    addQueryInfo(msg);
}

private void addQueryInfo(Message msg) {
    Set<Message> nodeMsg = querys.getOrDefault(msg.from, new HashSet<Message>());
    nodeMsg.add(msg);
    querys.put(msg.from, nodeMsg);
}

/*============================================================================*/
// inner class
/*============================================================================*/
class NodeQuery implements Comparable<NodeQuery> {
    public Node node;
    public int score;

    public NodeQuery(Node node, int score) {
        this.node = node;
        this.score = score;
    }

    @Override
    public int compareTo(NodeQuery that) {
        // descending
        return Integer.valueOf(that.score).compareTo(Integer.valueOf(this.score));
    }
}

}

package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

public class PlumtreeQueryProtocol implements CDProtocol, Deliverable, HitsConfigurable {
/*============================================================================*/
// parameters
/*============================================================================*/
private static final String PARAM_IHAVE_TIMEOUT= "ihavetimeout";
private static final String PARAM_GRAFT_TIMEOUT = "grafttimeout";

/*============================================================================*/
// fields
/*============================================================================*/
private int ihaveTimeout;
private int graftTimeout;

private Deque<Message> mailbox; // stores unchecked messages
private Map<Integer, Message> seen; // stores seen gossips
private Map<Integer, LinkedList<Node>> missing; // stores received ihave messages

private MessageIDTimer ihaveTimer;
private MessageIDTimer graftTimer;

private Map<Integer, Set<Node>> children;
private Map<Integer, Node> fathers;
private Map<Integer, Message> responses;

private boolean isHit;

/*============================================================================*/
// constructor
/*============================================================================*/
public PlumtreeQueryProtocol(String prefix) {
    ihaveTimeout = Configuration.getInt(prefix + "." + PARAM_IHAVE_TIMEOUT);
    graftTimeout = Configuration.getInt(prefix + "." + PARAM_GRAFT_TIMEOUT);
    mailbox = new LinkedList<>();
    seen = new HashMap<>();
    missing = new HashMap<>();

    ihaveTimer = new MessageIDTimer(ihaveTimeout);
    graftTimer = new MessageIDTimer(graftTimeout);

    children = new HashMap<>();
    fathers = new HashMap<>();
    responses = new HashMap<>();

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

    // in each cycle we do:
    // 1. open mailbox and check any unhandle messages;
    // 2. if receive a seen message, drop it;
    // 3. if receive an unseen message, resend it to serveral neighbors.

    if (!node.isUp()) {
        // don't process message if node is down
        return;
    }

    // get linkable
    int linkableID = FastConfig.getLinkable(protocolID);
    EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(linkableID);

    for(Message msg : mailbox) {

        Node from = msg.from;
        PlumtreeMessage incoming = (PlumtreeMessage) msg;

        switch (incoming.type) {
            case Request:
                // handle Gossip
                if (incoming.isGossip) {
                    QueryObserver.handleRecvRequest(incoming, incoming.from, node);
                    if (isHit) {
                        QueryObserver.handleHit(incoming, node);
                    }
                    handleGossip(linkable, protocolID, from, node, incoming);
                }
                break;
            case Response:
                QueryObserver.handleRecvResponse(incoming, incoming.from, node);
                handleResponse(linkable, protocolID, from, node, incoming);
                break;
            case Control:
                QueryObserver.handleRecvControl(incoming, incoming.from, node);
                // handle IHave
                if (incoming.isIHave) {
                    handleIHave(linkable, protocolID, from, node, incoming);
                }
                // handle Graft
                if (incoming.isGraft) {
                    handleGraft(linkable, protocolID, from, node, incoming);
                }
                // handle Prune
                if (incoming.isPrune) {
                    handlePrune(linkable, protocolID, from, node, incoming);
                }
                break;
        }

    }
    mailbox.clear();

    Set<Integer> ihaveTimeoutIDs = ihaveTimer.nextCycle();
    Set<Integer> graftTimeoutIDs = graftTimer.nextCycle();

    for(int msgID : ihaveTimeoutIDs) {
        handleTimeout(linkable, protocolID, node, msgID);
    }
    for(int msgID : graftTimeoutIDs) {
        handleTimeout(linkable, protocolID, node, msgID);
    }

}

@Override
public Object clone() {
    PlumtreeQueryProtocol that = null;
    try {
        that = (PlumtreeQueryProtocol) super.clone();
        that.mailbox = new LinkedList<>(this.mailbox);
        that.seen = new HashMap<>(this.seen);
        
        that.ihaveTimer = (MessageIDTimer) this.ihaveTimer.clone();
        that.graftTimer = (MessageIDTimer) this.graftTimer.clone();

        that.missing = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<Node>> entry : this.missing.entrySet()) {
            LinkedList<Node> thatlist = new LinkedList<>();
            for(Node n : entry.getValue()) {
                thatlist.add(n);
            }
            that.missing.put(entry.getKey(), thatlist);
        }
        
        that.children = new HashMap<>();
        for (Map.Entry<Integer, Set<Node>> entry : this.children.entrySet()) {
            Set<Node> thatset = new HashSet<>();
            for (Node n : entry.getValue()) {
                thatset.add(n);
            }
            that.children.put(entry.getKey(), thatset);
        }

        that.fathers = new HashMap<>(this.fathers);

        that.responses = new HashMap<>(this.responses);

    } catch(CloneNotSupportedException e){
        e.printStackTrace();
    }
    return that;
}

/*============================================================================*/
// helpers
/*============================================================================*/
private void handleGossip(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    Node node,
    PlumtreeMessage incoming
) {

    if (seen.containsKey(incoming.id)) {
        linkable.prune(from);
        
        // send prune
        PlumtreeMessage outgoing = (PlumtreeMessage) seen.get(incoming.id).hopFrom(node);
        outgoing.SetPrune();
        
        PlumtreeQueryProtocol pfrom = (PlumtreeQueryProtocol) from.getProtocol(protocolID);
        pfrom.deliver(outgoing);

        return;
    }
    seen.put(incoming.id, incoming);

    // cancel timer
    ihaveTimer.cancel(incoming.id);
    graftTimer.cancel(incoming.id);

    // eager push
    eagerPush(linkable, protocolID, from, node, incoming);
    // lazy push
    lazyPush(linkable, protocolID, from, node, incoming);
    
    // don't graft myself
    if (from != node) {
        // graft
        linkable.graft(from);
    }
}
private void handleIHave(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    Node node,
    PlumtreeMessage incoming
) {

    if (seen.containsKey(incoming.id)) {
        return;
    }
    // set up timer
    ihaveTimer.add(incoming.id);

    // add missing
    LinkedList<Node> ihaveList = missing.getOrDefault(incoming.id, new LinkedList<>());
    ihaveList.add(from);
    missing.put(incoming.id, ihaveList);

}
private void handleGraft(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    Node node,
    PlumtreeMessage incoming
) {
    linkable.graft(from);
    if (seen.containsKey(incoming.id)) {
        // send gossip
        PlumtreeMessage gossipMsg = (PlumtreeMessage) seen.get(incoming.id).hopFrom(node);
        
        PlumtreeQueryProtocol pfrom = (PlumtreeQueryProtocol) from.getProtocol(protocolID);
        pfrom.deliver(gossipMsg);

    }
}
private void handlePrune(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    Node node,
    PlumtreeMessage incoming
) {
    linkable.prune(from);
}

private void handleTimeout(
    EagerLazyLink linkable,
    int protocolID,
    Node node,
    int msgID
) {

    // setup timer
    graftTimer.add(msgID);

    // pop missing
    Node first = missing.get(msgID).pollFirst();

    // graft
    linkable.graft(first);

    // send graft message and notify
    PlumtreeMessage outgoing = new PlumtreeMessage();
    outgoing.from = node;
    outgoing.id = msgID;
    outgoing.SetGraft();

    PlumtreeQueryProtocol pfirst = (PlumtreeQueryProtocol) first.getProtocol(protocolID);
    pfirst.deliver(outgoing);

}

private void eagerPush(
    EagerLazyLink linkable,
    int protocolID, 
    Node from,
    Node node,
    PlumtreeMessage incoming
)  {
    PlumtreeMessage eagerMsg = (PlumtreeMessage) incoming.hopFrom(node);

    Set<Node> curChildren = new HashSet<>();

    for(Node eager: linkable.getEagerPeers()) {
        // don't send back to sender
        if (eager == from) {
            continue;
        }

        PlumtreeQueryProtocol p = (PlumtreeQueryProtocol) eager.getProtocol(protocolID);
        p.deliver(eagerMsg);

        curChildren.add(eager);
    }

    // response handle
    PlumtreeMessage resp = (PlumtreeMessage) incoming.clone();
    resp.from = node;
    resp.SetResponse();
    resp.collectedHits = isHit ? 1 : 0;

    // set children and father
    children.put(incoming.id, curChildren);
    fathers.put(incoming.id, from);

    // for leaf node, trigger send back
    if ( curChildren.size() == 0 ) {
        PlumtreeQueryProtocol pfrom = (PlumtreeQueryProtocol) from.getProtocol(protocolID);
        pfrom.deliver(resp);
        return;
    }

    // // add local response
    responses.put(incoming.id, resp);

}

private void lazyPush(
    EagerLazyLink linkable,
    int protocolID, 
    Node from,
    Node node,
    PlumtreeMessage incoming
) {
    PlumtreeMessage lazyMsg = (PlumtreeMessage) incoming.hopFrom(node);
    lazyMsg.SetIhave();

    for(Node lazy: linkable.getLazyPeers()) {
        // don't send back to sender
        if (lazy == from) {
            continue;
        }

        // for now we use the origin message id.
        PlumtreeQueryProtocol p = (PlumtreeQueryProtocol) lazy.getProtocol(protocolID);
        p.deliver(lazyMsg);
    }
}

private void handleResponse(
    EagerLazyLink linkable,
    int protocolID, 
    Node from,
    Node node,
    PlumtreeMessage incoming
) {
    Set<Node> expectedChildren = children.get(incoming.id);
    Node father = fathers.get(incoming.id);
    Message resp = responses.get(incoming.id);

    // update children and resp
    expectedChildren.remove(from);
    resp.hop = Math.max(resp.hop, incoming.hop);
    resp.collectedHits += incoming.collectedHits;

    // root handle
    if (incoming.root == node) {
        if (expectedChildren.size() == 0) {
            // notify observer
            QueryObserver.handleQuerySuccess(resp, node);
        }
        return;
    }
    
    // send back if expectedChildren is empty
    if (expectedChildren.size() == 0) {
        PlumtreeMessage outgoing = (PlumtreeMessage) resp.hopFrom(node);
        PlumtreeQueryProtocol pfather = (PlumtreeQueryProtocol) father.getProtocol(protocolID);
        pfather.deliver(outgoing);
    }

}

@Override
public void SetHit() {
    isHit = true;
}

@Override
public boolean IsHit() {
    return isHit;
}

}
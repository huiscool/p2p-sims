package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.Network;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

public class PlumtreeProtocol implements CDProtocol, Deliverable{
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
private Set<Message> seen; // stores seen gossips
private Map<Integer, LinkedList<Node>> missing; // stores received ihave messages

private MessageIDTimer ihaveTimer;
private MessageIDTimer graftTimer;

/*============================================================================*/
// constructor
/*============================================================================*/
public PlumtreeProtocol(String prefix) {
    ihaveTimeout = Configuration.getInt(prefix + "." + PARAM_IHAVE_TIMEOUT);
    graftTimeout = Configuration.getInt(prefix + "." + PARAM_GRAFT_TIMEOUT);
    mailbox = new LinkedList<>();
    seen = new HashSet<>();
    missing = new HashMap<>();

    ihaveTimer = new MessageIDTimer(ihaveTimeout);
    graftTimer = new MessageIDTimer(graftTimeout);
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
    EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(linkableID);

    if (!node.isUp()) {
        // don't process message if node is down
        return;
    }

    for(Message msg : mailbox) {

        Node from = Network.get(msg.fromNodeIndex);
        PlumtreeMessage incoming = (PlumtreeMessage) msg;
        PlumtreeMessage outgoing = (PlumtreeMessage) incoming.clone();
        outgoing.fromNodeIndex = node.getIndex();
        outgoing.hop += 1;

        // notify observer
        PlumtreeObserver.handleRecvMsg(protocolID, from, node, msg);

        // handle Gossip
        if (incoming.gossip != null) {
            handleGossip(linkable, protocolID, from, incoming, outgoing);
        }
        // handle IHave
        if (incoming.iHave != null ) {
            handleIHave(linkable, protocolID, from, incoming, outgoing);
        }
        // handle Graft
        if (incoming.graft != null) {
            handleGraft(linkable, protocolID, from, incoming, outgoing);
        }
        // handle Prune
        if (incoming.prune != null) {
            handlePrune(linkable, protocolID, from, incoming, outgoing);
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
    PlumtreeProtocol that = null;
    try {
        that = (PlumtreeProtocol) super.clone();
        that.mailbox = new LinkedList<>(this.mailbox);
        that.seen = new HashSet<>(this.seen);
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
    PlumtreeMessage incoming,
    PlumtreeMessage outgoing
) {
    if (seen.contains(incoming)) {
        linkable.prune(from);
        
        // send prune
        outgoing.prune = outgoing.new Prune();
        PlumtreeProtocol pfrom = (PlumtreeProtocol) from.getProtocol(protocolID);
        pfrom.deliver(outgoing);

        // notify
        Node node = Network.get(outgoing.fromNodeIndex);
        PlumtreeObserver.handleSendMsg(protocolID, node, from, outgoing);
        return;
    }
    seen.add(incoming);

    // cancel timer
    ihaveTimer.cancel(incoming.id);
    graftTimer.cancel(incoming.id);

    // eager push
    eagerPush(linkable, protocolID, outgoing);
    // lazy push
    lazyPush(linkable, protocolID, outgoing);
    // graft
    linkable.graft(from);
    
}
private void handleIHave(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    PlumtreeMessage incoming,
    PlumtreeMessage outgoing
) {
    if (seen.contains(incoming)) {
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
    PlumtreeMessage incoming,
    PlumtreeMessage outgoing
) {
    linkable.graft(from);
    if (seen.contains(incoming)) {
        // send gossip
        PlumtreeMessage gossipMsg = (PlumtreeMessage) outgoing.clone();
        gossipMsg.gossip = gossipMsg.new Gossip();

        PlumtreeProtocol p = (PlumtreeProtocol) from.getProtocol(protocolID);
        p.deliver(gossipMsg);

        // notify observer
        // node is the sender and from is the receiver.
        Node node = Network.get(outgoing.fromNodeIndex);
        PlumtreeObserver.handleSendMsg(protocolID, node, from, outgoing);
    }
}
private void handlePrune(
    EagerLazyLink linkable,
    int protocolID,
    Node from,
    PlumtreeMessage incoming,
    PlumtreeMessage outgoing
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
    outgoing.graft = outgoing.new Graft();
    outgoing.id = msgID;

    PlumtreeProtocol pfirst = (PlumtreeProtocol) first.getProtocol(protocolID);
    pfirst.deliver(outgoing);

    PlumtreeObserver.handleSendMsg(protocolID, node, first, outgoing);

}

private void eagerPush(EagerLazyLink linkable,int protocolID, PlumtreeMessage outgoing) {
    PlumtreeMessage eagerMsg = (PlumtreeMessage) outgoing.clone();
    for(Node eager: linkable.getEagerPeers()) {

        eagerMsg.gossip = eagerMsg.new Gossip();

        PlumtreeProtocol p = (PlumtreeProtocol) eager.getProtocol(protocolID);
        p.deliver(eagerMsg);

        // notify observer
        // node is the sender and eager is the receiver.
        Node node = Network.get(eagerMsg.fromNodeIndex);
        PlumtreeObserver.handleSendMsg(protocolID, node, eager, eagerMsg);
    }
}

private void lazyPush(EagerLazyLink linkable,int protocolID, PlumtreeMessage outgoing) {
    PlumtreeMessage lazyMsg = (PlumtreeMessage) outgoing.clone();
    for(Node lazy: linkable.getLazyPeers()) {

        lazyMsg.iHave = lazyMsg.new IHave();
        // for now we use the origin message id.

        PlumtreeProtocol p = (PlumtreeProtocol) lazy.getProtocol(protocolID);
        p.deliver(lazyMsg);

        // notify observer
        // node is the sender and lazy is the receiver.
        Node node = Network.get(lazyMsg.fromNodeIndex);
        PlumtreeObserver.handleSendMsg(protocolID, node, lazy, lazyMsg);
    }
}

}

/*============================================================================*/
/**
 * MessageIDTimer is the simulant timer to know whether a message is timeout.
 */
class MessageIDTimer {
    private LinkedList<Set<Integer>> messageIDs;
    
    public MessageIDTimer(int cycleCap) {
       messageIDs = new LinkedList<>(); 
       for (int i=0; i<cycleCap+1; i++) {
           messageIDs.add(new HashSet<>());
       }
    }

    public void cancel(Integer msgid) {
        for(Set<Integer> node : messageIDs) {
            node.remove(msgid);
        }
    }

    public void add(Integer msgid) {
        if (!contains(msgid)) {
            Set<Integer> head = messageIDs.peekFirst();
            head.add(msgid);
        }
    }

    public boolean contains(Integer msgid) {
        for(Set<Integer> node : messageIDs) {
            if (node.contains(msgid)) {
                return true;
            }
        }
        return false;
    }

    // re-evaluate the timer in next cycle
    // return the timeout msgids.
    public Set<Integer> nextCycle() {
        Set<Integer> last = messageIDs.pollLast();
        messageIDs.addFirst(new HashSet<Integer>());
        return last;
    }
}
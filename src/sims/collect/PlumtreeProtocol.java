package sims.collect;

import peersim.cdsim.CDProtocol;
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
        PlumtreeMessage outgoing = (PlumtreeMessage) msg.hopFrom(node);

        // notify observer
        PlumtreeObserver.handleRecvMsg(protocolID, from, node, msg);

        // handle Gossip
        if (incoming.isGossip) {
            handleGossip(linkable, protocolID, from, incoming, outgoing);
        }
        // handle IHave
        if (incoming.isIHave) {
            handleIHave(linkable, protocolID, from, incoming, outgoing);
        }
        // handle Graft
        if (incoming.isGraft) {
            handleGraft(linkable, protocolID, from, incoming, outgoing);
        }
        // handle Prune
        if (incoming.isPrune) {
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
        that.missing = new HashMap<>(this.missing);
        that.ihaveTimer = (MessageIDTimer) this.ihaveTimer.clone();
        that.graftTimer = (MessageIDTimer) this.graftTimer.clone();
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

    Node node = outgoing.from;

    if (seen.contains(incoming)) {
        linkable.prune(from);
        
        // send prune
        outgoing.isGossip = false;
        outgoing.isPrune = true;
        outgoing.isGraft = false;
        outgoing.isIHave = false;
        
        PlumtreeProtocol pfrom = (PlumtreeProtocol) from.getProtocol(protocolID);
        pfrom.deliver(outgoing);

        // notify
        PlumtreeObserver.handleSendMsg(protocolID, node, from, outgoing);
        return;
    }
    seen.add(incoming);

    // cancel timer
    ihaveTimer.cancel(incoming.id);
    graftTimer.cancel(incoming.id);

    // eager push
    eagerPush(linkable, protocolID, from, outgoing);
    // lazy push
    lazyPush(linkable, protocolID, from, outgoing);
    
    // don't graft myself
    if (from.getIndex() != node.getIndex()) {
        // graft
        linkable.graft(from);
    }
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
        gossipMsg.isGossip = true;
        gossipMsg.isPrune = false;
        gossipMsg.isGraft = false;
        gossipMsg.isIHave = false;

        PlumtreeProtocol pfrom = (PlumtreeProtocol) from.getProtocol(protocolID);
        pfrom.deliver(gossipMsg);

        // notify observer
        // node is the sender and from is the receiver.
        Node node = outgoing.from;
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
    outgoing.from = node;
    outgoing.id = msgID;
    outgoing.isGossip = false;
    outgoing.isGraft = true;
    outgoing.isPrune = false;
    outgoing.isIHave = false;

    PlumtreeProtocol pfirst = (PlumtreeProtocol) first.getProtocol(protocolID);
    pfirst.deliver(outgoing);

    PlumtreeObserver.handleSendMsg(protocolID, node, first, outgoing);

}

private void eagerPush(
    EagerLazyLink linkable,
    int protocolID, 
    Node from,
    PlumtreeMessage outgoing
)  {
    PlumtreeMessage eagerMsg = (PlumtreeMessage) outgoing.clone();
    eagerMsg.isGossip = true;
    eagerMsg.isGraft = false;
    eagerMsg.isPrune = false;
    eagerMsg.isIHave = false;
    for(Node eager: linkable.getEagerPeers()) {
        // don't send back to sender
        if (eager.getIndex() == from.getIndex()) {
            continue;
        }


        PlumtreeProtocol p = (PlumtreeProtocol) eager.getProtocol(protocolID);
        p.deliver(eagerMsg);

        // notify observer
        // node is the sender and eager is the receiver.
        Node node = eagerMsg.from;
        PlumtreeObserver.handleSendMsg(protocolID, node, eager, eagerMsg);
    }
}

private void lazyPush(
    EagerLazyLink linkable,
    int protocolID, 
    Node from,
    PlumtreeMessage outgoing
) {
    PlumtreeMessage lazyMsg = (PlumtreeMessage) outgoing.clone();
    lazyMsg.isGossip = false;
    lazyMsg.isGraft = false;
    lazyMsg.isPrune = false;
    lazyMsg.isIHave = true;

    for(Node lazy: linkable.getLazyPeers()) {
        // don't send back to sender
        if (lazy.getIndex() == from.getIndex()) {
            continue;
        }

        // for now we use the origin message id.
        PlumtreeProtocol p = (PlumtreeProtocol) lazy.getProtocol(protocolID);
        p.deliver(lazyMsg);

        // notify observer
        // node is the sender and lazy is the receiver.
        Node node = lazyMsg.from;
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

    @Override
    public String toString() {
        return messageIDs.toString();
    }

    @Override
    public Object clone() {
        MessageIDTimer that = new MessageIDTimer(-1);
        for (Set<Integer> ids : this.messageIDs) {
            that.messageIDs.add(new HashSet<>(ids));
        }
        return that;
    }
}
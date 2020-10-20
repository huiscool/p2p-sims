package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.IdleProtocol;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

// GossipProtocol: a simple implementation of gossip protocol
public class GossipProtocol extends IdleProtocol implements CDProtocol {

/*============================================================================*/
// parameters
/*============================================================================*/
private final String ParamFanout = "fanout";


/*============================================================================*/
// fields
/*============================================================================*/
private int fanout; 
private Set<Message> mailbox; // the messages in mailbox
private Set<Message> seen;

/*============================================================================*/
// constructor
/*============================================================================*/
public GossipProtocol(String prefix) {
    super(prefix);
    fanout = Configuration.getInt(prefix + "." + ParamFanout);
    mailbox = new HashSet<>();
    seen = new HashSet<>();
}


/*============================================================================*/
// methods
/*============================================================================*/

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
    for (Message msg : mailbox) {
        mailbox.remove(msg);
        if (seen.contains(msg)) {
            continue;
        }
        Set<Node> neighbors = Util.PickupNeighbors(fanout, linkable);
        for (Node neigh : neighbors) {
            sendMsg(node, neigh, msg);
        }
    }
}

@Override
public Object clone() {
    GossipProtocol gp = (GossipProtocol) super.clone();
    gp.fanout = this.fanout;
    gp.mailbox = this.mailbox;
    gp.seen = this.seen;
    return gp;
}

/*============================================================================*/
// helpers
/*============================================================================*/

private void sendMsg(Node from, Node to, Message msg) {
    senderHandleSendMsg(from, msg);
    receiverHandleSendMsg(to, msg);
    // notify the observer
    notifyObserverSendMsg(from, to, msg);
}

private void senderHandleSendMsg(Node from, Message msg) {
    // add message to seen
    seen.add(msg);
}

private void receiverHandleSendMsg(Node to, Message msg) {
    // deliver the message
    mailbox.add(msg);
}

private void notifyObserverSendMsg(Node from, Node to, Message msg) {
    
}

}

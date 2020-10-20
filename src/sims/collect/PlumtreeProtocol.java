package sims.collect;

import peersim.cdsim.CDProtocol;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.config.*;
import java.util.*;

public class PlumtreeProtocol implements CDProtocol, Deliverable{
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
public PlumtreeProtocol(String prefix) {
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
    for(Message msg : mailbox) {
        handleMsg(msg);
    }
    mailbox.clear();
}

@Override
public Object clone() {
    PlumtreeProtocol that = null;
    try {
        that = (PlumtreeProtocol) super.clone();
        that.fanout = this.fanout;
        that.mailbox = new LinkedList<>();
        that.seen = new HashSet<>();
    } catch(CloneNotSupportedException e){
    }
    return that;
}

/*============================================================================*/
// helpers
/*============================================================================*/

private void handleMsg(Message msg) {
}

}

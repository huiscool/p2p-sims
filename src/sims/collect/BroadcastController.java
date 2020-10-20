package sims.collect;

import peersim.config.Configuration;
import peersim.core.*;

public class BroadcastController implements Control {

/*============================================================================*/
// parameters
/*============================================================================*/

private static final String PARAM_PROTOCOL = "protocol";
private static final String PARAM_MSG_NUM = "msgnum";
private static final String PARAM_MSG_SIZE = "msgsize";
private static final String PARAM_PERIOD = "period";
private static final String PARAM_BEGIN_TIME = "begintime";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private int msgNum;
private int msgSize;
private int period;
private int beginTime;

/*============================================================================*/
// initializations
/*============================================================================*/

public BroadcastController(String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
    msgNum = Configuration.getInt(prefix + "." + PARAM_MSG_NUM);
    msgSize = Configuration.getInt(prefix + "." + PARAM_MSG_SIZE);
    period = Configuration.getInt(prefix + "." + PARAM_PERIOD);
    beginTime = Configuration.getInt(prefix + "." + PARAM_BEGIN_TIME);
}


/*============================================================================*/
// methods
/*============================================================================*/

    public boolean execute() {
        if ((CommonState.getTime() - beginTime) % period != 0) {
            return false;
        }
        // pickup some nodes and deliver messages to their mailbox
        int[] nodeindexs = Util.pickup(msgNum, Network.size());
        for(int i=0; i<nodeindexs.length; i++) {
            // generate msg
            Message msg = new Message(msgSize, i);

            // get nodes and related protocol
            Node node = Network.get(nodeindexs[i]);
            Deliverable d = (Deliverable) node.getProtocol(protocolID);

            // deliver message into their mailboxes
            d.deliver(msg);
        }
        return false;
    }
}

/**
 * Deliverable is the interface for message delivery.
 */
interface Deliverable {
    void deliver(Message msg);
}
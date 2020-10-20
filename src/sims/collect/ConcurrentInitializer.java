package sims.collect;

import peersim.config.Configuration;
import peersim.core.*;

public class ConcurrentInitializer implements Control {

/*============================================================================*/
// parameters
/*============================================================================*/

private static final String PARAM_PROTOCOL = "protocol";
private static final String PARAM_INIT_NUM = "initnum";
private static final String PARAM_MSG_SIZE = "msgsize";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private int initNum;
private int msgSize;

/*============================================================================*/
// initializations
/*============================================================================*/

public ConcurrentInitializer(String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
    initNum = Configuration.getInt(prefix + "." + PARAM_INIT_NUM);
    msgSize = Configuration.getInt(prefix + "." + PARAM_MSG_SIZE);
}


/*============================================================================*/
// methods
/*============================================================================*/

    public boolean execute() {
        // pickup some nodes and deliver messages to their mailbox

        int[] nodeindexs = Util.pickup(initNum, Network.size());
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
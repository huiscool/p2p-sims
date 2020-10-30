package sims.collect;

import peersim.config.Configuration;
import peersim.core.*;
import java.util.Arrays;

public class BroadcastController implements Control {

/*============================================================================*/
// parameters
/*============================================================================*/

private static final String PARAM_PROTOCOL = "protocol";
private static final String PARAM_MSG_TYPE = "msgtype";
private static final String PARAM_MSG_NUM = "msgnum";
private static final String PARAM_MSG_SIZE = "msgsize";
private static final String PARAM_PERIOD = "period";
private static final String PARAM_BEGIN_TIME = "begintime";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private String msgType;
private int msgNum;
private int msgSize;
private int period;
private int beginTime;
private int counter;

/*============================================================================*/
// initializations
/*============================================================================*/

public BroadcastController(String prefix) {
    this.protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
    this.msgType = Configuration.getString(prefix + "." + PARAM_MSG_TYPE);
    this.msgNum = Configuration.getInt(prefix + "." + PARAM_MSG_NUM);
    this.msgSize = Configuration.getInt(prefix + "." + PARAM_MSG_SIZE);
    this.period = Configuration.getInt(prefix + "." + PARAM_PERIOD);
    this.beginTime = Configuration.getInt(prefix + "." + PARAM_BEGIN_TIME);
    this.counter = 0;
}


/*============================================================================*/
// methods
/*============================================================================*/

    public boolean execute() {
        if ((CommonState.getTime() - this.beginTime) % this.period != 0) {
            return false;
        }
        // pickup some nodes and deliver messages to their mailbox
        int[] nodeindexs = Util.pickup(this.msgNum, Network.size());
        for(int i=0; i<nodeindexs.length; i++) {

            Node node = Network.get(nodeindexs[i]);

            // generate msg
            Message msg = Message.New(msgType);
            msg.size = this.msgSize;
            msg.id = (counter++);
            msg.hop = 0;
            msg.from = node;
            msg.root = node;

            // notify observer
            QueryObserver.handleNewRequest(msg, node);

            // deliver message into their mailboxes
            Deliverable d = (Deliverable) node.getProtocol(this.protocolID);
            d.deliver(msg);
        }
        System.out.println("send to:" + Arrays.toString(nodeindexs));
        return false;
    }
}

/**
 * Deliverable is the interface for message delivery.
 */
interface Deliverable {
    void deliver(Message msg);
}
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
private static final String PARAM_STRATEGY = "strategy";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private String msgType;
private int msgNum;
private int msgSize;
private int period;
private int beginTime;
private String strategy;

private int counter;
private int[] fixedNodeIndices;

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
    this.strategy = Configuration.getString(prefix + "." + PARAM_STRATEGY);
    this.counter = 0;
    this.fixedNodeIndices = Util.pickup(msgNum, Network.size());
}


/*============================================================================*/
// methods
/*============================================================================*/

public boolean execute() {
    switch (strategy) {
        case "fix-period":
            return fixPeriod();
        case "random-period":
        default:
            return randomPeriod();
    }
}

private boolean randomPeriod() {
    if ((CommonState.getTime() - this.beginTime) % this.period != 0) {
        return false;
    }
    // pickup some nodes and deliver messages to their mailbox
    int[] nodeIndices = Util.pickup(this.msgNum, Network.size());
    for(int i=0; i<nodeIndices.length; i++) {

        Node node = Network.get(nodeIndices[i]);

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
    System.out.println("send to:" + Arrays.toString(nodeIndices));
    return false;
}

private boolean fixPeriod() {
    if ((CommonState.getTime() - this.beginTime) % this.period != 0) {
        return false;
    }
    // pickup some nodes and deliver messages to their mailbox
    for(int i=0; i<this.fixedNodeIndices.length; i++) {

        Node node = Network.get(fixedNodeIndices[i]);

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
    System.out.println("send to:" + Arrays.toString(fixedNodeIndices));
    return false;
}

}

/**
 * Deliverable is the interface for message delivery.
 */
interface Deliverable {
    void deliver(Message msg);
}
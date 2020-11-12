package sims.collect;

import java.util.*;

import peersim.cdsim.*;
import peersim.config.Configuration;
import peersim.core.*;

public class GossipCollectProtocol implements CDProtocol, HitsConfigurable, Deliverable, RequestHandler {

/*============================================================================*/
// parameters
/*============================================================================*/
private static String PARAM_ROUTER = "router";


/*============================================================================*/
// fields
/*============================================================================*/
private List<Message> mailbox;

private int routerID;

private boolean isHit;

/*============================================================================*/
// constructor
/*============================================================================*/
public GossipCollectProtocol(String prefix) {
    mailbox = new LinkedList<>();
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

    CollectRoutable router = (CollectRoutable) node.getProtocol(routerID);

    for(Message msg : mailbox) {
        switch (msg.type) {
            case Response:
                QueryObserver.handleRecvResponse(msg, msg.from, node);
                handleResponse(router, protocolID, msg.from, node, msg);
                break;
            case Control:
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
        GossipCollectProtocol pto = (GossipCollectProtocol) Util.GetNodeProtocol(to, GossipCollectProtocol.class);

        Message outgoing = (Message) msg.hopFrom(node);
        outgoing.type = MessageType.Response;
        outgoing.collectedHits = 1;
        pto.deliver(outgoing);
        // System.out.printf("hit: %d->%d%n", node.getIndex(), to.getIndex());
    }
}

@Override
public void SetHit() {
    isHit = true;
}

@Override
public Object clone() {
    try {
        GossipCollectProtocol that = (GossipCollectProtocol) super.clone();
        that.isHit = this.isHit;
        that.mailbox = new LinkedList<>(this.mailbox);
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

    Node to = router.getFather(msg);
    GossipCollectProtocol pto = (GossipCollectProtocol) to.getProtocol(protocolID);

    Message outgoing = msg.hopFrom(node);
    pto.deliver(outgoing);
}

}

/**
 * CollectRoutable is the interface to generate a broadcast tree
 */
interface CollectRoutable {
    Node getFather(Message msg);
}

interface RequestHandler {
    void handleRequest(Node node, Message msg);
}

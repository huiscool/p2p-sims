package sims.collect;

import java.util.HashSet;
import java.util.Set;

import peersim.cdsim.*;
import peersim.config.Configuration;
import peersim.core.*;

public class CollectProtocol implements CDProtocol, HitsConfigurable, Deliverable, RequestHandler {

/*============================================================================*/
// parameters
/*============================================================================*/
private static String PARAM_ROUTER = "router";


/*============================================================================*/
// fields
/*============================================================================*/
private Set<Message> mailbox;

private int routerID;

private boolean isHit;

/*============================================================================*/
// constructor
/*============================================================================*/
public CollectProtocol(String prefix) {
    mailbox = new HashSet<>();
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
    router.SetRequestHandler(this);

    for(Message msg : mailbox) {
        switch (msg.type) {
            case Response:
                QueryObserver.handleRecvResponse(msg, msg.from, node);
                handleResponse(router, protocolID, msg.from, node, msg);
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
        Node to = router.GetFather(msg);
        CollectProtocol pto = (CollectProtocol) Util.GetNodeProtocol(to, CollectProtocol.class);

        Message outgoing = (Message) msg.clone();
        outgoing.type = MessageType.Response;
        outgoing.collectedHits = 1;
        pto.deliver(outgoing);
    }
}

@Override
public void SetHit() {
    isHit = true;
}

@Override
public Object clone() {
    try {
        CollectProtocol that = (CollectProtocol) super.clone();
        that.isHit = this.isHit;
        that.mailbox = new HashSet<>(this.mailbox);
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

    Node to = router.GetFather(msg);
    CollectProtocol pto = (CollectProtocol) to.getProtocol(protocolID);

    Message outgoing = msg.hopFrom(node);
    pto.deliver(outgoing);
}

}

/**
 * CollectRoutable is the interface to generate a broadcast tree
 */
interface CollectRoutable {
    Node GetFather(Message msg);
    void SetRequestHandler(RequestHandler handler);
}

interface RequestHandler {
    void handleRequest(Node node, Message msg);
}

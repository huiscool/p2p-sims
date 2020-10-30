package sims.collect;

import java.util.*;

import peersim.core.*;
import peersim.util.IncrementalStats;

public class QueryObserver {
/*============================================================================*/
// fields
/*============================================================================*/
private static QueryObserverInstance instance;
/*============================================================================*/
// constructor
/*============================================================================*/
public QueryObserver(String prefix) {
    getInstance(); // it is not thread safe, but ok here
}

/*============================================================================*/
// methods
/*============================================================================*/
public static void handleNewRequest(Message msg, Node node) {
    instance.handleNewRequest(msg, node);
}
public static void handleQueryHit(Message msg, Node node) {
    instance.handleQueryHit(msg, node);
}
public static void handleSendRequest(Message msg, Node from, Node to) {
    instance.handleSendRequest(msg, from, to);
}
public static void handleRecvRequest(Message msg, Node from, Node to) {
    instance.handleRecvRequest(msg, from, to);
}
public static void handleSendResponse(Message msg, Node from, Node to) {
    instance.handleSendResponse(msg, from, to);
}
public static void handleRecvResponse(Message msg, Node from, Node to) {
    instance.handleRecvResponse(msg, from, to);
}
public QueryObserverInstance getInstance() {
    if (instance == null) {
        instance = new QueryObserverInstance();
    }
    return instance;
}


}

class QueryObserverInstance {
/*============================================================================*/
// fields
/*============================================================================*/
private HashMap<Integer, QueryStat> queryStats = new HashMap<>();

/*============================================================================*/
// methods
/*============================================================================*/
public void handleNewRequest(Message msg, Node node) {
    queryStats.put(msg.id, new QueryStat(node));
}
public void handleQueryHit(Message msg, Node node) {
    QueryStat qs = queryStats.get(msg.id);
    qs.hitStats.add(1);
}
public void handleSendRequest(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Request);
    QueryStat qs = queryStats.get(msg.id);
    qs.sendRequestStats.add(1);
}
public void handleRecvRequest(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Request);
    QueryStat qs = queryStats.get(msg.id);
    qs.recvRequestStats.add(1);
    qs.requestHopStats.add(msg.hop);
    qs.covered.add(to);
}
public void handleSendResponse(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Response);
    QueryStat qs = queryStats.get(msg.id);
    qs.sendResponseStats.add(1);
}
public void handleRecvResponse(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Response);
    QueryStat qs = queryStats.get(msg.id);
    qs.recvResponseStats.add(1);
    if (to == qs.root) {
        qs.finalResponseStats.add(1);
        qs.responseHopStats.add(msg.hop);
    }

}

}

class QueryStat {
public Node root;
public IncrementalStats sendRequestStats;
public IncrementalStats recvRequestStats;
public IncrementalStats sendResponseStats;
public IncrementalStats recvResponseStats;
public IncrementalStats finalResponseStats;
public IncrementalStats requestHopStats;
public IncrementalStats responseHopStats;
public IncrementalStats hitStats;
public HashSet<Node> covered;
public Boolean success;

public QueryStat(Node n) {
    root = n;
    sendRequestStats = new IncrementalStats();
    recvRequestStats = new IncrementalStats();
    sendResponseStats = new IncrementalStats();
    recvResponseStats = new IncrementalStats();
    finalResponseStats = new IncrementalStats();
    requestHopStats = new IncrementalStats();
    responseHopStats = new IncrementalStats();
    hitStats = new IncrementalStats();
    covered = new HashSet<>();
    success = false;
}

}

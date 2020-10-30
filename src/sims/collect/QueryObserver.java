package sims.collect;

import java.util.*;

import peersim.core.*;
import peersim.util.IncrementalStats;

public class QueryObserver implements Control {
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
    getInstance().handleNewRequest(msg, node);
}

public static void handleRecvRequest(Message msg, Node from, Node to) {
    getInstance().handleRecvRequest(msg, from, to);
}

public static void handleRecvResponse(Message msg, Node from, Node to) {
    getInstance().handleRecvResponse(msg, from, to);
}

public static void handleRecvControl(Message msg, Node from, Node to) {
    getInstance().handleRecvControl(msg, from, to);
}

public static void handleHit(Message msg, Node node) {
    getInstance().handleHit(msg, node);
}

public static QueryObserverInstance getInstance() {
    if (instance == null) {
        instance = new QueryObserverInstance();
    }
    return instance;
}

public boolean execute() {
    for(Map.Entry<Integer, QueryStat> entry: getInstance().queryStats.entrySet()) {
        System.out.println("msgid="+entry.getKey()+","+entry.getValue());
        System.out.println();
    }
    return false;
}

}

class QueryObserverInstance {
/*============================================================================*/
// fields
/*============================================================================*/
public HashMap<Integer, QueryStat> queryStats = new HashMap<>();

/*============================================================================*/
// methods
/*============================================================================*/
public void handleNewRequest(Message msg, Node node) {
    QueryStat qs = new QueryStat();
    qs.root = node;
    qs.sendTime = CommonState.getIntTime();
    queryStats.put(msg.id, qs);
}

public void handleRecvRequest(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Request);
    QueryStat qs = queryStats.get(msg.id);
    qs.totalRecvRequest++;

    qs.covered.add(to);

    qs.requestHops.add(msg.hop);
}

public void handleRecvResponse(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Response);
    QueryStat qs = queryStats.get(msg.id);
    qs.totalRecvResponse++;

    // handleFinalResponse
    if (to == qs.root) {
        qs.finalResponseHops.add(msg.hop);

        int time = CommonState.getIntTime();
        for (int i=0; i<msg.collectedHits; i++){
            qs.arriveTimes.add(time);
        }

    }
}

public void handleRecvControl(Message msg, Node from, Node to) {
    assert(msg.type == MessageType.Control);
    QueryStat qs = queryStats.get(msg.id);
    qs.totalRecvControl++;
}

public void handleHit(Message msg, Node node) {
    QueryStat qs = queryStats.get(msg.id);
    qs.hits++;
}

}

class QueryStat {
public int sendTime;
public Node root;

public HashSet<Node> covered; // how many node receive requests
public int hits; // how many node are hit;
public int totalRecvRequest; // how many received requests around the network
public int totalRecvResponse; // how many received responses around the network
public int totalRecvControl; // how many received controls around the network
public IncrementalStats requestHops; // request message hop statistics;
public IncrementalStats finalResponseHops; // final response message hop statistics;
public ArrayList<Integer> arriveTimes; // record the time when the i-th result arrived at the root node. The size is how many results that the root node received.

public QueryStat() {
    covered = new HashSet<>();
    requestHops = new IncrementalStats();
    finalResponseHops = new IncrementalStats();
    arriveTimes = new ArrayList<>();
}

public String toString() {
    return String.format(
        "root=%d, startTime=%d%n",
        root.getIndex(),
        sendTime
    ) + String.format(
        "req=%d, resp=%d, ctrl=%d%n", 
        totalRecvRequest,
        totalRecvResponse,
        totalRecvControl
    ) + String.format(
        "reqHopMax=%f, reqHopAvg=%f, respHopMax=%f, respHopAvg=%f%n",
        requestHops.getMax(),
        requestHops.getAverage(),
        finalResponseHops.getMax(),
        finalResponseHops.getAverage()
    ) + String.format(
        "covered=%d, hits=%d%n", 
        covered.size(),
        hits
    ) + "arrivals=" + arriveTimes.toString();
}

public boolean success(int hitNeeded) {
    return hitNeeded <= arriveTimes.size();
}

public double hopNumber() {
    return finalResponseHops.getAverage();
}

public double MessagePerNode() {
    return (double)(totalRecvRequest + totalRecvResponse + totalRecvControl)/3.0;
}

public double QueryHits() {
    return (double) hits;
}

}

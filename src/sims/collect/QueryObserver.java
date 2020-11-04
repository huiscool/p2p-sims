package sims.collect;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.annotation.*;
import com.alibaba.fastjson.serializer.SerializerFeature;

import peersim.config.Configuration;
import peersim.core.*;
import peersim.util.IncrementalStats;

public class QueryObserver implements Control {
/*============================================================================*/
// parameters
/*============================================================================*/
private static String PARAM_LOG_PATH = "logpath";

/*============================================================================*/
// fields
/*============================================================================*/
private static QueryObserverInstance instance;
private String logPath;
private File logFile;

/*============================================================================*/
// constructor
/*============================================================================*/
public QueryObserver(String prefix) {
    logPath = Configuration.getString(prefix + "." + PARAM_LOG_PATH, "./query.log");

    logFile = new File(logPath);
    if (! logFile.exists() ) {
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // truncate
    try {
        FileWriter writer = new FileWriter(logFile);
        writer.write("");
        writer.flush();
        writer.close();
    } catch (IOException e) {
        e.printStackTrace();
    }

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

public static void handleQuerySuccess(Message msg, Node node) {
    getInstance().handleQuerySuccess(msg, node);
}

public static QueryObserverInstance getInstance() {
    if (instance == null) {
        instance = new QueryObserverInstance();
    }
    return instance;
}

public boolean execute() {
    String json = JSON.toJSONString(getInstance());
    System.out.println(json);
    try {
        FileWriter w = new FileWriter(logFile, true);
        w.write(json);
        w.write(System.getProperty("line.separator"));
        w.flush();
        w.close();
    }catch(IOException e) {
        e.printStackTrace();
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
// json
/*============================================================================*/

@JSONField(name = "queryStats")
public JSONArray getQueryStats() {
    JSONArray out = new JSONArray();
    for (Map.Entry<Integer, QueryStat> entry : queryStats.entrySet()) {
        JSONObject o = new JSONObject();
        o.put("msgID", entry.getKey());
        o.put("stat", entry.getValue());
        out.add(o);
    }
    return out;
}

@JSONField(name = "time") 
public int getTime() {
    return CommonState.getIntTime();
}

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

    if (msg.root == to) {
        qs.finalResponseHops.add(msg.hop);
        int t = CommonState.getIntTime();
        for (int i=0; i<msg.collectedHits; i++) {
            qs.arriveTimes.add(t);
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
    qs.hits.add(node);
}

public void handleQuerySuccess(Message msg, Node node) {
}

}

class QueryStat {
public int sendTime;
public Node root;
public HashSet<Node> covered; // how many node receive requests
public HashSet<Node> hits; // how many node are hit
public int totalRecvRequest; // how many received requests around the network
public int totalRecvResponse; // how many received responses around the network
public int totalRecvControl; // how many received controls around the network
public ArrayList<Integer> arriveTimes; // record the time when the i-th result arrived at the root node. The size is how many results that the root node received.

@JSONField(serialize = false)
public IncrementalStats requestHops; // request message hop statistics;
@JSONField(serialize = false)
public IncrementalStats finalResponseHops; // final response message hop statistics;

/*============================================================================*/
// JSON getter
/*============================================================================*/

@JSONField(name = "root")
public int getRoot() {
    return root.getIndex();
}

@JSONField(name = "covered")
public int getCovered() {
    return covered.size();
}

@JSONField(name = "hits")
public int getHits() {
    return hits.size();
}

@JSONField(name = "requestHopsMax")
public double getReqMaxHops() {
    return requestHops.getMax();
}

@JSONField(name = "requestHopsAvg")
public double getReqAvgHops() {
    return requestHops.getAverage();
}

@JSONField(name = "responseHopsMax")
public double getRespMaxHops() {
    return finalResponseHops.getMax();
}

@JSONField(name = "responseHopsAvg")
public double getRespAvgHops() {
    return finalResponseHops.getAverage();
}

/*============================================================================*/
// methods
/*============================================================================*/

public QueryStat() {
    covered = new HashSet<>();
    hits = new HashSet<>();
    requestHops = new IncrementalStats();
    finalResponseHops = new IncrementalStats();
    arriveTimes = new ArrayList<>();
}

public String toString() {
    return JSON.toJSONString(this);
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
    return (double) hits.size();
}

}

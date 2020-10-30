package sims.collect;

import peersim.config.*;
import peersim.core.*;

/**
 * QueryHitsInitializer creates several answer nodes all over the network.
 */
public class QueryHitsInitializer {

/*============================================================================*/
// parameters
/*============================================================================*/
private static final String PARAM_PROTOCOL = "protocol";
private static final String PARAM_TOTAL = "total";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private int total;

/*============================================================================*/
// constructor
/*============================================================================*/
public QueryHitsInitializer(String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
    total = Configuration.getInt(prefix + "." + PARAM_TOTAL);
}

/*============================================================================*/
// methods
/*============================================================================*/
public boolean execute() {
    int[] nodeIndexs = Util.pickup(total, Network.size());
    for (int i=0; i<nodeIndexs.length; i++) {
        Node n = Network.get(nodeIndexs[i]);
        HitsConfigurable h = (HitsConfigurable) n.getProtocol(protocolID);
        h.SetHit();
    }
    return false;
}

}

/**
 * HitsConfigurable is the interface to set up whether a node can answer the query.
 * For simplicity, we assume that the set node can answer all the query no matter what it is.
 */
interface HitsConfigurable {
    void SetHit();
}
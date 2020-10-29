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
private static final String PARAM_TOTALHITS = "totalhits";

/*============================================================================*/
// fields
/*============================================================================*/
private int protocolID;
private int totalHits;

/*============================================================================*/
// constructor
/*============================================================================*/
public QueryHitsInitializer(String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);
    totalHits = Configuration.getInt(prefix + "." + PARAM_TOTALHITS);
}

/*============================================================================*/
// methods
/*============================================================================*/
public boolean execute() {
    int[] nodeIndexs = Util.pickup(totalHits, Network.size());
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
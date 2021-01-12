package sims.collect;

import peersim.config.Configuration;
import peersim.core.*;
import java.util.*;

/**
 * ChurnController induces a churn in the overlay network by making node failures.
 */
public class ChurnController implements Control {
/*============================================================================*/
// parameters
/*============================================================================*/

private static final String PARAM_SCHEDULE = "schedule"; // in which turn begin to fail
private static final String PARAM_PERCENTAGE = "percentage"; // failure percentage
private static final String PARAM_PROTOCOL = "protocol";

/*============================================================================*/
// fields
/*============================================================================*/

private ArrayList<Integer> schedule;
private ArrayList<Float> percentage;
private int scheduleIndex;
private int protocolID;

/*============================================================================*/
// initializer
/*============================================================================*/

public ChurnController(String prefix) {
    String scheduleStr = Configuration.getString(prefix + "." + PARAM_SCHEDULE);
    String percentageStr = Configuration.getString(prefix + "." + PARAM_PERCENTAGE);

    this.schedule = new ArrayList<>();
    for(String s : scheduleStr.split("\s*,\s*") ) {
        schedule.add(Integer.parseInt(s));
    }

    this.percentage = new ArrayList<>();
    for(String s : percentageStr.split("\s*,\s*") ) {
        percentage.add(Float.parseFloat(s));
    }

    this.protocolID = Configuration.getPid(prefix + "." + PARAM_PROTOCOL);

    scheduleIndex = 0;
}

/*============================================================================*/
// methods
/*============================================================================*/

@Override
public boolean execute() {
    if ( scheduleIndex >= schedule.size() || 
         CommonState.getTime() != this.schedule.get(scheduleIndex) ) {
        return false;
    }
    
    int n = Network.size();
    ArrayList<Integer> hits = new ArrayList<>();
    for(int i=0; i<n; i++) {
        Network.get(i).setFailState(Fallible.OK);
        HitsConfigurable h = (HitsConfigurable) Network.get(i).getProtocol(protocolID);
        if (h.IsHit()) {
            hits.add(i);
        }
    }
    int[] arr = new int[hits.size()];
    for (int i = 0; i < arr.length; i++){
        arr[i] = hits.get (i);
    }

    int k = (int)Math.round(percentage.get(scheduleIndex)*n/100.0);

    int[] nodeindexs = Util.pickupWithout(k, n, arr);

    for(int i=0; i<nodeindexs.length; i++) {
        Network.get(nodeindexs[i]).setFailState(Fallible.DOWN);
    }

    System.out.println("induced churn in network: "+k+" node are down");

    scheduleIndex++;
    return false;
}

}

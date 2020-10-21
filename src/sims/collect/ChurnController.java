package sims.collect;

import peersim.config.Configuration;
import peersim.core.*;

/**
 * ChurnController induces a churn in the overlay network by making node failures.
 */
public class ChurnController implements Control {
/*============================================================================*/
// parameters
/*============================================================================*/

private static final String PARAM_BEGIN_TIME = "begintime"; // in which turn begin to fail
private static final String PARAM_PERCENTAGE = "percentage"; // failure percentage

/*============================================================================*/
// fields
/*============================================================================*/

private int beginTime;
private double percentage;

/*============================================================================*/
// initializer
/*============================================================================*/

public ChurnController(String prefix) {
    beginTime = Configuration.getInt(prefix + "." + PARAM_BEGIN_TIME);
    percentage = Configuration.getDouble(prefix + "." + PARAM_PERCENTAGE);
    percentage = percentage <= 0.0 ? 0.0 : percentage;
    percentage = percentage >= 1.0 ? 1.0 : percentage;
}

/*============================================================================*/
// methods
/*============================================================================*/

@Override
public boolean execute() {
    if (CommonState.getTime() != beginTime) {
        return false;
    }
    int n = Network.size();
    int k = (int)Math.round(percentage*n);
    int[] nodeindexs = Util.pickup(k, n);
    for(int i=0; i<nodeindexs.length; i++) {
        Network.get(nodeindexs[i]).setFailState(Fallible.DEAD);
    }
    System.out.println("induced churn in network: "+k+" node are killed");
    return false;
}

}

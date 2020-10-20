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

private static final String PARAM_BEGIN_TURN = "beginturn"; // in which turn begin to fail
private static final String PARAM_PERCENTAGE = "percentage"; // failure percentage

/*============================================================================*/
// fields
/*============================================================================*/

private int beginTurn;
private double percentage;

/*============================================================================*/
// initializer
/*============================================================================*/

public ChurnController(String prefix) {
    beginTurn = Configuration.getInt(prefix + "." + PARAM_BEGIN_TURN);
    percentage = Configuration.getDouble(prefix + "." + PARAM_PERCENTAGE);
    percentage = percentage <= 0.0 ? 0.0 : percentage;
    percentage = percentage >= 1.0 ? 1.0 : percentage;
}

/*============================================================================*/
// methods
/*============================================================================*/

@Override
public boolean execute() {
    if (CommonState.getTime() != beginTurn) {
        return false;
    }
    int n = Network.size();
    int[] nodeindexs = Util.pickup((int)Math.round(percentage*n), n);
    for(int i=0; i<nodeindexs.length; i++) {
        Network.get(nodeindexs[i]).setFailState(Fallible.DOWN);
    }

    return false;
}

}

package sims.collect;

import java.util.HashMap;
import java.util.Set;

import peersim.cdsim.*;
import peersim.core.*;

public class CollectProtocol implements CDProtocol, HitsConfigurable, Deliverable {

/*============================================================================*/
// parameters
/*============================================================================*/


/*============================================================================*/
// fields
/*============================================================================*/
private boolean hit;
private HashMap<Integer, Integer> fathers;
private Set<Message> mailbox;

/*============================================================================*/
// constructor
/*============================================================================*/
public CollectProtocol(String prefix) {
    hit = false;
    fathers = new HashMap<>();
}

/*============================================================================*/
// methods
/*============================================================================*/

public void deliver(Message msg) {
    mailbox.add(msg);
}


@Override
public void nextCycle(Node node, int protocolID) {
    if (!node.isUp()) {
        return;
    }

    for(Message msg : mailbox) {
        
    }

}

@Override
public void SetHit() {
    hit = true;
}

@Override
public Object clone() {
    try {
        CollectProtocol that = (CollectProtocol) super.clone();
        that.hit = this.hit;
        that.fathers = new HashMap<>(this.fathers);
        return that;
    }catch(CloneNotSupportedException e) {
        e.printStackTrace();
    }
    return null;
}

/*============================================================================*/
// helper
/*============================================================================*/



}

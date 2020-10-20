package sims.collect;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Node;

public class BroadcastObserver implements Control{

/*============================================================================*/
// parameters
/*============================================================================*/
static private final String ParamProtocol = "protocol";

/*============================================================================*/
// fields
/*============================================================================*/

private int protocolID;

// statistics
private int msgSendTotal;

/*============================================================================*/
// static functions
// they are the callbacks to notify observer after messages have been sent.
/*============================================================================*/

static public void handleSendMsg(Node from, Node to, Message msg) {
   
}

/*============================================================================*/
// constructor
/*============================================================================*/
public BroadcastObserver(String prefix) {
   this.protocolID = Configuration.getPid(prefix + "." + ParamProtocol);
   this.msgSendTotal = 0;
}


/*============================================================================*/
// methods
/*============================================================================*/

public boolean execute() {
   return false;
}

}
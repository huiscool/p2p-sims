package sims.adhoc;

import peersim.cdsim.CDProtocol;
import peersim.core.*;

public class AdhocTreeProtocol implements CDProtocol {

/*============================================================================*/
// parameters
/*============================================================================*/
public final String ParamProtocol = "protocol";

/*============================================================================*/
// fields
/*============================================================================*/




/*============================================================================*/
// method
/*============================================================================*/

@Override
public void nextCycle(Node node, int protocolID)  {
    
}

public Object clone() {
    try {
        AdhocTreeProtocol adhoc = (AdhocTreeProtocol) super.clone();
        // TODO
        return adhoc;
    }catch(CloneNotSupportedException e){
        e.printStackTrace();
    }
    return null;
}

/*============================================================================*/
// helper
/*============================================================================*/




}

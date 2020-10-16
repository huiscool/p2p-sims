package sims.pingpong;

import peersim.config.*;
import peersim.core.*;

public class Initializer implements Control {
    private final String parmFirst = "first";
    private final String parmProtocol = "protocol";
    private final int first;
    private final int pid;
    
    public Initializer(String prefix) {
        first = Configuration.getInt(prefix + "." + parmFirst);
        pid = Configuration.getPid(prefix + "." + parmProtocol);
    }

    public boolean execute() {
        Pingpong p = (Pingpong) Network.get(first).getProtocol(pid);
        p.HasBall = true;
        return false;
    }
}

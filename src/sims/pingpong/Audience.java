package sims.pingpong;

import java.util.ArrayList;
import peersim.core.*;
import peersim.config.*;

public class Audience implements Control {
    private final String parmProtocol = "protocol";
    private final int pid;
    public Audience(String prefix) {
        pid = Configuration.getPid(prefix + "." + parmProtocol);
    }
    public boolean execute() {
        ArrayList<Integer> l = new ArrayList<Integer>(2);
        for (int i = 0; i<Network.size(); i++) {
            Pingpong p = (Pingpong) Network.get(i).getProtocol(pid);
            l.add(p.HasBall ? 1 : 0);
        }
        System.out.println(l);
        return false;
    }
}
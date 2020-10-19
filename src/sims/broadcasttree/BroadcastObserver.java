package sims.broadcasttree;

import peersim.config.Configuration;
import peersim.core.*;
import peersim.util.IncrementalStats;

import java.util.HashMap;
import java.util.Map;

public class BroadcastObserver implements Control {

    private int pid;

    private static final String PAR_PROTOCOL = "protocol";

    public static boolean broadcastCycle = true;

    private int lastEpidemicCount = -1;

    public static IncrementalStats averageMaxSteps = new IncrementalStats();

    public static Map<Integer, Integer> steps = new HashMap<>();

    public static int broadcastNum = 0;


    public BroadcastObserver(String name) {
        this.pid = Configuration.getPid(name + "." + PAR_PROTOCOL);
    }

    @Override
    public boolean execute() {
        if (!broadcastCycle) return false;
        int count = 0;
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            BroadcastRule pro = ((BroadcastRule) node.getProtocol(pid));
            if (pro.eagerReceivedTime != -100) count++;
        }
        if (count == Network.size() || count == lastEpidemicCount) {
            if (broadcastNum < 10) {
                System.out.println("count is :" + count);
                System.out.println("broadcast num is : " + broadcastNum);
                if (broadcastNum == 0) {
                    BroadcastRule.initialed = true;
                }
                int maxSteps = 0;
                for (int j = 0; j < Network.size(); j++) {
                    if (!Network.get(j).isUp()) continue;
                    //get statistics steps of leaf node
                    int step = ((BroadcastRule) Network.get(j).getProtocol(pid)).getSteps();
                    if (((BroadcastRule) Network.get(j).getProtocol(pid)).childrenNum == 0)
                        steps.put(step, steps.getOrDefault(step, 0) + 1);
                    maxSteps = Integer.max(maxSteps, step);
                }
                averageMaxSteps.add(maxSteps);
                //继续广播
                broadcastNum++;
                broadcastCycle = false;
                System.out.println("setting false to broadcastCycle");
                System.out.println("有父节点的节点有" + getNodeHasEagerFatherNum());
                return false;
            } else return true;
        }
        lastEpidemicCount = count;
        return false;
    }

    private int getNodeHasEagerFatherNum() {
        int count = 0;
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            BroadcastRule pro = ((BroadcastRule) node.getProtocol(pid));
            if (pro.fatherEagerNode != null) count++;
        }
        return count;
    }
}

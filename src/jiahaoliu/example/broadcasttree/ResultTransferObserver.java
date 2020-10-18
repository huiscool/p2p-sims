package jiahaoliu.example.broadcasttree;

import jiahaoliu.example.broadcasttree.model.QueryResult;
import peersim.config.Configuration;
import peersim.core.*;
import peersim.util.IncrementalStats;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ResultTransferObserver implements Control {

    private static String PAR_PRO = "protocol";
    private static String PAR_DOWNPEERS = "downpeers";

    private int pid;

    private int downPeersPerCycle;

    public static IncrementalStats averageDedup = new IncrementalStats();

    public ResultTransferObserver(String name) {
        pid = Configuration.getPid(name + "." + PAR_PRO);
        this.downPeersPerCycle = Configuration.getInt(name + "." + PAR_DOWNPEERS) / 10;
    }

    @Override
    public boolean execute() {
        if (BroadcastObserver.broadcastCycle) return false;
        boolean collectFinished = true;
        BroadcastRule rootContent = null;
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            BroadcastRule childContent = ((BroadcastRule) node.getProtocol(pid));
            Node fatherNode = childContent.fatherEagerNode;
            if (fatherNode == null) {//广播根节点
                if (childContent.childrenNum != 0) {
                    rootContent = childContent;
                    System.out.println("children num is  : " + rootContent.childrenNum);
                }
                continue;
            }
            if (!childContent.msgTransfered && childContent.childrenNum == childContent.childrenReplyNum) {
                collectFinished = false;
                BroadcastRule fatherContent = (BroadcastRule) fatherNode.getProtocol(pid);
                childContent.msgTransfered = true;
                fatherContent.childrenReplyNum++;
                fatherContent.receivedTotalNum += childContent.getResults().size();
                fatherContent.getResults().addAll(childContent.getResults());
            }
        }
        if (collectFinished) {
            System.out.println("root content received " + rootContent.receivedTotalNum);
            System.out.println("total content num is : " + rootContent.getResults().size());
            averageDedup.add(1.0 - rootContent.receivedTotalNum / 12000.0);
            BroadcastObserver.broadcastCycle = true;
            //重新发送查询条件
            resetToQuery();
            //随机down 一部分节点
            downPeerRandom();
            //不结束执行
            return false;
        }
        return false;
    }

    private void resetToQuery() {
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            BroadcastRule pro = ((BroadcastRule) node.getProtocol(pid));
            pro.childrenNum = 0;
            pro.eagerReceivedTime = -100;
            pro.lazyReceivedTime = -100;
            pro.lazyReceivedPeer = null;
            pro.fatherEagerNode = null;
            pro.steps = 0;
            // reset transfer data
            pro.getResults().clear();
            pro.childrenReplyNum = 0;
            pro.receivedTotalNum = 0;
            pro.msgTransfered = false;
        }
        ((BroadcastRule) MsgBroadcastInitializer.rootNode.getProtocol(pid)).eagerReceivedTime = CommonState.getTime();//下次继续广播
        generateQueryResult();
    }

    private void generateQueryResult() {
        Set<String> set = new HashSet<>();
        while (set.size() < 1000) {
            set.add(UUID.randomUUID().toString());
        }
        Random r = new Random();
        for (String transactionId : set)
            for (int i = 0; i < 12; i++) {
                ((BroadcastRule) Network.get(r.nextInt(Network.size())).getProtocol(pid)).getResults().add(new QueryResult(i, transactionId));
            }
    }

    private void downPeerRandom() {
        //每次down 10个
        for (int i = 0; i < downPeersPerCycle; ) {
            Node node = Network.get(new Random().nextInt(Network.size()));
            if (node == MsgBroadcastInitializer.rootNode) continue;
            if (node.isUp()) {
                i++;
            }
            node.setFailState(Fallible.DOWN);
        }
    }
}

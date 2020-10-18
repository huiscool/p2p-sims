package jiahaoliu.example.broadcasttree;

import jiahaoliu.example.broadcasttree.Util.FileOperator;
import jiahaoliu.example.broadcasttree.model.QueryResult;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.stream.Collectors;

public class BroadcastRule implements CDProtocol {

    private static final String kSteps = "k";

    long eagerReceivedTime;
    long lazyReceivedTime;
    boolean msgTransfered;
    Node lazyReceivedPeer;
    Node fatherEagerNode;
    String name;
    private static int k;
    public int steps = 0;//跳数
    public int childrenReplyNum;//几个孩子节点返回了数据
    public int receivedTotalNum;//接收到的总数据量大小


    private Set<QueryResult> results = new HashSet<>();

    public static boolean initialed = false;

    public int childrenNum;

    public BroadcastRule(String name) {
        this.name = name;
        this.lazyReceivedTime = -100;
        this.eagerReceivedTime = -100;
        this.msgTransfered = false;
        k = Configuration.getInt(name + "." + kSteps);
        this.childrenReplyNum = 0;
        this.childrenNum = 0;
        this.receivedTotalNum = 0;
    }

    public int getSteps() {
        return steps;
    }

    public Set<QueryResult> getResults() {
        return results;
    }

    @Override
    public void nextCycle(Node node, int protocolID) {
        //如果是查询结果返回的阶段，不进行广播
        if (!BroadcastObserver.broadcastCycle) return;
        if (!initialed) {
            generateTree(node, protocolID);
        } else {
            broadcastMsg(node, protocolID);
        }
    }

    private void broadcastMsg(Node node, int protocolID) {
        eagerMessageBroadcast(node, protocolID);
        lazyMessageBroadcast(node, protocolID);
    }

    private void eagerMessageBroadcast(Node node, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        BroadcastRule nodeContent = ((BroadcastRule) node.getProtocol(protocolID));
        List<Node[]> eagerToPassiveTmp = new ArrayList<>();
        if (nodeContent.eagerReceivedTime == CommonState.getTime() - 1) {
            //只有是上个周期收到的消息，才发送给邻居节点
            for (Node eagerNode : linkable.getEagerPeers()) {
                //不应该再把数据返回给父节点
                if (eagerNode == nodeContent.fatherEagerNode || ((BroadcastRule) eagerNode.getProtocol(protocolID)).eagerReceivedTime != -100)
                    continue;
                if (!eagerNode.isUp()) {
//                    FileOperator.writeToFile("down peer");
                    eagerToPassiveTmp.add(new Node[]{node, eagerNode});
                    continue;
                }
                BroadcastRule childContent = ((BroadcastRule) eagerNode.getProtocol(protocolID));
                childContent.eagerReceivedTime = CommonState.getTime();
                nodeContent.childrenNum++;
                childContent.steps = nodeContent.steps + 1;
                childContent.fatherEagerNode = node;
            }
        }
        for (Node[] tuple : eagerToPassiveTmp)
            replaceEagerNode(tuple[0], tuple[1], protocolID);
    }

    private void lazyMessageBroadcast(Node node, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        BroadcastRule nodeContent = ((BroadcastRule) node.getProtocol(protocolID));
        if (nodeContent.eagerReceivedTime == CommonState.getTime() - k - 1) {
            List<Node[]> lazyToEagerTmp = new ArrayList<>();
            List<Node[]> lazyToPassive = new ArrayList<>();
            //lazy message 滞后K个cycle
            for (Node lazyNode : linkable.getLazyPeers()) {
                BroadcastRule childContent = ((BroadcastRule) lazyNode.getProtocol(protocolID));
                if (!lazyNode.isUp()) {
                    lazyToPassive.add(new Node[]{node, lazyNode});
                    continue;
                }
                if (childContent.eagerReceivedTime == -100) {
                    //未通过eager link收到消息
                    childContent.eagerReceivedTime = CommonState.getTime();
                    lazyToEagerTmp.add(new Node[]{lazyNode, node});
                    nodeContent.childrenNum++;
                    childContent.steps = nodeContent.steps + 1;
                    childContent.fatherEagerNode = node;
                }
            }
            for (Node[] tuple : lazyToPassive)
                replaceLazyNode(tuple[0], tuple[1], protocolID);
            for (Node[] tuple : lazyToEagerTmp) lazyLinkToEagerLink(tuple[0], tuple[1], protocolID);
        }
    }

    public Node getCandidate(Node node, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        Node minLazyPeersNode = linkable.passiveNodes
                .stream().filter(x -> x.isUp() && ((EagerLazyLink) x.getProtocol(FastConfig.getLinkable(protocolID))).getLazyPeers().size() > 0)
                .min(Comparator.comparingInt(x -> ((EagerLazyLink) x.getProtocol(FastConfig.getLinkable(protocolID))).getLazyPeers().size())).get();
        if (minLazyPeersNode == null) {
            //TODO:所有的节点都没有lazy link
            FileOperator.writeToFile("没有节点有lazy link ");
        }
//        linkable.getLazyPeers().add(minLazyPeersNode);
        //TODO:如果passive list中可用于连接的节点，小于某一个阈值，更新passive list
        int activeNodeNum = (int) linkable.passiveNodes.stream().filter(x -> x.isUp()).count();
        if (activeNodeNum < linkable.passiveSize / 2) {
            System.out.println("passive link is too few.  updating...");
            //生成一个10以内的随机数K，从这K跳中选择最近更新的在线节点
            fillPassivePeers(node, new Random().nextInt(8) + 2, protocolID);
        }
        return minLazyPeersNode;
    }

    private void replaceLazyNode(Node node, Node rNode, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        linkable.getLazyPeers().remove(rNode);
        linkable.passiveNodes.add(rNode);

        EagerLazyLink rLink = (EagerLazyLink) rNode.getProtocol(FastConfig.getLinkable(protocolID));
        rLink.getLazyPeers().remove(node);
        rLink.passiveNodes.add(node);

        Node candidate = getCandidate(node, protocolID);
        addLazyPeerToEachOther(node, candidate, protocolID);
    }

    private void replaceEagerNode(Node node, Node rNode, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        linkable.getEagerPeers().remove(rNode);
        linkable.passiveNodes.add(rNode);

        EagerLazyLink rLink = (EagerLazyLink) rNode.getProtocol(FastConfig.getLinkable(protocolID));
        rLink.getEagerPeers().remove(node);
        rLink.passiveNodes.add(node);

        addLazyPeerToEachOther(node, getCandidate(node, protocolID), protocolID);
    }

    private void fillPassivePeers(Node node, int hop, int protocolID) {
        FileOperator.writeToFile("fill passive peers");
        int hops = 0;
        EagerLazyLink nLink = getNodeLinkable(node, protocolID);
        Set<Node> nPSet = new HashSet<>(nLink.passiveNodes.stream().filter(x -> x.isUp()).collect(Collectors.toSet()));
        while (nPSet.size() < nLink.passiveSize && hops < hop) {
            Node reNode = getRandomEagerPeer(node, protocolID);
            List<Node> passiveNodes = getNodeLinkable(reNode, protocolID).passiveNodes.stream().filter(x -> x.isUp()).collect(Collectors.toList());
            nPSet.addAll(passiveNodes);
            node = reNode;
            hops++;
        }
        nLink.passiveNodes = nPSet.stream().limit(nLink.passiveSize).collect(Collectors.toList());
    }

    private void addLazyPeerToEachOther(Node shortageNode, Node provideNode, int protocolID) {
        EagerLazyLink sLink = getNodeLinkable(shortageNode, protocolID), pLink = getNodeLinkable(provideNode, protocolID);
        pLink.getLazyPeers().remove(new Random().nextInt(pLink.getLazyPeers().size()));
        pLink.getLazyPeers().add(shortageNode);
        sLink.getLazyPeers().add(provideNode);
    }

    private EagerLazyLink getNodeLinkable(Node node, int protocolID) {
        return (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
    }

    private Node getRandomEagerPeer(Node node, int protocolID) {
        EagerLazyLink link = getNodeLinkable(node, protocolID);
        Random r = new Random();
        Node res;
        do {
            res = link.getEagerPeers().get(r.nextInt(link.getEagerPeers().size()));
        } while (!res.isUp());
        return res;
    }

    private void generateTree(Node node, int protocolID) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(protocolID));
        //节点在上一个周期收到消息，才广播给其他人
        BroadcastRule nodeContent = ((BroadcastRule) node.getProtocol(protocolID));
        //eager 当收到的时间是上次，lazy当收到的时间是上k次，就可以通过相应的频道广播
        if (nodeContent.eagerReceivedTime != CommonState.getTime() - 1 && nodeContent.lazyReceivedTime != CommonState.getTime() - k - 1)
            return;
        if (nodeContent.lazyReceivedTime == CommonState.getTime() - k - 1 &&
                nodeContent.eagerReceivedTime == -100) {
            //将lazy peer 提升至 eager peer
            lazyLinkToEagerLink(node, nodeContent.lazyReceivedPeer, protocolID);
            //设置接收时间，当即广播
            nodeContent.eagerReceivedTime = CommonState.getTime();
            nodeContent.fatherEagerNode = nodeContent.lazyReceivedPeer;
            nodeContent.lazyReceivedPeer = null;
            ((BroadcastRule) nodeContent.fatherEagerNode.getProtocol(protocolID)).childrenNum++;
//            nodeContent.fatherEagerNode = fatherEagerNode;
        }
        if (nodeContent.eagerReceivedTime != CommonState.getTime() - 1) return;

        List<Node[]> eagerToLazyTmp = new ArrayList<>();
        for (Node eagerPeer : linkable.getEagerPeers()) {
            //不应该把消息再次广播给父节点
            if (nodeContent.fatherEagerNode == eagerPeer) continue;
            BroadcastRule eagerPeerContent = ((BroadcastRule) eagerPeer.getProtocol(protocolID));
            if (eagerPeerContent.eagerReceivedTime == -100) {
                eagerPeerContent.eagerReceivedTime = CommonState.getTime();
                eagerPeerContent.steps = nodeContent.steps + 1;
                eagerPeerContent.fatherEagerNode = node;
                nodeContent.childrenNum++;
            } else {
                //提前收到了该消息，将eager peer放入到lazy peer中
                eagerToLazyTmp.add(new Node[]{eagerPeer, node});
            }
        }
        for (Node[] tuple : eagerToLazyTmp) eagerLinkToLazyLink(tuple[0], tuple[1], protocolID);
        for (Node lazyPeer : linkable.getLazyPeers()) {
            BroadcastRule lazyPeerContent = ((BroadcastRule) lazyPeer.getProtocol(protocolID));
            if (lazyPeer == lazyPeerContent.lazyReceivedPeer) continue;
            if (lazyPeerContent.lazyReceivedTime == -100 && lazyPeerContent.eagerReceivedTime == -100) {
                lazyPeerContent.lazyReceivedTime = CommonState.getTime();
                lazyPeerContent.steps = nodeContent.steps + 1;
                lazyPeerContent.lazyReceivedPeer = node;
            }
        }
    }

    private void eagerLinkToLazyLink(Node node1, Node node2, int protocolID) {
        EagerLazyLink linkable1 = (EagerLazyLink) node1.getProtocol(FastConfig.getLinkable(protocolID));
        EagerLazyLink linkable2 = (EagerLazyLink) node2.getProtocol(FastConfig.getLinkable(protocolID));
        linkable1.getLazyPeers().add(node2);
        linkable1.getEagerPeers().remove(node2);
        linkable2.getLazyPeers().add(node1);
        linkable2.getEagerPeers().remove(node1);
    }

    private void lazyLinkToEagerLink(Node node1, Node node2, int protocolID) {
        EagerLazyLink linkable1 = (EagerLazyLink) node1.getProtocol(FastConfig.getLinkable(protocolID));
        EagerLazyLink linkable2 = (EagerLazyLink) node2.getProtocol(FastConfig.getLinkable(protocolID));
        linkable1.getLazyPeers().remove(node2);
        linkable1.getEagerPeers().add(node2);
        linkable2.getLazyPeers().remove(node1);
        linkable2.getEagerPeers().add(node1);
    }

    @Override
    public Object clone() {
        try {
            BroadcastRule rule = (BroadcastRule) super.clone();
            rule.lazyReceivedTime = this.lazyReceivedTime;
            rule.eagerReceivedTime = this.eagerReceivedTime;
            rule.msgTransfered = this.msgTransfered;
            rule.name = this.name;
            rule.lazyReceivedPeer = this.lazyReceivedPeer;
            rule.fatherEagerNode = this.fatherEagerNode;
            rule.steps = this.steps;
            rule.childrenNum = 0;
            rule.results = (this.results == null ? null : new HashSet<>(this.results));
            rule.childrenReplyNum = 0;
            rule.receivedTotalNum = this.receivedTotalNum;
            return rule;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

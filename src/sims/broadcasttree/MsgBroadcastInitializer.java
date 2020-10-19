package sims.broadcasttree;

import sims.broadcasttree.model.QueryResult;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.util.*;
import java.util.stream.IntStream;

public class MsgBroadcastInitializer implements Control {

    private static final String PAR_PRO = "protocol";
    private static final String PAR_RESULT_NUM = "resultnum";
    private static final String PAR_FANOUT = "k";
    private static final String PAR_ROOT_FANOUT = "rootk";


    private int pid;
    private int resultNum;

    public Set<Node> notFullNodes = new HashSet<>();

    private int k;

    private int rootk;

    public static Node rootNode = null;

    public MsgBroadcastInitializer(String name) {
        this.pid = Configuration.getPid(name + "." + PAR_PRO);
        this.resultNum = Configuration.getInt(name + "." + PAR_RESULT_NUM, 100);
        this.k = Configuration.getInt(name + "." + PAR_FANOUT);
        this.rootk = Configuration.getInt(name + "." + PAR_ROOT_FANOUT);
    }

    @Override
    public boolean execute() {
        initializeSet();
        initializeReceiveTime();
        generateQueryResult();
        fillEagerPeers();
        fillPassivePeers();
        return false;
    }

    private void fillEagerPeers() {
        for (int i = 0; i < Network.size(); i++) {
            EagerLazyLink linkable = (EagerLazyLink) Network.get(i).getProtocol(FastConfig.getLinkable(pid));
            linkable.getEagerPeers().clear();
        }
        for (int i = 0; i < Network.size(); i++) {
            EagerLazyLink linkable = (EagerLazyLink) Network.get(i).getProtocol(FastConfig.getLinkable(pid));
            while (linkable.getEagerPeers().size() < getFanout(Network.get(i) == rootNode)) {
                if (notFullNodes.isEmpty()) return;
                Node nei = getRandomFromSet(Network.get(i));
                if (nei == null) break;
                EagerLazyLink rLink = (EagerLazyLink) nei.getProtocol(FastConfig.getLinkable(pid));
                linkable.getEagerPeers().add(nei);
                if (linkable.getEagerPeers().size() == getFanout(Network.get(i) == rootNode))
                    notFullNodes.remove(Network.get(i));
                rLink.getEagerPeers().add(Network.get(i));
                if (rLink.getEagerPeers().size() == getFanout(Network.get(i) == rootNode)) notFullNodes.remove(nei);
            }
            if (i % 100 == 0)
                System.out.println("filling eager peers, peer node: " + i);
        }
    }

    private int getFanout(boolean isRoot) {
        return isRoot ? rootk : k;
    }

    private void initializeSet() {
        for (int i = 0; i < Network.size(); i++) {
            notFullNodes.add(Network.get(i));
        }
    }

    private void initializeReceiveTime() {
        for (int i = 0; i < Network.size(); i++) {
            ((BroadcastRule) Network.get(i).getProtocol(pid)).eagerReceivedTime = -100;
        }
        Node node = Network.get(0);
        rootNode = node;
        ((BroadcastRule) node.getProtocol(pid)).eagerReceivedTime = -1;
    }

    private Node getRandomFromSet(Node node) {
        EagerLazyLink linkable = (EagerLazyLink) node.getProtocol(FastConfig.getLinkable(pid));
        Set<Node> nSet = new HashSet<>(notFullNodes);
        nSet.removeAll(linkable.getEagerPeers());
        nSet.remove(node);
        if (nSet.isEmpty()) return null;
        int index = new Random().nextInt(nSet.size());
        Iterator<Node> iter = nSet.iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        return iter.next();
    }

    public void fillPassivePeers() {
        for (int i = 0; i < Network.size(); i++) {
            EagerLazyLink linkable = (EagerLazyLink) Network.get(i).getProtocol(FastConfig.getLinkable(pid));
            IntStream.range(0, Network.size()).distinct().limit(linkable.passiveSize).forEach(x -> linkable.passiveNodes.add(Network.get(x)));
        }
    }

    public void generateQueryResult() {
        Set<String> uuidSet = new HashSet<>();
        while (uuidSet.size() < resultNum) {
            uuidSet.add(UUID.randomUUID().toString());
        }
        Random r = new Random();
        for (String uuid : uuidSet) {
            for (int i = 0; i < 12; i++) {
                QueryResult result = new QueryResult(i, uuid);
                BroadcastRule rule = ((BroadcastRule) Network.get(r.nextInt(Network.size())).getProtocol(pid));
                rule.getResults().add(result);
            }
        }
    }
}

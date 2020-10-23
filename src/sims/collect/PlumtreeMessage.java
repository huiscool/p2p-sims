package sims.collect;

public class PlumtreeMessage extends Message {
    class Gossip {
    }
    class Graft {
    }
    class Prune {
    }
    class IHave {
    }

    Gossip gossip;
    Graft graft;
    Prune prune;
    IHave iHave;

    public String toString() {
        return "@" + Integer.toHexString(System.identityHashCode(this))+"{" +
        "id:" + id + "," + 
        "from:" + fromNodeIndex + "," + 
        ((gossip != null)? "gossip,":"") +
        ((graft != null)? "graft,":"") +
        ((prune != null)? "prune,":"") +
        ((iHave != null)? "ihave,":"") +
        "}";
    }
}

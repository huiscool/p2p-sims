package sims.collect;

public class PlumtreeMessage extends Message {
    Boolean isGossip = false;
    Boolean isGraft  = false;
    Boolean isPrune  = false;
    Boolean isIHave  = false;

    public String toString() {
        return "@" + Integer.toHexString(System.identityHashCode(this))+"{" +
        "id:" + id + "," + 
        "from:" + fromNodeIndex + "," + 
        (isGossip ? "gossip,": "") +
        (isGraft  ? "graft," : "") +
        (isPrune  ? "prune," : "") +
        (isIHave  ? "ihave," : "") +
        "}";
    }
}

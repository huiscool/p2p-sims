package sims.collect;

import peersim.core.*;

// Message is an abstraction of query payload.
// it can be a request or a response or a control message.
public class Message implements Cloneable {

    public int size;
    public int id;
    public int hop;
    public Node root;
    public Node from;
    public MessageType type;

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message)) {
            return false;
        }
        Message that = (Message) obj;
        return this.id == that.id;
    }

    @Override
    public Object clone() {
        try {
            Message msg = (Message) super.clone();
            // add reference copy
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Message hopFrom(Node node) {
        Message next = (Message) this.clone();
        next.hop++;
        next.from = node;
        return next;
    }
    
    public static Message New(String type) {
        Message msg;
        switch(type) {
            case "plumtree":
                PlumtreeMessage pmsg = new PlumtreeMessage();
                pmsg.isGossip = true;
                msg = pmsg;
                break;
            default:
                msg = new Message();
        }
        msg.type = MessageType.Request;
        return msg;
        
    }
}

class PlumtreeMessage extends Message {
    Boolean isGossip = false;
    Boolean isGraft  = false;
    Boolean isPrune  = false;
    Boolean isIHave  = false;

    public String toString() {
        return "@" + Integer.toHexString(System.identityHashCode(this))+"{" +
        "id:" + id + "," + 
        "from:" + from.getIndex() + "," + 
        (isGossip ? "gossip,": "") +
        (isGraft  ? "graft," : "") +
        (isPrune  ? "prune," : "") +
        (isIHave  ? "ihave," : "") +
        "}";
    }
}

enum MessageType {
    Request,
    Response,
    Control
}
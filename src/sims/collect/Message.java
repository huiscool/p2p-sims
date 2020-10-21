package sims.collect;

// Message is an abstraction of query payload.
// it can be a request or a response or a control message.
public class Message implements Cloneable {

    public int size;
    public int id;
    public int hop;
    public int fromNodeIndex;
    public int rootNodeIndex;

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
}
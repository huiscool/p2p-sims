package sims.collect;

// Message is an abstraction of query payload.
// it can be a request or a response or a control message.
public class Message implements Cloneable {

    private int size;
    private int id;

    public Message(int size, int id) {
        this.size = size;
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public int getID() {
        return id; 
    }

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


class MessageDigest extends Message {

private static final int DEFAULT_DIGEST_SIZE = 5;

public MessageDigest(int size, int id) {
    super(size, id);
}

@Override
public int getSize() {
    return DEFAULT_DIGEST_SIZE;
}

}


package sims.collect;

// Message is an abstraction of query payload.
// it can be a request or a response or a control message.
public class Message {

    private int size;
    private int id;

    public Message(int size, int id) {
        this.size = size;
        this.id = id;
    }

    public int GetSize() {
        return size;
    }

    public int GetID() {
       return id; 
    }
}

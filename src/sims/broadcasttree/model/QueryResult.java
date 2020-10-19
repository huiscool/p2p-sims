package sims.broadcasttree.model;

public class QueryResult {
    private int blockId;
    private String transactionId;

    public QueryResult(int blockId, String transactionId) {
        this.blockId = blockId;
        this.transactionId = transactionId;
    }

    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        QueryResult other = (QueryResult) obj;
        return other.transactionId.equals(this.transactionId);
    }
}

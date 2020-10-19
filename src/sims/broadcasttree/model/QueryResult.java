package sims.broadcasttree.model;

public class QueryResult {
    private String transactionId;

    public QueryResult(int blockId, String transactionId) {
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

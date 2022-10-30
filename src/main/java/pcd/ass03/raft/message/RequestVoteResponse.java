package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestVoteResponse {
    public final int term;
    public final boolean accepted;

    @JsonCreator
    public RequestVoteResponse(@JsonProperty("term") int term, @JsonProperty("accepted") boolean accepted) {
        this.term = term;
        this.accepted = accepted;
    }
}

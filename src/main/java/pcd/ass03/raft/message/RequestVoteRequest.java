package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestVoteRequest {
    public final int term;
    public final int candidateId;
    public final int lastLogIndex;
    public final int lastLogTerm;

    @JsonCreator
    public RequestVoteRequest(
        @JsonProperty("term") int term,
        @JsonProperty("candidateId") int candidateId,
        @JsonProperty("lastLogIndex") int lastLogIndex,
        @JsonProperty("lastLogTerm") int lastLogTerm
    ) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }
}

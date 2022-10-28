package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestVoteResponse {
    public final boolean accepted;

//    public RequestVoteResponse() { }
    @JsonCreator
    public RequestVoteResponse(@JsonProperty("accepted") boolean accepted) {
        this.accepted = accepted;
    }
}

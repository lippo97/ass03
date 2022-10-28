package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppendEntriesResponse {
    public final int term;
    public final boolean success;

    @JsonCreator
    public AppendEntriesResponse(@JsonProperty("term") int term, @JsonProperty("success") boolean success) {
        this.term = term;
        this.success = success;
    }
}

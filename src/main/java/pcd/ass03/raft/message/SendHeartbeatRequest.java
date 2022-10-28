package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SendHeartbeatRequest {
    public final int leaderId;

    @JsonCreator
    public SendHeartbeatRequest(@JsonProperty("leaderId") int leaderId) {
        this.leaderId = leaderId;
    }
}

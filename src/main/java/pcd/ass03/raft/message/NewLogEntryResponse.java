package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NewLogEntryResponse {
    public final boolean success;

    @JsonCreator
    public NewLogEntryResponse(@JsonProperty("success") boolean success) {
        this.success = success;
    }
}

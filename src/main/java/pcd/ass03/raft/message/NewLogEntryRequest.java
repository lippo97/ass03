package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NewLogEntryRequest<A> {
    public final int source;
    public final List<A> entries;

    @JsonCreator
    public NewLogEntryRequest(@JsonProperty("source") int source, @JsonProperty("entries") List<A> entries) {
        this.source = source;
        this.entries = entries;
    }
}

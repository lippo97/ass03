package pcd.ass03.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LogEntry<A> {
    public final int term;
    public final A value;

    @JsonCreator
    public LogEntry(@JsonProperty("term") int term, @JsonProperty("value") A value) {
        this.term = term;
        this.value = value;
    }
}

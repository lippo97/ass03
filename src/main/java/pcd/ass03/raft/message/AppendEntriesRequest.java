package pcd.ass03.raft.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pcd.ass03.raft.LogEntry;

import java.util.List;

public class AppendEntriesRequest<A> {
    /**
     * The current leader term.
     */
    public final int term;
    /**
     * The current leader id.
     */
    public final int leaderId;
    /**
     * Index of log entry immediatly preceding new ones.
     */
    public final int prevLogIndex;
    /**
     * Term of prevLogIndex entry.
     */
    public final int prevLogTerm;
    /**
     * Log entries to store (empty for heartbeats)
     */
    public final List<LogEntry<A>> entries;
    /**
     * Leader's commit index.
     */
    public final int leaderCommit;

    @JsonCreator
    public AppendEntriesRequest(
        @JsonProperty("term") int term,
        @JsonProperty("leaderId") int leaderId,
        @JsonProperty("prevLogIndex") int prevLogIndex,
        @JsonProperty("prevLogTerm") int prevLogTerm,
        @JsonProperty("entries") List<LogEntry<A>> entries,
        @JsonProperty("leaderCommit") int leaderCommit
    ) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }
}

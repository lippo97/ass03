package pcd.ass03.raft;

import java.util.Collections;
import java.util.Map;

public class RaftParameters {
    private final int id;
    private final Map<Integer, Member> members;

    public RaftParameters(int id, Map<Integer, Member> members) {
        this.id = id;
        this.members = Collections.unmodifiableMap(members);
    }

    public int getId() {
        return id;
    }

    public Map<Integer, Member> getMembers() {
        return members;
    }
}

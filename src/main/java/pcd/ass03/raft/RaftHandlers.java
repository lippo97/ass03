package pcd.ass03.raft;

import io.vertx.core.Handler;

public interface RaftHandlers<A> {
    void onBecomeLeader(Handler<Integer> handler);
    void onApplyLogEntry(Handler<A> handler);
}

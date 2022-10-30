package pcd.ass03.raft;

import io.vertx.core.Future;
import io.vertx.core.Verticle;

import java.util.List;

public interface RaftPeer<A> extends Verticle, RaftHandlers<A> {

    Future<Void> pushLogEntries(List<A> entries);

    default Future<Void> pushLogEntry(A entry) {
        return pushLogEntries(List.of(entry));
    }

     static <B> RaftPeer<B> make(final Parameters parameters) {
         return new RaftPeerImpl<B>(parameters);
     }
}

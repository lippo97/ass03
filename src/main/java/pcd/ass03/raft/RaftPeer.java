package pcd.ass03.raft;

import io.vertx.core.Verticle;

public interface RaftPeer<A> extends Verticle, RaftHandlers<A> {

     static <B> RaftPeer<B> make(final Parameters parameters) {
         return new RaftPeerImpl<B>(parameters);
     }
}

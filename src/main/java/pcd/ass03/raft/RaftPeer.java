package pcd.ass03.raft;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import pcd.ass03.raft.message.AppendEntriesRequest;
import pcd.ass03.raft.message.NewLogEntryRequest;

import java.util.List;

public interface RaftPeer<A> extends Verticle, RaftHandlers<A> {

    Future<Void> pushLogEntries(List<A> entries);

    default Future<Void> pushLogEntry(A entry) {
        return pushLogEntries(List.of(entry));
    }

     static <B> RaftPeer<B> make(
         final RaftParameters parameters,
         final TypeReference<AppendEntriesRequest<B>> appendEntriesRequestTypeReference,
         final TypeReference<NewLogEntryRequest<B>> newLogEntryRequestTypeReference
     ) {
         return new RaftPeerImpl<B>(parameters, appendEntriesRequestTypeReference, newLogEntryRequestTypeReference);
     }
}

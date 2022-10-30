package pcd.ass03.raft;

import io.vertx.core.Vertx;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        var members = IntStream.range(0, 4)
            .boxed()
            .collect(Collectors.toMap(Function.identity(), Main::createMember));

        members.keySet().forEach((id) -> {
            var parameters = new Parameters(id, members);
            var client = RaftPeer.<Integer>make(parameters);
            client.onBecomeLeader(leaderId -> System.out.printf("I'm the leader (%d)\n", leaderId));
            client.onApplyLogEntry(entry -> System.out.printf("%d | Applying entry %d\n", id, entry));
            vertx.deployVerticle(client);
        });
    }

    private static Member createMember(int id) {
        return new Member(id, "localhost", Utils.getPortForId(id));
    }
}

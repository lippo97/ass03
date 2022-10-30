package pcd.ass03.raft;

import io.vertx.core.Vertx;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        final var vertx = Vertx.vertx();
        final var members = IntStream.range(0, 3)
            .boxed()
            .collect(Collectors.toMap(Function.identity(), Main::createMember));

        members.keySet().forEach((id) -> {
            final var parameters = new Parameters(id, members);
            final var client = RaftPeer.<Integer>make(parameters);
            client.onBecomeLeader(leaderId -> System.out.printf("I'm the leader (%d)\n", leaderId));
            client.onApplyLogEntry(entry -> System.out.printf("%d | Applying entry %d\n", id, entry));
            vertx.deployVerticle(client);

            if (id == 0) {
                vertx.setPeriodic(5000, (_id) -> client.pushLogEntry(0)
                    .onSuccess((_u) -> System.out.println("Push succeeded"))
                    .onFailure(System.err::println));
            }

        });
    }

    private static Member createMember(int id) {
        return new Member(id, "localhost", Utils.getPortForId(id));
    }
}

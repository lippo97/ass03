package pcd.ass03.puzzle.distributed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import pcd.ass03.puzzle.distributed.commands.Command;
import pcd.ass03.puzzle.distributed.commands.Initialize;
import pcd.ass03.raft.RaftPeer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        DatabindCodec.mapper().registerModule(new SimpleModule() {{
            addSerializer(Command.class, Command.serializer);
        }});
        DatabindCodec.prettyMapper().registerModule(new SimpleModule() {{
            addSerializer(Command.class, Command.serializer);
        }});

        try {
            final var parameters = AppParameters.parse(args);
            final var vertx = Vertx.vertx();
            final var raftPeer = RaftPeer.<Command>make(
                parameters.raftParameters,
                new TypeReference<>() {},
                new TypeReference<>() {}
            );
            final var board = new PuzzleBoard(parameters.rows, parameters.columns, parameters.imagePath);
//            final var positions = IntStream.range(0, parameters.rows * parameters.columns)
//                .boxed()
//                .collect(Collectors.toList());

            final var positions = List.of(3,1,2,0);

            raftPeer.onApplyLogEntry((entry) -> {
                System.out.println("applying " + entry);
                if (entry instanceof Initialize) {
                    final var initialize = (Initialize) entry;
                    board.createTiles(initialize.positions);
                    board.setVisible(true);
                }
            });
            raftPeer.onBecomeLeader((id) -> {
                System.out.println(id + " | I'm the leader");
                vertx.setTimer(2000, (timerId) ->
                    raftPeer.pushLogEntry(new Initialize(positions))
                );
            });
            vertx.deployVerticle(raftPeer);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}

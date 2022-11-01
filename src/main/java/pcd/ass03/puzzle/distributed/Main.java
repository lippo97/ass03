package pcd.ass03.puzzle.distributed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import pcd.ass03.puzzle.concentrated.Tile;
import pcd.ass03.puzzle.distributed.commands.Click;
import pcd.ass03.puzzle.distributed.commands.Command;
import pcd.ass03.puzzle.distributed.commands.Initialize;
import pcd.ass03.raft.RaftPeer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        configureCommandSerializer();
        try {
            final var parameters = AppParameters.parse(args);
            final var vertx = Vertx.vertx();
            final var raftPeer = RaftPeer.<Command>make(
                parameters.raftParameters,
                new TypeReference<>() {},
                new TypeReference<>() {}
            );
            final var id = parameters.raftParameters.getId();
            final var title = Integer.toString(id);
            final var board = new PuzzleBoard(title, id, parameters.rows, parameters.columns, parameters.imagePath);

            final var state = new State();

            board.onTileClick((Tile tile) -> {
                raftPeer.pushLogEntry(new Click(id, tile.getCurrentPosition()));
            });
            raftPeer.onApplyLogEntry((entry) -> {
                System.out.println(id + " | applying " + entry);
                if (entry instanceof Initialize) {
                    final var initialize = (Initialize) entry;
                    state.initialized = true;
                    board.createTiles(initialize.positions);
                    board.setVisible(true);
                } else if (entry instanceof Click) {
                    final var click = (Click) entry;
                    board.click(click);
                }
            });
            raftPeer.onBecomeLeader((_id) -> {
                System.out.println(_id + " | I'm the leader " );
                if (!state.initialized) {
                    final var positions = IntStream.range(0, parameters.rows * parameters.columns)
                        .boxed()
                        .collect(Collectors.toList());
                    while (isListSorted(positions)) {
                        System.out.println("Shuffle");
                        Collections.shuffle(positions);
                    }
                    raftPeer.pushLogEntry(new Initialize(positions));
                    state.initialized = true;
                }
            });
            vertx.deployVerticle(raftPeer)
                .onSuccess((s) -> {
                    System.out.println(id + " | Successfully deployed raft peer id=" + id);
                    System.out.println(id + " | Waiting for the election...");
                });
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static void configureCommandSerializer() {
        DatabindCodec.mapper().registerModule(new SimpleModule() {{
            addSerializer(Command.class, Command.serializer);
        }});
        DatabindCodec.prettyMapper().registerModule(new SimpleModule() {{
            addSerializer(Command.class, Command.serializer);
        }});
    }

    private static <A extends Comparable<A>> boolean isListSorted(List<A> list) {
        if (list.size() == 2) {
            return list.get(0).compareTo(list.get(1)) <= 0;
        } else if (list.size() == 1 || list.size() == 0) {
            return true;
        } else {
            return list.get(0).compareTo(list.get(1)) <= 0 &&
                isListSorted(list.subList(1, list.size()));
        }
    }

    static class State {
        public boolean initialized = false;
    }
}

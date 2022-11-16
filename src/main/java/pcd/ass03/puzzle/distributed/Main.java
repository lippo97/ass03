package pcd.ass03.puzzle.distributed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import pcd.ass03.puzzle.distributed.commands.Command;
import pcd.ass03.puzzle.distributed.viewmodel.PuzzleBoard;
import pcd.ass03.raft.RaftPeer;

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
            final var controller = new Controller(raftPeer, board, parameters);
            vertx.deployVerticle(controller);
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
}

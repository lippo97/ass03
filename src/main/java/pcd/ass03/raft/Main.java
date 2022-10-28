package pcd.ass03.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        var members = Stream.iterate(0, x -> x + 1)
            .limit(4)
            .collect(Collectors.toMap(Function.identity(), Main::createMember));

        members.keySet().forEach((id) -> {
            var parameters = new Parameters(id, members);
            vertx.deployVerticle(new RaftClient<>(parameters));
        });
    }

    private static Member createMember(int id) {
        return new Member(id, "localhost", Utils.getPortForId(id));
    }
}

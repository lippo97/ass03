package pcd.ass03;

import io.vertx.core.Vertx;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
	public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        int leader = 0;
        List<Integer> peers = Stream.iterate(0, x -> x + 1)
          .limit(4)
          .collect(Collectors.toList());

        peers
          .stream()
          .map(n -> new RaftClient(n, leader, Collections.unmodifiableList(peers)))
          .forEach(vertx::deployVerticle);
    }
}

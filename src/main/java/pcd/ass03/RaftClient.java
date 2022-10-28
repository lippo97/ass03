package pcd.ass03;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.stream.Collectors;

public class RaftClient extends AbstractVerticle {

  private final int id;
  private final int leader;
  private final List<Integer> peers;
  private final int port;
  private int state;
  private HttpClient client;

  public RaftClient(final int id, final int leader, final List<Integer> peers) {
    this.id = id;
    this.leader = leader;
    this.peers = peers;
    this.port = 8080 + id;
    this.state = 0;
  }

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.client = vertx.createHttpClient();
    System.out.println("Deployed pcd.ass03.RaftClient " + id);
  }

  @Override
  public void start() throws Exception {
    super.start();
    System.out.println("Started pcd.ass03.RaftClient " + id);
    getVertx().createHttpServer()
      .requestHandler(makeRouter())
      .listen(port)
      .onSuccess(server ->
        System.out.println("pcd.ass03.RaftClient " + id + ": started HTTP server on port " + port)
      );
  }

  private Router makeRouter() {
    Router router = Router.router(this.getVertx());
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.POST, "/trigger").handler(ctx -> {
      List<Future> futures = peers.stream()
        .filter(x -> x != this.id)
        .map(id -> {
          int port = 8080 + id;
          System.out.println("Requesting towards: " + port);
          return client.request(HttpMethod.POST, port, "localhost", "/update")
            .flatMap(req -> req.end(Json.encode(new Update<>(0, 0, 3))));
        })
        .collect(Collectors.toList());
      CompositeFuture.all(futures)
        .onSuccess(x -> {
          ctx.response().end();
        })
        .onFailure(x -> {
          System.out.println(x);
          ctx.response().setStatusCode(500).end();
        });
    });

    router.route(HttpMethod.POST, "/update").handler(context -> {
      Update<Integer> update = context.body().asPojo(Update.class);

      updateStateAndPrintValue(update);
      context.response().end();
    });
    return router;
  }

  private boolean amILeader() {
    return id == leader;
  }

  private void updateStateAndPrintValue(Update<Integer> update) {
    state += update.value;
    System.out.println(state);
  }
}

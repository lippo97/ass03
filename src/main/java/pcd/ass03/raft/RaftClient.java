package pcd.ass03.raft;

import io.vertx.core.*;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.javatuples.Pair;
import pcd.ass03.raft.message.AppendEntriesRequest;
import pcd.ass03.raft.message.AppendEntriesResponse;
import pcd.ass03.raft.message.RequestVoteRequest;
import pcd.ass03.raft.message.RequestVoteResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RaftClient<S> extends AbstractVerticle {

    public static final int HEARTBEAT_TIMEOUT_LEADER = 3_000;
    public static final int HEARTBEAT_TIMEOUT_MIN = 10_000;
    public static final int HEARTBEAT_TIMEOUT_MAX = 30_000;
    private final Parameters parameters;
    private RequestManager requestManager;
    private Role currentRole;
    private int currentTerm;
    private int votedFor;
    private List<LogEntry<String>> log;
    private int logIndex;
    private int commitIndex;
    private int lastApplied;
    private int currentLeaderId;
    private Optional<Long> currentTimerId = Optional.empty();

    public RaftClient(final Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        this.log = new ArrayList<>();
        this.currentRole = Role.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = -1;
        this.logIndex = -1;
        this.commitIndex = -1;
        this.lastApplied = -1;
        this.currentLeaderId = -1;
        this.requestManager = new RequestManager(
            parameters.getId(),
            vertx.createHttpClient(
                new HttpClientOptions().setConnectTimeout(1000)
            )
        );
        resetTimer();
        log("Initialized...");
    }

    @Override
    public void start() throws Exception {
        super.start();
        final var server = getVertx().createHttpServer();
        server.requestHandler(makeRouter());
        server.listen(Utils.getPortForId(parameters.getId()));
    }

    private Router makeRouter() {
        final var router = Router.router(getVertx());
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/heartbeat").handler(ctx -> {
            if (currentRole == Role.FOLLOWER) {
                resetTimer();
            }
            ctx.end();
        });
        router.route(HttpMethod.POST, "/appendEntries").handler(ctx -> {
            final AppendEntriesRequest<String> req = ctx.body().asPojo(AppendEntriesRequest.class);
            if (req.term < currentTerm ||
                logTerm(req.prevLogIndex) != req.prevLogTerm) {
                log("Append entries failed");
                ctx.response().send(Json.encodeToBuffer(new AppendEntriesResponse(currentTerm, false)));
                return;
            }

            // Assuming here I have to become follower
            currentRole = Role.FOLLOWER;
            currentTerm = req.term;
            currentLeaderId = req.leaderId;

            if (req.entries.size() > 0) {
                final var previousSize = log.size();
                log.addAll(req.prevLogIndex + 1, req.entries);
                log = log.subList(0, previousSize);
                logIndex = log.size();
                commitIndex = Math.min(req.leaderCommit, logIndex);
            }
            // This has to be checked
            resetTimer();
            ctx.response().send(Json.encodeToBuffer(new AppendEntriesResponse(currentTerm, true)));
        });
        router.route(HttpMethod.POST, "/requestVote").handler(ctx -> {
            final var req = ctx.body().asPojo(RequestVoteRequest.class);
            if (req.term < currentTerm ||
                (req.term == currentTerm && votedFor != req.candidateId) ||
                (req.lastLogTerm < this.currentTerm) ||
                (req.lastLogTerm == this.currentTerm && req.lastLogIndex < logIndex)
            ) {
                var res = new RequestVoteResponse(false);
                ctx.response().send(Json.encodeToBuffer(res));
                return;
            }
            votedFor = req.candidateId;
            currentLeaderId = req.candidateId;
            currentTerm = req.term;
            currentRole = Role.FOLLOWER;
            final var res = new RequestVoteResponse(true);
            ctx.response().send(Json.encodeToBuffer(res));
            resetTimer();
            log("Following leader %d", currentLeaderId);
        });
        router.route(HttpMethod.POST, "/shutdown").handler(ctx -> {
            currentTimerId.ifPresent(getVertx()::cancelTimer);
            ctx.end();
        });
        router.route(HttpMethod.GET, "/dump").handler(ctx -> {
            final var entries = log.stream()
                .map((e) -> new JsonObject().put("term", e.term).put("value", e.value))
                .collect(Collectors.toList());
            final var array = new JsonArray(entries);
            ctx.response().send(
                new JsonObject()
                    .put("log", array)
                    .toBuffer()
            );
        });
        router.route(HttpMethod.POST, "/pushLogEntry").handler(ctx -> {
            log.add(new LogEntry<>(currentTerm, "mock"));
            logIndex++;
            ctx.end();
        });
        return router;
    }

    private void resetTimer() {
        currentTimerId.ifPresent(getVertx()::cancelTimer);
        log("Resetting timer.");
        final var nextTimer = ThreadLocalRandom.current()
            .nextInt(HEARTBEAT_TIMEOUT_MIN, HEARTBEAT_TIMEOUT_MAX + 1);
        currentTimerId = Optional.of(getVertx().setTimer(nextTimer, this::handleTimeout));
    }

    private void handleTimeout(long timerId) {
        log("Timer expired.");
        this.currentRole = Role.CANDIDATE;
        this.currentTerm = this.currentTerm + 1;
        final var futures = getOtherPeers().stream()
            .map(member -> requestManager.requestVote(member, new RequestVoteRequest(
                currentTerm,
                parameters.getId(),
                logIndex,
                logTerm(logIndex)
            ))
                .map(f -> new Pair<>(member.getId(), f)))
            .collect(Collectors.toList());
        final var votes = parameters.getMembers().keySet().stream()
            .collect(Collectors.toMap(Function.identity(), (id) -> id == parameters.getId()));
        CompositeFuture.join(Collections.unmodifiableList(futures))
            .onComplete((agg) -> {
                if (agg.succeeded()) {
                    agg.result().<Pair<Integer, RequestVoteResponse>>list().forEach(res -> {
                        if (res != null) {
                            var id = res.getValue0();
                            var response = res.getValue1();
                            votes.put(id, response.accepted);
                        }
                    });
                    var countVotes = votes.values().stream().filter(x -> x).count();
                    if (isMajority(countVotes)) {
                        becomeLeader();
                        log("I'm the leader.");
                    } else {
                        log("Votes weren't enough.");
                        resetTimer();
                    }
                } else {
                    log("On complete failed: %s", agg.cause().toString());
                }
            });
    }

    private void becomeLeader() {
        currentLeaderId = parameters.getId();
        currentRole = Role.LEADER;
        final var timerId = getVertx().setPeriodic(HEARTBEAT_TIMEOUT_LEADER, (id) -> {
            var futures = getOtherPeers().stream()
                .map(m -> requestManager.appendEntries(m, createAppendEntriesRequest()))
                .collect(Collectors.toList());
            CompositeFuture.join(Collections.unmodifiableList(futures))
                .onComplete(res -> {
                    log("Sent heartbeats to everyone.");
                });
        });
        currentTimerId = Optional.of(timerId);
    }

    private List<Member> getOtherPeers() {
        return parameters.getMembers().values().stream()
            .filter(m -> m.getId() != parameters.getId())
            .collect(Collectors.toList());
    }

    private boolean isMajority(long count) {
        final var members = parameters.getMembers().keySet().size();
        return count > (members / 2 + members % 2);
    }

    private int logTerm(int logIndex) {
        return logIndex < 0 ? 0 : log.get(logIndex).term;
    }

    private AppendEntriesRequest<String> createAppendEntriesRequest() {
//        List<LogEntry<String>> entries = new ArrayList<>();
        final var entries = log;
        return new AppendEntriesRequest<>(
            currentTerm,
            parameters.getId(),
            logIndex,
            logTerm(logIndex),
            entries,
            commitIndex
        );
    }
//    private AppendEntriesRequest createAppendEntriesRequest(List<LogEntry<Integer>> entries) {
//        return new AppendEntriesRequest(
//            currentTerm,
//            parameters.getId(),
//            prevLogIndex,
//            prevLogTerm,
//            entries,
//            leaderCommit
//        )));
//    }

    private void log(String format, Object... args) {
        var role = currentRole.toString();
        role = String.format(
            "%s%s",
            currentRole.toString(),
            currentRole == Role.FOLLOWER ? String.format("(leader=%d)", currentLeaderId) : ""
        );
        var description = String.format("Raft(id=%d,term=%d,role=%s) | ", parameters.getId(), currentTerm, role);
        System.out.printf(description + format + "\n", args);
    }

}

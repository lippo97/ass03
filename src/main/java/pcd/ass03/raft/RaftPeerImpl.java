package pcd.ass03.raft;

import io.netty.channel.AbstractChannel;
import io.vertx.core.*;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.javatuples.Pair;
import pcd.ass03.raft.message.*;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RaftPeerImpl<A> extends AbstractVerticle implements RaftPeer<A> {
    private static final boolean DEBUG = true;
    public static final int PROACTIVE_BEHAVIOR_TIMEOUT = 50;
    public static final int HEARTBEAT_TIMEOUT_LEADER = 25;
    public static final int HEARTBEAT_TIMEOUT_MIN = 150;
    public static final int HEARTBEAT_TIMEOUT_MAX = 300;
    private final Parameters parameters;
    private RequestManager requestManager;
    private Role currentRole;
    private int currentTerm;
    private Map<Integer, Integer> votedFor;
    private List<LogEntry<A>> log;
    private int commitIndex;
    private int lastApplied;
    private int currentLeaderId;
    private Map<Integer, Integer> nextIndex;
    private Map<Integer, Integer> matchIndex;
    private Handler<Integer> _onBecomeLeader;
    private Handler<A> _onApplyEntry;
    private Optional<Long> behaviorTimerId;
    private Optional<Long> followerBehaviorTimerId;
    private Optional<Long> leaderElectionTimerId;

    public RaftPeerImpl(final Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        this.log = new ArrayList<>();
        this.log.add(new LogEntry<>(0, null));
        this.currentRole = Role.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = new HashMap<>() {{
            put(0, -1);
        }};
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.currentLeaderId = -1;
        this.behaviorTimerId = Optional.of(vertx.setPeriodic(PROACTIVE_BEHAVIOR_TIMEOUT, (_id) -> {
            for (int i = lastApplied + 1; i <= commitIndex; i++) {
                final var value = log.get(i).value;
                log("applying %s", value);
                if (_onApplyEntry != null) {
                    _onApplyEntry.handle(value);
                }
            }
            lastApplied = commitIndex;
        }));
        this.followerBehaviorTimerId = Optional.empty();
        this.leaderElectionTimerId = Optional.empty();
        lastApplied = commitIndex;
        this.requestManager = new RequestManager(
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

    @Override
    public void stop() throws Exception {
        super.stop();
        behaviorTimerId.ifPresent(getVertx()::cancelTimer);
        behaviorTimerId = Optional.empty();
        followerBehaviorTimerId.ifPresent(getVertx()::cancelTimer);
        followerBehaviorTimerId = Optional.empty();
        leaderElectionTimerId.ifPresent(getVertx()::cancelTimer);
        leaderElectionTimerId = Optional.empty();
    }

    private Router makeRouter() {
        final var router = Router.router(getVertx());
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/appendEntries").handler(ctx -> {
            final AppendEntriesRequest<A> req = ctx.body().asPojo(AppendEntriesRequest.class);
            if (req.term < currentTerm ||
                (req.entries.size() > 0 &&
                    logTerm(req.prevLogIndex).stream()
                        .boxed()
                        .allMatch(Predicate.not(Predicate.isEqual(req.prevLogTerm)))
                )) {
                log("Append entries failed");
                resetTimer();
                ctx.response().send(Json.encodeToBuffer(new AppendEntriesResponse(currentTerm, false)));
                return;
            }

            // If someone is sending heartbeats that pass the previous guard,
            // then it must be the current leader.
            currentRole = Role.FOLLOWER;
            currentTerm = req.term;
            currentLeaderId = req.leaderId;
            commitIndex = Math.min(req.leaderCommit, lastLogIndex());

            if (req.entries.size() > 0) {
                final var finalSize = req.prevLogIndex + req.entries.size() + 1;
                log.addAll(req.prevLogIndex + 1, req.entries);
                log = log.subList(0, finalSize);
            }
            resetTimer();
            ctx.response().send(Json.encodeToBuffer(new AppendEntriesResponse(currentTerm, true)));
        });
        router.route(HttpMethod.POST, "/requestVote").handler(ctx -> {
            final var req = ctx.body().asPojo(RequestVoteRequest.class);
            if (req.term < currentTerm ||
                (req.term == currentTerm && currentRole == Role.CANDIDATE) ||
                (req.term == currentTerm && votedFor.containsKey(req.term) && votedFor.get(req.term) != req.candidateId) ||
                !isLogUpToDate(req)
            ) {
                var res = new RequestVoteResponse(currentTerm, false);
                ctx.response().send(Json.encodeToBuffer(res));
                return;
            }
            votedFor.put(req.term, req.candidateId);
            currentLeaderId = req.candidateId;
            currentTerm = req.term;
            currentRole = Role.FOLLOWER;
            final var res = new RequestVoteResponse(currentTerm,true);
            ctx.response().send(Json.encodeToBuffer(res));
            resetTimer();
            log("Following leader %d", currentLeaderId);
        });
        router.route(HttpMethod.POST, "/shutdown").handler(ctx -> {
            leaderElectionTimerId.ifPresent(getVertx()::cancelTimer);
            ctx.end();
        });
        router.route(HttpMethod.GET, "/dump").handler(ctx -> {
            final var entries = log.stream()
                .map((e) -> new JsonObject()
                    .put("term", e.term)
                    .put("value", e.value))
                .collect(Collectors.toList());
            final var array = new JsonArray(entries);
            ctx.response().send(
                new JsonObject()
                    .put("logIndex", lastLogIndex())
                    .put("commitIndex", commitIndex)
                    .put("lastApplied", lastApplied)
                    .put("log", array)
                    .toBuffer()
            );
        });
        router.route(HttpMethod.POST, "/newLogEntry").handler(ctx -> {
            if (currentRole != Role.LEADER) {
                ctx.response().setStatusCode(405);
                return;
            }
            final var req = (NewLogEntryRequest<A>) ctx.body().asPojo(NewLogEntryRequest.class);
            final var entries = req.entries.stream()
                .map(e -> new LogEntry<>(currentTerm, e))
                .collect(Collectors.toList());
            log.addAll(entries);
            ctx.end();
        });
        return router;
    }

    private void resetTimer() {
        leaderElectionTimerId.ifPresent(getVertx()::cancelTimer);
        log("Resetting timer.");
        final var nextTimer = ThreadLocalRandom.current()
            .nextInt(HEARTBEAT_TIMEOUT_MIN, HEARTBEAT_TIMEOUT_MAX + 1);
        leaderElectionTimerId = Optional.of(getVertx().setTimer(nextTimer, this::handleTimeout));
    }

    private void handleTimeout(long timerId) {
        log("Timer expired.");
        this.currentRole = Role.CANDIDATE;
        this.currentTerm = this.currentTerm + 1;
        final var futures = getOtherPeers().stream()
            .map(member ->
                requestManager.requestVote(member, new RequestVoteRequest(
                    currentTerm,
                    parameters.getId(),
                    lastLogIndex(),
                    logTerm(lastLogIndex()).orElse(0) // unsafe
                ))
                    .recover(throwable -> {
                        if (throwable instanceof IOException) {
                            return Future.succeededFuture();
                        }
                        return Future.failedFuture(throwable);
                    })
            .map(f -> new Pair<>(member.getId(), f)))
            .collect(Collectors.toList());
        final var votes = parameters.getMembers().keySet().stream()
            .collect(Collectors.toMap(Function.identity(), (id) -> id == parameters.getId()));
        CompositeFuture.join(Collections.unmodifiableList(futures))
            .onComplete((agg) -> {
                if (agg.succeeded()) {
                    final var results= agg.result().<Pair<Integer, RequestVoteResponse>>list();
                    final var greaterTerm = results.stream()
                        .filter(t -> t.getValue1() != null && t.getValue1().term > currentTerm)
                        .findAny();
                    if (greaterTerm.isPresent()) {
                        currentLeaderId = greaterTerm.get().getValue0();
                        currentTerm = greaterTerm.get().getValue1().term;
                        currentRole = Role.FOLLOWER;
                        return;
                    }
                    results.forEach(res -> {
                        if (res != null && res.getValue1() != null) {
                            var id = res.getValue0();
                            var response = res.getValue1();
                            votes.put(id, response.accepted);
                        }
                    });
                    final var countVotes = votes.values().stream()
                        .filter(x -> x)
                        .count();
                    if (isMajority(countVotes)) {
                        becomeLeader();
                        log("I'm the leader with %d out of %d votes.", countVotes, parameters.getMembers().size());
                    } else {
                        log("Votes weren't enough.");
                        resetTimer();
                    }
                    return;
                }
                log("On complete failed: %s", agg.cause().toString());
                resetTimer();
            });
    }

    private void becomeLeader() {
        // Ensure no current timeout is running.
        leaderElectionTimerId.ifPresent(getVertx()::cancelTimer);
        currentLeaderId = parameters.getId();
        currentRole = Role.LEADER;
        initializeNextIndex();
        initializeMatchIndex();
        if (_onBecomeLeader != null) {
            _onBecomeLeader.handle(parameters.getId());
        }
        final var timerId = getVertx().setPeriodic(HEARTBEAT_TIMEOUT_LEADER, this::proactiveLeaderBehavior);
        leaderElectionTimerId = Optional.of(timerId);
    }

    private void initializeNextIndex() {
        this.nextIndex = parameters.getMembers().entrySet().stream()
            .filter(e -> e.getKey() != parameters.getId())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> lastLogIndex() + 1));
    }

    private void initializeMatchIndex() {
        this.matchIndex = parameters.getMembers().entrySet().stream()
            .filter(e -> e.getKey() != parameters.getId())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> 0));
    }

    private void proactiveLeaderBehavior(long timerId) {
        final var futures = nextIndex.keySet().stream()
            .map(this::createAppendEntriesRequest)
            .collect(Collectors.toList());
        CompositeFuture.join(Collections.unmodifiableList(futures))
            .onComplete(res -> log("Sent heartbeats to everyone."));

        updateCommitIndex();
    }

    private void updateCommitIndex() {
        int n = commitIndex + 1;
        /*
         * Move commitIndex to the right one step at time.
         * As soon as it finds different terms or the majority of the peers don't have replicated that index yet,
         * it stops.
         */
        while (n <= lastLogIndex()) {
            final int finalN = n;
            if (log.get(n).term != currentTerm ||
                !isMajority(matchIndex.values().stream().filter(x -> x >= finalN).count())) {
                break;
            }
            commitIndex = n;
            n = n + 1;
        }
    }

    private boolean isLogUpToDate(RequestVoteRequest req) {
        return logTerm(req.lastLogIndex).stream()
            .allMatch(term ->
                (req.lastLogTerm > term) ||
                    (req.lastLogTerm == term && req.lastLogIndex >= lastLogIndex()));
    }


    private List<Member> getOtherPeers() {
        return parameters.getMembers().values().stream()
            .filter(m -> m.getId() != parameters.getId())
            .collect(Collectors.toList());
    }

    private int lastLogIndex() {
        return log.size() - 1;
    }

    private boolean isMajority(long count) {
        final var members = parameters.getMembers().keySet().size();
        return count > (members / 2 + members % 2);
    }

    private OptionalInt logTerm(int logIndex) {
        return logIndex < 0 || logIndex >= log.size() ? OptionalInt.empty() : OptionalInt.of(log.get(logIndex).term);
    }

    private Future<AppendEntriesResponse> createAppendEntriesRequest(int memberId) {
        final var _lastLogIndex = lastLogIndex();
        final var nextIndexI = nextIndex.get(memberId);

        if (_lastLogIndex >= nextIndexI) {
            final var appendBody = log.subList(nextIndexI, _lastLogIndex + 1);
            final var req = new AppendEntriesRequest<>(
                currentTerm,
                parameters.getId(),
                nextIndexI - 1, // logIndex,
                logTerm(nextIndexI - 1).getAsInt(), // unsafe
                appendBody,
                commitIndex
            );
            return requestManager.appendEntries(parameters.getMembers().get(memberId), req)
                .onSuccess(res -> {
                    if (res.success) {
                        nextIndex.put(memberId, _lastLogIndex);
                        matchIndex.put(memberId, _lastLogIndex);
                    } else {
                        nextIndex.put(memberId, nextIndexI - 1);
                    }
                });
        }
        final var req = new AppendEntriesRequest<>(
            currentTerm,
            parameters.getId(),
            -1,
            -1,
            List.of(),
            commitIndex
        );
        return requestManager.appendEntries(parameters.getMembers().get(memberId), req);
    }

    private void log(String format, Object... args) {
        if (!DEBUG) {
            return;
        }
        var role = currentRole.toString();
        role = String.format(
            "%s%s",
            currentRole.toString(),
            currentRole == Role.FOLLOWER ? String.format("(leader=%d)", currentLeaderId) : ""
        );
        var description = String.format("Raft(id=%d,term=%d,role=%s) | ", parameters.getId(), currentTerm, role);
        System.out.printf(description + format + "\n", args);
    }

    @Override
    public void onBecomeLeader(Handler<Integer> handler) {
        _onBecomeLeader = handler;
    }

    @Override
    public void onApplyLogEntry(Handler<A> handler) {
        _onApplyEntry = handler;
    }

    @Override
    public Future<Void> pushLogEntries(List<A> entries) {
        if (parameters.getId() == currentLeaderId) {
            final var logEntries = entries.stream()
                .map(e -> new LogEntry<>(currentTerm, e))
                .collect(Collectors.toList());
            this.log.addAll(logEntries);
            return Future.succeededFuture();
        }
        final var req = new NewLogEntryRequest<>(parameters.getId(), entries);
        return requestManager.newLogEntry(parameters.getMembers().get(currentLeaderId), req);
    }
}

package pcd.ass03.raft;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.handler.HttpException;
import pcd.ass03.raft.message.*;

public class RequestManager {
    private final HttpClient httpClient;

    public RequestManager(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Future<Void> sendHeartbeat(Member member) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/heartbeat")
                .flatMap(HttpClientRequest::end);
    }

    public Future<RequestVoteResponse> requestVote(Member member, RequestVoteRequest body) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/requestVote")
            .flatMap(req -> req.send(Json.encode(body)))
            .flatMap(HttpClientResponse::body)
            .map(buffer -> Json.decodeValue(buffer, RequestVoteResponse.class));
    }

    public <A> Future<AppendEntriesResponse> appendEntries(Member member, AppendEntriesRequest<A> body) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/appendEntries")
            .flatMap(req -> req.send(Json.encodeToBuffer(body)))
            .flatMap(HttpClientResponse::body)
            .map(buffer -> Json.decodeValue(buffer, AppendEntriesResponse.class));
    }

    public <A> Future<Void> newLogEntry(Member member, NewLogEntryRequest<A> body) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/newLogEntry")
            .flatMap(req -> req.send(Json.encodeToBuffer(body)))
            .flatMap(res -> {
                if (res.statusCode() != 200) {
                    return Future.failedFuture(new HttpException(res.statusCode()));
                }
                return Future.succeededFuture();
           });
    }
}

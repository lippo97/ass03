package pcd.ass03.raft;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import pcd.ass03.raft.message.*;

public class RequestManager {

    private final int id;
    private final HttpClient httpClient;

    public RequestManager(int id, HttpClient httpClient) {
        this.id = id;
        this.httpClient = httpClient;
    }

    public Future<Void> sendHeartbeat(Member member) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/heartbeat")
                .flatMap(HttpClientRequest::end);
    }

//    public Future<RequestVoteResponse> requestVote(int term, Member member) {
//        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/requestVote")
//            .flatMap(req -> req.send(Json.encode(new RequestVoteRequest(term, this.id))))
//            .flatMap(HttpClientResponse::body)
//            .map(buffer -> Json.decodeValue(buffer, RequestVoteResponse.class));
//    }

    public Future<RequestVoteResponse> requestVote(Member member, RequestVoteRequest body) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/requestVote")
            .flatMap(req -> req.send(Json.encode(body)))
            .flatMap(HttpClientResponse::body)
            .map(buffer -> Json.decodeValue(buffer, RequestVoteResponse.class));
    }

    public Future<Void> sendHeartbeat(int leaderId, Member member) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/heartbeat")
            .flatMap(req -> req.send(Json.encodeToBuffer(new SendHeartbeatRequest(leaderId))))
            .mapEmpty();
    }

    public Future<AppendEntriesResponse> appendEntries(Member member, AppendEntriesRequest body) {
        return httpClient.request(HttpMethod.POST, member.getPort(), member.getHostname(), "/appendEntries")
            .flatMap(req -> req.send(Json.encodeToBuffer(body)))
            .flatMap(HttpClientResponse::body)
            .map(buffer -> Json.decodeValue(buffer, AppendEntriesResponse.class));

    }
}

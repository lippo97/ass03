package pcd.ass03.puzzle.distributed;

import io.vertx.core.Vertx;
import org.javatuples.Pair;
import pcd.ass03.raft.Member;
import pcd.ass03.raft.Parameters;
import pcd.ass03.raft.RaftPeer;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            final var parameters = parseArgs(args);
            final var vertx = Vertx.vertx();
            final var raftPeer = RaftPeer.make(parameters);
            vertx.deployVerticle(raftPeer);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static Parameters parseArgs(String[] args) throws IllegalArgumentException {
        var id = Optional.<Integer>empty();
        var members = Optional.<Map<Integer, Member>>empty();
        var argList = List.of(args);
        try {
            while (argList.size() > 0) {
                if (argList.get(0).equals("id")) {
                    id = Optional.of(Integer.parseInt(argList.get(1)));
                    argList = argList.subList(2, argList.size());
                } else if (argList.get(0).equals("members")) {
                    members = Optional.of(parseMap(argList.get(1)));
                    argList = argList.subList(2, argList.size());
                } else {
                    argList = argList.subList(1, argList.size());
                }
            }
            return new Parameters(id.get(), members.get());
        } catch(IndexOutOfBoundsException | NoSuchElementException ex) {
            throw new IllegalArgumentException("You must provide the following arguments:" +
                "id <PEER_ID> members <PEER_0;...;PEER_N>");
        }
    }

    private static Map<Integer, Member> parseMap(String in) {
        return Arrays.stream(in.split(";"))
            .map(Main::parseMember)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<Integer, Member> parseMember(String in) {
        final var tokens = in.split(":");
        final var id = Integer.parseInt(tokens[0]);
        final var hostname = tokens[1];
        final var port = Integer.parseInt(tokens[2]);
        return new AbstractMap.SimpleImmutableEntry<>(id, new Member(id, hostname, port));
    }
}

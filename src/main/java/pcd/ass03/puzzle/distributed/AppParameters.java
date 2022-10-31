package pcd.ass03.puzzle.distributed;

import pcd.ass03.raft.Member;
import pcd.ass03.raft.RaftParameters;

import java.util.*;
import java.util.stream.Collectors;

public class AppParameters {
    public RaftParameters raftParameters;
    public String imagePath;
    public int rows;
    public int columns;

    AppParameters() { }

    public boolean checkValidity() {
        return raftParameters != null && !imagePath.equals("") && rows != 0 && columns != 0;
    }

    static AppParameters parse(String[] args) throws IllegalArgumentException {
        var id = Optional.<Integer>empty();
        var members = Optional.<Map<Integer, Member>>empty();
        final var appParameters = new AppParameters();

        var argList = List.of(args);
        try {
            while (argList.size() > 0) {
                switch (argList.get(0)) {
                    case "id":
                        id = Optional.of(Integer.parseInt(argList.get(1)));
                        argList = argList.subList(2, argList.size());
                        break;
                    case "members":
                        members = Optional.of(parseMap(argList.get(1)));
                        argList = argList.subList(2, argList.size());
                        break;
                    case "imagePath":
                        appParameters.imagePath = argList.get(1);
                        argList = argList.subList(2, argList.size());
                        break;
                    case "columns":
                        appParameters.columns = Integer.parseInt(argList.get(1));
                        argList = argList.subList(2, argList.size());
                        break;
                    case "rows":
                        appParameters.rows = Integer.parseInt(argList.get(1));
                        argList = argList.subList(2, argList.size());
                        break;
                    default:
                        argList = argList.subList(1, argList.size());
                        break;
                }
            }
            appParameters.raftParameters = new RaftParameters(id.get(), members.get());
            if (!appParameters.checkValidity()) {
                throw new IllegalArgumentException("You must provide the following arguments:\n" +
                    "id <PEER_ID> members <PEER_0;...;PEER_N>");
            }
            return appParameters;
        } catch (IndexOutOfBoundsException | NoSuchElementException ex) {
            throw new IllegalArgumentException("You must provide the following arguments:\n" +
                "id <PEER_ID> members <PEER_0;...;PEER_N>");
        }
    }

    private static Map<Integer, Member> parseMap(String in) {
        return Arrays.stream(in.split(";"))
            .map(AppParameters::parseMember)
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

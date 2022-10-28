package pcd.ass03.raft;

public class Member {
    private final int id;
    private final String hostname;
    private final int port;

    public Member(int id, String hostname, int port) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}

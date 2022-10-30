package pcd.ass03.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExampleMessage {
    public final int value;

    @JsonCreator
    public ExampleMessage(@JsonProperty("value") int value) {
        this.value = value;
    }
}

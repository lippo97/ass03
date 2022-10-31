package pcd.ass03.puzzle.distributed.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("click")
public class Click extends Command {
    public final int position;

    @JsonCreator
    public Click(@JsonProperty("position") int position) {
        this.position = position;
    }
}

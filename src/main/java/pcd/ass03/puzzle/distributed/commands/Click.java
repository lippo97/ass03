package pcd.ass03.puzzle.distributed.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("click")
public class Click extends Command {
    public final int sourceId;
    public final int position;

    @JsonCreator
    public Click(
        @JsonProperty("sourceId") int sourceId,
        @JsonProperty("position") int position
    ) {
        this.sourceId = sourceId;
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("Click(sourceId=%d,position=%d)", sourceId, position);
    }
}

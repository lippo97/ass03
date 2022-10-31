package pcd.ass03.puzzle.distributed.commands;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

@JsonTypeName("initialize")
public class Initialize extends Command {
    public final List<Integer> positions;

    @JsonCreator
    public Initialize(@JsonProperty("positions") List<Integer> positions) {
        this.positions = positions;
    }

    @Override
    public String toString() {
        final var pos = positions.size() > 0 ?
            positions.stream()
                .map(Object::toString)
                .reduce("", (x, y) -> x + "," + y)
                .replaceFirst(",", "[") + "]"
            : "[]";

        return String.format("Initialize(positions=%s)", pos);
    }
}

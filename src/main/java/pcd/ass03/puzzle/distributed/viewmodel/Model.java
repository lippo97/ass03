package pcd.ass03.puzzle.distributed.viewmodel;

import pcd.ass03.puzzle.distributed.commands.Click;

import java.util.List;

public interface Model {
    void createTiles(List<Integer> randomPositions);
    void handleClick(Click click);
}

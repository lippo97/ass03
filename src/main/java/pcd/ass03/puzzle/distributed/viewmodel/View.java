package pcd.ass03.puzzle.distributed.viewmodel;

import pcd.ass03.puzzle.concentrated.Tile;

public interface View {
    void onTileClick(Listener onClick);

    void setVisible(boolean visible);

    @FunctionalInterface
    interface Listener {
        void handle(Tile tile);
    }
}

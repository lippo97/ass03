package pcd.ass03.puzzle.distributed;

import pcd.ass03.puzzle.concentrated.Tile;

import java.util.HashMap;
import java.util.Map;

public class SelectionManager {
	private Map<Integer, Tile> selectedTiles = new HashMap<>();

	public void selectTile(final int id, final Tile tile, final Listener listener) {
        final var entries = selectedTiles.entrySet();

        if (isSelectedBySomeoneElse(id, tile)) {
            System.out.println("Tile was already selected by someone else.");
            return;
        }
        if (selectedTiles.containsKey(id)) {
            final var selected = selectedTiles.get(id);
            if (selected != tile) {
                swap(selected, tile);
            }
            selectedTiles.remove(id);
            selected.setOwnerId(-1);
            listener.onSwapPerformed();
            return;
        }
        selectedTiles.put(id, tile);
        tile.setOwnerId(id);
        listener.onSwapPerformed();
    }

	private void swap(final Tile t1, final Tile t2) {
		int pos = t1.getCurrentPosition();
		t1.setCurrentPosition(t2.getCurrentPosition());
		t2.setCurrentPosition(pos);
	}

    private boolean isSelectedBySomeoneElse(final int id, final Tile tile) {
        return selectedTiles.entrySet().stream()
            .anyMatch(e -> e.getKey() != id && e.getValue() == tile);
    }

	@FunctionalInterface
	public interface Listener {
		void onSwapPerformed();
	}
}

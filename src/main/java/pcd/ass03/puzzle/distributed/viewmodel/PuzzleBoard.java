package pcd.ass03.puzzle.distributed.viewmodel;

import pcd.ass03.puzzle.concentrated.Tile;
import pcd.ass03.puzzle.concentrated.TileButton;
import pcd.ass03.puzzle.distributed.SelectionManager;
import pcd.ass03.puzzle.distributed.commands.Click;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public class PuzzleBoard extends JFrame implements ViewModel {

    private final String title;
    private final int id;
	private final int rows, columns;
    private final String imagePath;
	private List<Tile> tiles = new ArrayList<>();
    private final JPanel board = new JPanel();
    private Listener _onTileClick;

	private SelectionManager selectionManager = new SelectionManager();

    public PuzzleBoard(final String title, final int id, final int rows, final int columns, final String imagePath) {
        this.title = title;
        this.id = id;
    	this.rows = rows;
		this.columns = columns;
        this.imagePath = imagePath;

    	setTitle("Puzzle - " + title);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(rows, columns, 0, 0));
        getContentPane().add(board, BorderLayout.CENTER);
    }

    @Override
    public void createTiles(List<Integer> randomPositions) {
		final BufferedImage image;

        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load image", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int imageWidth = image.getWidth(null);
        final int imageHeight = image.getHeight(null);

        int position = 0;
        tiles.clear();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
            	final Image imagePortion = createImage(new FilteredImageSource(image.getSource(),
                        new CropImageFilter(j * imageWidth / columns,
                        					i * imageHeight / rows,
                        					(imageWidth / columns),
                        					imageHeight / rows)));

                tiles.add(new Tile(imagePortion, position, randomPositions.get(position)));
                position++;
            }
        }
        try {
            System.out.printf("%d | Thread: %s, calling invokeAndWait\n", id, Thread.currentThread().getName());
            SwingUtilities.invokeAndWait(this::paintPuzzle);
        } catch (InterruptedException | InvocationTargetException e) {
            System.err.println(id + " | Error while updating the GUI.");
        }
    }

    @Override
    public void handleClick(Click click) {
        final var tile = tiles.stream()
            .filter(t -> t.getCurrentPosition() == click.position)
            .findFirst()
            .get();
        selectionManager.selectTile(click.sourceId, tile, () -> {
            try {
                long start = System.currentTimeMillis();
                SwingUtilities.invokeAndWait(this::paintPuzzle);
                long finish = System.currentTimeMillis();
                long timeElapsed = finish - start;
                System.out.println(id + " | Time elapsed painting: " + timeElapsed);
            } catch (InvocationTargetException | InterruptedException e) {
                System.err.println(id + " | Error while updating the GUI.");
            } finally {
                checkSolution();
            }
        });
    }

    @Override
    public void onTileClick(Listener onClick) {
        this._onTileClick = onClick;
    }

    private void paintPuzzle() {
        board.removeAll();

        Collections.sort(tiles);

        tiles.forEach(tile -> {
            final TileButton btn = new TileButton(tile);
            final var color = tile.getOwnerId() == -1 ? Color.gray :
                tile.getOwnerId() == id ? Color.red :
                    Color.blue;
            btn.setBorder(BorderFactory.createLineBorder(color));
            btn.addActionListener(actionListener -> {
                _onTileClick.handle(tile);
            });
            board.add(btn);
        });

        pack();
    }

    private void checkSolution() {
        if (tiles.stream().allMatch(Tile::isInRightPlace)) {
            new Thread(() -> JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE))
                .start();
        }
    }

}

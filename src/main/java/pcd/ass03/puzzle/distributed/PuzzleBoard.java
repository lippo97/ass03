package pcd.ass03.puzzle.distributed;

import pcd.ass03.puzzle.concentrated.SelectionManager;
import pcd.ass03.puzzle.concentrated.Tile;
import pcd.ass03.puzzle.concentrated.TileButton;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings("serial")
public class PuzzleBoard extends JFrame {

	private final int rows, columns;
    private final String imagePath;
	private List<Tile> tiles = new ArrayList<>();
    private final JPanel board = new JPanel();

	private SelectionManager selectionManager = new SelectionManager();

    public PuzzleBoard(final int rows, final int columns, final String imagePath) {
    	this.rows = rows;
		this.columns = columns;
        this.imagePath = imagePath;

    	setTitle("Puzzle");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(rows, columns, 0, 0));
        getContentPane().add(board, BorderLayout.CENTER);

//        createTiles(imagePath);
//        paintPuzzle(board);
    }

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
        paintPuzzle();
    }

    private void paintPuzzle() {
    	board.removeAll();

    	Collections.sort(tiles);

    	tiles.forEach(tile -> {
    		final TileButton btn = new TileButton(tile);
            board.add(btn);
            btn.setBorder(BorderFactory.createLineBorder(Color.gray));
            btn.addActionListener(actionListener -> {
            	selectionManager.selectTile(tile, () -> {
            		paintPuzzle();
                	checkSolution();
            	});
            });
    	});

    	pack();
        setLocationRelativeTo(null);
    }

    private void checkSolution() {
    	if(tiles.stream().allMatch(Tile::isInRightPlace)) {
    		JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE);
    	}
    }
}

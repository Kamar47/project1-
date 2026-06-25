package gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Utility class for generating a QR-like visual code for order confirmation codes.
 * <p>
 * The generated pattern is deterministic and is based on the confirmation code text.
 * This class creates a display-only simulation and does not generate a real scannable QR code.
 * </p>
 */
public class QRCodeGenerator {

    private static final int SIZE = 150;
    private static final int MODULES = 21; // 21x21 grid like QR version 1
    private static final int MODULE_SIZE = SIZE / MODULES;

    /**
     * Generates a QR-like canvas from a confirmation code string.
     * The same confirmation code always produces the same visual pattern.
     *
     * @param text the confirmation code text used to generate the pattern
     * @param pixelSize the requested size of the generated canvas in pixels
     * @return a JavaFX canvas containing the generated QR-like pattern
     */
    public static Canvas generateQR(String text, int pixelSize) {
        int mod = MODULES;
        int cell = pixelSize / mod;
        Canvas canvas = new Canvas(cell * mod, cell * mod);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Background
        gc.setFill(Color.web("#ffffff"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        boolean[][] grid = buildGrid(text, mod);

        gc.setFill(Color.web("#12121f"));
        for (int r = 0; r < mod; r++) {
            for (int c = 0; c < mod; c++) {
                if (grid[r][c]) {
                    gc.fillRect(c * cell, r * cell, cell, cell);
                }
            }
        }

        // Draw border
        gc.setStroke(Color.web("#34d399"));
        gc.setLineWidth(2);
        gc.strokeRect(1, 1, canvas.getWidth() - 2, canvas.getHeight() - 2);

        return canvas;
    }

    /**
     * Builds the boolean grid used for drawing the QR-like pattern.
     * Finder patterns are added in three corners, and the remaining cells are filled
     * using a deterministic random pattern based on the input text.
     *
     * @param text the confirmation code text used to generate the grid
     * @param mod the number of modules in each row and column
     * @return a two-dimensional boolean grid representing filled and empty cells
     */
    private static boolean[][] buildGrid(String text, int mod) {
        boolean[][] grid = new boolean[mod][mod];

        // Finder patterns (3 corners — top-left, top-right, bottom-left)
        addFinderPattern(grid, 0, 0, mod);
        addFinderPattern(grid, 0, mod - 7, mod);
        addFinderPattern(grid, mod - 7, 0, mod);

        // Data region — deterministic from text hash
        int hash = 0;
        for (char ch : text.toCharArray()) hash = hash * 31 + ch;

        java.util.Random rand = new java.util.Random(hash);
        for (int r = 0; r < mod; r++) {
            for (int c = 0; c < mod; c++) {
                if (!isFinderZone(r, c, mod)) {
                    grid[r][c] = rand.nextBoolean();
                }
            }
        }

        return grid;
    }

    /**
     * Adds a 7x7 finder pattern to the given grid.
     * Finder patterns are used to visually imitate the corner markers of a QR code.
     *
     * @param grid the QR-like grid to update
     * @param row the starting row of the finder pattern
     * @param col the starting column of the finder pattern
     * @param mod the size of the grid
     */
    private static void addFinderPattern(boolean[][] grid, int row, int col, int mod) {
        // 7x7 finder pattern
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                int ar = row + r, ac = col + c;
                if (ar < mod && ac < mod) {
                    boolean outer = (r == 0 || r == 6 || c == 0 || c == 6);
                    boolean inner = (r >= 2 && r <= 4 && c >= 2 && c <= 4);
                    grid[ar][ac] = outer || inner;
                }
            }
        }
    }

    /**
     * Checks whether a grid cell is located inside one of the finder pattern zones.
     * These zones are skipped when filling the data region of the QR-like pattern.
     *
     * @param r the row index of the cell
     * @param c the column index of the cell
     * @param mod the size of the grid
     * @return true if the cell is inside a finder pattern zone, otherwise false
     */
    private static boolean isFinderZone(int r, int c, int mod) {
        // Top-left
        if (r < 8 && c < 8) return true;
        // Top-right
        if (r < 8 && c >= mod - 8) return true;
        // Bottom-left
        if (r >= mod - 8 && c < 8) return true;
        return false;
    }
}

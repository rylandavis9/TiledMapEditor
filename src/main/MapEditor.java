package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class MapEditor extends JFrame {
    int tileSize = 16; // size of tiles in tilesheet and map
    int rows = 20, cols = 20;
    int layerCount = 4;// number of layers

    int alpha = 48;
    Color myColor = new Color(255, 0, 255, alpha);

    // 3D map array: [layer][row][col]
    String[][][] mapLayers = new String[layerCount][rows][cols];

    String currentTile = "e0"; // default tile symbol selected
    int currentLayer = 0;      // currently active editing layer

    Tile[] tileSet;
    ArrayList<String> symbolList = new ArrayList<>();

    // New: track spritesheet info
    ArrayList<String> spritesheetNames = new ArrayList<>();
    ArrayList<Integer> spritesheetStartIndexes = new ArrayList<>();

    private double zoom = 1.0;
    private final double zoomStep = 0.1;

    JPanel tileButtonsPanel;
    EditorPanel editorPanel;

    JComboBox<String> layerSelector;  // UI control for layer selection

    public MapEditor() {
        initializeMap();
        //initializeTiles();

        editorPanel = new EditorPanel();
        JScrollPane scrollPane = new JScrollPane(editorPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Tile Buttons Panel (wrap layout with scroll)
        tileButtonsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 5));
        populateTileButtons();

        JScrollPane tilesScrollPane = new JScrollPane(tileButtonsPanel);
        tilesScrollPane.setPreferredSize(new Dimension(600, 180));
        add(tilesScrollPane, BorderLayout.NORTH);

        // Controls Panel
        JPanel controls = new JPanel();

        JSpinner rowSpinner = new JSpinner(new SpinnerNumberModel(rows, 1, 100, 1));
        JSpinner colSpinner = new JSpinner(new SpinnerNumberModel(cols, 1, 100, 1));
        JButton resizeBtn = new JButton("Resize Map");

        resizeBtn.addActionListener(e -> {
            rows = (Integer) rowSpinner.getValue();
            cols = (Integer) colSpinner.getValue();

            // Resize each layer's map properly, keep old data when possible
            String[][][] newMapLayers = new String[layerCount][rows][cols];
            for (int l = 0; l < layerCount; l++) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (r < mapLayers[l].length && c < mapLayers[l][0].length) {
                            newMapLayers[l][r][c] = mapLayers[l][r][c];
                        } else {
                            newMapLayers[l][r][c] = "e0"; // default tile
                        }
                    }
                }
            }
            mapLayers = newMapLayers;

            updateEditorPanelSize();
        });

        JButton saveBtn = new JButton("Save");
        JButton loadBtn = new JButton("Load");
        JButton zoomInBtn = new JButton("+");
        JButton zoomOutBtn = new JButton("-");
        JButton exportBtn = new JButton("Export Map + Tiles");
        exportBtn.addActionListener(e -> exportMapAndTiles());

        saveBtn.addActionListener(e -> saveMapWithFileChooser());
        loadBtn.addActionListener(e -> {
            loadMapWithFileChooser();
            editorPanel.repaint();
        });

        zoomInBtn.addActionListener(e -> {
            zoom += zoomStep;
            updateEditorPanelSize();
        });

        zoomOutBtn.addActionListener(e -> {
            zoom = Math.max(zoomStep, zoom - zoomStep);
            updateEditorPanelSize();
        });

        // Layer selector combo box
        layerSelector = new JComboBox<>();
        for (int i = 0; i < layerCount; i++) {
            if(i == 0) {
                layerSelector.addItem("Ground");
            } else if (i == 1) {
                layerSelector.addItem("Midground");
            } else if (i == 2) {
                layerSelector.addItem("COLLISION");
            }else {
                layerSelector.addItem("Top ");
            }

        }
        layerSelector.addActionListener(e -> {
            currentLayer = layerSelector.getSelectedIndex();
        });

        controls.add(new JLabel("Rows:"));
        controls.add(rowSpinner);
        controls.add(new JLabel("Columns:"));
        controls.add(colSpinner);
        controls.add(resizeBtn);
        controls.add(saveBtn);
        controls.add(loadBtn);
        controls.add(zoomInBtn);
        controls.add(zoomOutBtn);

        JButton clearTopLayersBtn = new JButton("Clear Top Layers");
        clearTopLayersBtn.addActionListener(e -> {
            for (int l = 1; l < layerCount; l++) {
                for (int r = 0; r < rows; r++) {
                    Arrays.fill(mapLayers[l][r], "e0");
                }
            }
            editorPanel.repaint();
        });
        controls.add(clearTopLayersBtn);

        controls.add(new JLabel("Edit Layer:"));
        controls.add(layerSelector);
        controls.add(exportBtn);

        JButton eraserButton = new JButton("Eraser");
        eraserButton.addActionListener(e -> currentTile = "e0");
        controls.add(eraserButton);
        JButton collisionButton = new JButton("Colider");
        collisionButton.addActionListener(e -> currentTile = "c0");
        controls.add(collisionButton);

        JButton importTilesheetBtn = new JButton("Import Tilesheet");
        importTilesheetBtn.addActionListener(e -> importTilesheets());
        controls.add(importTilesheetBtn);

        add(controls, BorderLayout.SOUTH);

        setTitle("Tile Map Editor");
        setSize(1450, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        updateEditorPanelSize();
    }

    private void updateEditorPanelSize() {
        editorPanel.setPreferredSize(new Dimension((int)(cols * tileSize * zoom), (int)(rows * tileSize * zoom)));
        editorPanel.revalidate();
        editorPanel.repaint();
    }

    private void initializeMap() {
        for (int l = 0; l < layerCount; l++) {
            for (int i = 0; i < rows; i++) {
                Arrays.fill(mapLayers[l][i], "e0");
            }
        }
    }

    /*private void initializeTiles() {
        ArrayList<Tile> tileList = new ArrayList<>();
        symbolList.clear();
        spritesheetNames.clear();
        spritesheetStartIndexes.clear();

        int index = 1;

        try {
            File tilesDir = new File("src/tiles");
            File[] imageFiles = tilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

            if (imageFiles == null) {
                JOptionPane.showMessageDialog(this, "No tilesheets found in src/tiles");
                System.exit(1);
            }

            for (File file : imageFiles) {
                spritesheetNames.add(file.getName());
                spritesheetStartIndexes.add(tileList.size());

                BufferedImage sheet = ImageIO.read(file);

                int sheetCols = sheet.getWidth() / tileSize;
                int sheetRows = sheet.getHeight() / tileSize;

                for (int y = 0; y < sheetRows; y++) {
                    for (int x = 0; x < sheetCols; x++) {
                        BufferedImage sub = sheet.getSubimage(x * tileSize, y * tileSize, tileSize, tileSize);
                        tileList.add(new Tile(sub));
                        symbolList.add("t" + index);
                        index++;
                    }
                }
            }

            tileSet = tileList.toArray(new Tile[0]);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load tilesheets.");
            System.exit(1);
        }
    }*/

    private void populateTileButtons() {
        tileButtonsPanel.removeAll();

        int sheetCount = spritesheetNames.size();

        for (int sheetIdx = 0; sheetIdx < sheetCount; sheetIdx++) {
            // Add header label for this spritesheet
            String sheetName = spritesheetNames.get(sheetIdx);
            JLabel header = new JLabel(sheetName);
            header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
            header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            tileButtonsPanel.add(header);

            int startIdx = spritesheetStartIndexes.get(sheetIdx);
            int endIdx = (sheetIdx + 1 < sheetCount) ? spritesheetStartIndexes.get(sheetIdx + 1) : tileSet.length;

            for (int i = startIdx; i < endIdx; i++) {
                BufferedImage img = tileSet[i].image;
                if (isEmptyTile(img)) continue; // skip fully transparent tiles

                Image scaled = img.getScaledInstance(tileSize * 2, tileSize * 2, Image.SCALE_SMOOTH);
                final String tileCode = symbolList.get(i);
                JButton btn = new JButton(new ImageIcon(scaled));
                btn.setPreferredSize(new Dimension(tileSize * 2 + 4, tileSize * 2 + 4));
                btn.addActionListener(e -> currentTile = tileCode);
                tileButtonsPanel.add(btn);
            }
        }

        tileButtonsPanel.revalidate();
        tileButtonsPanel.repaint();
    }

    private boolean isEmptyTile(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha != 0) return false;  // non-transparent pixel found
            }
        }
        return true;  // fully transparent tile
    }

    private void placeTile(MouseEvent e) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, editorPanel);
        if (scrollPane == null) return;

        Point viewPos = scrollPane.getViewport().getViewPosition();
        int scaledTileSize = (int) (tileSize * zoom);

        int mouseX = e.getX() + viewPos.x;
        int mouseY = e.getY() + viewPos.y;

        int col = mouseX / scaledTileSize;
        int row = mouseY / scaledTileSize;

        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            mapLayers[currentLayer][row][col] = currentTile;
            int x = col * scaledTileSize;
            int y = row * scaledTileSize;
            editorPanel.repaint(x, y, scaledTileSize, scaledTileSize);
        }
    }

    class EditorPanel extends JPanel {
        public EditorPanel() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    placeTile(e);
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    placeTile(e);
                }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int scaledTileSize = (int) (tileSize * zoom);

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {

                    // Draw all layers bottom to top
                    for (int l = 0; l < layerCount; l++) {
                        String symbol = mapLayers[l][i][j];
                        if (symbol == null || symbol.equals("e0")) continue; // skip empty tiles

                        if (symbol.equals("c0")) {
                            g2.setColor(myColor);
                            g2.fillRect(j * scaledTileSize, i * scaledTileSize, scaledTileSize, scaledTileSize);
                        }

                        int tileIndex = symbolList.indexOf(symbol);
                        if (tileIndex < 0) continue;

                        BufferedImage img = tileSet[tileIndex].image;
                        if (isEmptyTile(img)) continue; // skip fully transparent tiles

                        int x = j * scaledTileSize;
                        int y = i * scaledTileSize;
                        g2.drawImage(img, x, y, scaledTileSize, scaledTileSize, null);
                    }

                    // Draw grid lines once on top
                    g2.setColor(Color.BLACK);
                    g2.drawRect(j * scaledTileSize, i * scaledTileSize, scaledTileSize, scaledTileSize);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int scaledTileSize = (int) (tileSize * zoom);
            return new Dimension(cols * scaledTileSize, rows * scaledTileSize);
        }
    }

    // Save all layers, each to its own file named with layer index
    public void saveMapWithFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select folder to save all map layers");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = fileChooser.getSelectedFile();

            try {
                for (int l = 0; l < layerCount; l++) {
                    File file = new File(dir, "map_layer_" + l + ".txt");
                    try (PrintWriter pw = new PrintWriter(file)) {
                        for (int i = 0; i < rows; i++) {
                            pw.println(String.join(" ", mapLayers[l][i]));
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "Map layers saved to " + dir.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to save map layers: " + e.getMessage());
            }
        }
    }

    // Load all layers from files named map_layer_0.txt, map_layer_1.txt, etc.
    public void loadMapWithFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select folder containing map layer files");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = fileChooser.getSelectedFile();

            try {
                for (int l = 0; l < layerCount; l++) {
                    File file = new File(dir, "map_layer_" + l + ".txt");
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(this, "Missing layer file: " + file.getName());
                        return;
                    }

                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        for (int i = 0; i < rows; i++) {
                            String line = br.readLine();
                            if (line == null) break;
                            mapLayers[l][i] = line.split(" ");
                        }
                    }
                }
                editorPanel.repaint();
                JOptionPane.showMessageDialog(this, "Map layers loaded from " + dir.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load map layers: " + e.getMessage());
            }
        }
    }

    // Export map layers and tiles to folder
    public void exportMapAndTiles() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // user cancelled
        }

        File exportDir = folderChooser.getSelectedFile();

        // Save each layer as separate text file
        try {
            for (int l = 0; l < layerCount; l++) {
                if (l == 2) {
                    File mapFile = new File(exportDir, "COLLISION.txt");
                    try (PrintWriter pw = new PrintWriter(mapFile)) {
                        for (int i = 0; i < rows; i++) {
                            pw.println(String.join(" ", mapLayers[l][i]));
                        }
                    }
                } else {
                    File mapFile = new File(exportDir, "map_layer_" + l + ".txt");
                    try (PrintWriter pw = new PrintWriter(mapFile)) {
                        for (int i = 0; i < rows; i++) {
                            pw.println(String.join(" ", mapLayers[l][i]));
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save map files: " + e.getMessage());
            return;
        }

        // Save tile images
        for (int i = 0; i < tileSet.length; i++) {
            String tileCode = symbolList.get(i);
            BufferedImage img = tileSet[i].image;

            if (isEmptyTile(img)) continue;

            File tileFile = new File(exportDir, tileCode + ".png");
            try {
                ImageIO.write(img, "png", tileFile);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to save tile " + tileCode + ": " + e.getMessage());
                return;
            }
        }

        JOptionPane.showMessageDialog(this, "Export complete!\nMap layers and tiles saved to:\n" + exportDir.getAbsolutePath());
    }

    private BufferedImage createTransparentTile() {
        BufferedImage transparent = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparent.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tileSize, tileSize);
        g2d.dispose();
        return transparent;
    }
    private void importTilesheets() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Tilesheet(s) to Import");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File[] selectedFiles = fileChooser.getSelectedFiles();

        ArrayList<Tile> tileList = (tileSet != null)
                ? new ArrayList<>(Arrays.asList(tileSet))
                : new ArrayList<>();
        int index = symbolList.size();

        for (File file : selectedFiles) {
            try {
                spritesheetNames.add(file.getName());
                spritesheetStartIndexes.add(tileList.size());

                BufferedImage sheet = ImageIO.read(file);
                int sheetCols = sheet.getWidth() / tileSize;
                int sheetRows = sheet.getHeight() / tileSize;

                for (int y = 0; y < sheetRows; y++) {
                    for (int x = 0; x < sheetCols; x++) {
                        BufferedImage sub = sheet.getSubimage(x * tileSize, y * tileSize, tileSize, tileSize);
                        tileList.add(new Tile(sub));
                        symbolList.add("t" + index++);
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to import: " + file.getName());
            }
        }

        tileSet = tileList.toArray(new Tile[0]);
        populateTileButtons();
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapEditor::new);
    }
}

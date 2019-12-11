package GUI.Canvas;

import blockchainCore.blockchain.Block;
import blockchainCore.utils.Utils;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BlockShape {
    private static final double BLANK = 10, OPACITY = 0.3;;

    private Pane rootPane;

    private final BlockTree blockTree = new BlockTree();
    private boolean bSelected = false; // it's flag that any node is selected.

    public BlockShape(ScrollPane scrollPane) {
        rootPane = new Pane();
        rootPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrollPane.setContent(rootPane);
    }

    public synchronized void addBlock(Block block) {
        if (!blockTree.contains(Utils.toHexString(block.getHash())))
            blockTree.add(rootPane, block, bSelected);
    }
    public synchronized void addKnownBlock(String... blocks) {
        blockTree.addKnownBlocks(blocks);
    }
    public synchronized void clearKnownBlock(boolean bSelected) {
        this.bSelected = bSelected;
        blockTree.clearKnownBlocks(bSelected);
    }
    public synchronized void setTip(String blockHash) {
        blockTree.setTip(blockHash);
    }

    public static class GBlock {
        private static final double SIZE = 20;
        private Block block;

        private double x, y, ry, h = SIZE;
        private Rectangle rect;

        GBlock(Block block) {
            this.block = block;
            rect = new Rectangle(SIZE, SIZE);
            rect.setFill(Color.RED);
            rect.setStroke(Color.GREEN);
            rect.setStrokeWidth(1);

            x = block.getHeight()*SIZE + (block.getHeight()+1)*BLANK;
            rect.setX(x);
        }

        void setY(double y) {
            this.y = y;
            rect.setY(y-SIZE/2);
        }
        String getHash() { return Utils.toHexString(block.getHash()); }
        String getPrevBlockHash() { return Utils.toHexString(block.getPrevBlockHash()); }

        void setColor(Color color) { rect.setFill(color); }
        void setOpaque(boolean bOpaque) {
            rect.setOpacity(bOpaque ? 1 : OPACITY);
        }
    }
    public static class BlockTree {
        private final HashMap<String, ArrayList<GBlock>> tree = new HashMap<>();
        private final HashMap<String, GBlock> blocks = new HashMap<>();
        private final HashMap<String, Line> lines = new HashMap<>();

        private final HashSet<String> knownBlocks = new HashSet<>();
        private String tip = null;

        public BlockTree() {
            tree.put("", new ArrayList<>());
        }

        public synchronized void add(Pane pane, Block block, boolean bSelected) {
            GBlock gBlock = new GBlock(block);
            gBlock.setOpaque(!bSelected);

            String hash = gBlock.getHash();
            String prevHash = gBlock.getPrevBlockHash();

            tree.get(prevHash).add(gBlock);
            tree.put(hash, new ArrayList<>());
            blocks.put(hash, gBlock);

            if (!prevHash.equals("")) {
                Line line = new Line();
                line.setStrokeWidth(3);
                line.setOpacity(bSelected ? OPACITY : 1);

                lines.put(hash, line);
                pane.getChildren().add(line);
            }
            pane.getChildren().add(gBlock.rect);

            calcTreeRelativeY(gBlock);
        }

        private void calcTreeRelativeY(GBlock gBlock) {
            do
                calcRelativeY(gBlock.getHash());
            while ((gBlock = blocks.get(gBlock.getPrevBlockHash())) != null);

            calcPosition("", BLANK);
        }
        private void calcRelativeY(String hash) {
            ArrayList<GBlock> child = tree.get(hash);
            GBlock block = blocks.get(hash);

            if (child.isEmpty())
                block.ry = block.h/2;
            else {
                // calc block height
                double totalH = 0;
                for (GBlock cBlock : child)
                    totalH += cBlock.h;
                totalH += (child.size()-1) * BLANK;
                block.h = totalH;

                GBlock cBlock = child.get(child.size()-1);
                block.ry = (child.get(0).ry + (block.h - cBlock.h) + cBlock.ry) / 2;
            }
        }
        private void calcPosition(String _hash, double y) {
            for (GBlock gBlock : tree.get(_hash)) {
                gBlock.setY(y + gBlock.ry); // Block Position

                // Line Position
                String prevHash = gBlock.getPrevBlockHash();
                String hash = gBlock.getHash();
                if (!prevHash.equals("")) {
                    Line line = lines.get(hash);
                    GBlock prevBlock = blocks.get(prevHash);

                    line.setStartX(prevBlock.x + GBlock.SIZE);
                    line.setStartY(prevBlock.y);

                    line.setEndX(gBlock.x);
                    line.setEndY(gBlock.y);
                }

                calcPosition(hash, y); // Next Block
                y += gBlock.h + BLANK; // Next Block chain
            }
        }

        public void addKnownBlocks(String... blockHash) {
            for (String hash : blockHash) {
                knownBlocks.add(hash);

                GBlock gBlock = blocks.get(hash);
                gBlock.setOpaque(true);

                String pHash = gBlock.getPrevBlockHash();
                if (knownBlocks.contains(pHash))
                    lines.get(hash).setOpacity(1);

                for (GBlock cBlock : tree.get(hash)) {
                    String cHash = cBlock.getHash();
                    if (knownBlocks.contains(cHash))
                        lines.get(cHash).setOpacity(1);
                }
            }
        }
        public void clearKnownBlocks(boolean bSelected) {
            for (GBlock gBlock : blocks.values())
                gBlock.setOpaque(!bSelected);
            for (Line line : lines.values())
                line.setOpacity(bSelected ? OPACITY : 1);
            knownBlocks.clear();
        }
        public void setTip(String tip) {
            if (this.tip != null)
                setChainColor(this.tip, Color.RED);

            this.tip = tip;

            if (this.tip != null)
                setChainColor(this.tip, Color.GREEN);
        }
        private void setChainColor(String hash, Color color) {
            while (!hash.equals("")) {
                GBlock gBlock = blocks.get(hash);
                gBlock.setColor(color);
                hash = gBlock.getPrevBlockHash();
            }
        }

        public boolean contains(String hash) {
            return blocks.containsKey(hash);
        }
    }
}

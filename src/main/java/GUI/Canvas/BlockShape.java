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
import java.util.concurrent.ConcurrentSkipListSet;

public class BlockShape {
    private static final double BLANK = 10;

    private Pane rootPane;

    private final BlockTree blockTree = new BlockTree();
    private ConcurrentSkipListSet<String> knownBlock = null; //TODO: Add Effect

    public BlockShape(ScrollPane scrollPane) {
        rootPane = new Pane();
        rootPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrollPane.setContent(rootPane);
    }

    public void addBlock(Block block) {
        if (!blockTree.contains(block))
            blockTree.add(rootPane, block);
    }
    public void setKnownBlock(ConcurrentSkipListSet<String> knownBlocks) {
        this.knownBlock = knownBlocks;
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

            // rect.setTranslateX(100); // TODO: Add Animation
        }
    }
    public static class BlockTree {
        private final HashMap<String, ArrayList<GBlock>> tree = new HashMap<>();
        private final HashMap<String, GBlock> blocks = new HashMap<>();
        private final HashMap<String, Line> lines = new HashMap<>();

        public BlockTree() {
            tree.put("", new ArrayList<>());
        }

        public synchronized void add(Pane pane, Block block) {
            GBlock gBlock = new GBlock(block);
            String prevHash = Utils.toHexString(block.getPrevBlockHash());
            String hash = Utils.toHexString(block.getHash());

            tree.get(prevHash).add(gBlock);
            tree.put(hash, new ArrayList<>());
            blocks.put(hash, gBlock);

            if (!prevHash.equals("")) {
                Line line = new Line();
                line.setStrokeWidth(3);
                lines.put(hash, line);
                pane.getChildren().add(line);
            }
            pane.getChildren().add(gBlock.rect);

            calcTreeRelativeY(gBlock);
        }
        public ArrayList<GBlock> get(String hash) {
            return tree.get(hash);
        }

        private void calcTreeRelativeY(GBlock gBlock) {
            while (gBlock != null) {
                calcRelativeY(Utils.toHexString(gBlock.block.getHash()));
                gBlock = blocks.get(Utils.toHexString(gBlock.block.getPrevBlockHash()));
            }

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
            for (GBlock gBlock : get(_hash)) {
                gBlock.setY(y + gBlock.ry);

                String hash = Utils.toHexString(gBlock.block.getHash());
                String prevHash = Utils.toHexString(gBlock.block.getPrevBlockHash());
                if (!prevHash.equals("")) {
                    Line line = lines.get(hash);
                    GBlock prevBlock = blocks.get(prevHash);

                    line.setStartX(prevBlock.x + GBlock.SIZE);
                    line.setStartY(prevBlock.y);

                    line.setEndX(gBlock.x);
                    line.setEndY(gBlock.y);
                }

                calcPosition(hash, y);
                y += gBlock.h + BLANK;
            }
        }

        public boolean contains(Block block) {
            return blocks.containsKey(Utils.toHexString(block.getHash()));
        }
    }
}

package GUI.Canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeContext {
    private Canvas canvas;
    private GraphicsContext gc;
    private final ConcurrentHashMap<String, GNode> nodes = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<GConnection> connections = new ConcurrentSkipListSet<>(Comparator.comparing(gConnection -> gConnection.id));
    private final ConcurrentSkipListSet<GTransmission> transmissions = new ConcurrentSkipListSet<>(Comparator.comparing(gTransmission -> gTransmission.id));

    public NodeContext(Canvas canvas) {
        this.canvas = canvas;
        gc = canvas.getGraphicsContext2D();
    }
    public void close() {
        nodes.clear();
        connections.clear();
        transmissions.clear();
    }

    public void update() {
        for (GTransmission gTransmission : transmissions) {
            if (!gTransmission.update())
                transmissions.remove(gTransmission);
        }
    }
    public void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (GConnection gConn : connections)
            gConn.draw(gc);
        for (GTransmission gTransmission : transmissions)
            gTransmission.draw(gc);
        for (GNode gNode : nodes.values())
            gNode.draw(gc);
    }

    public Object onClick(double x, double y) {
        for (GNode gNode : nodes.values()) {
            if (gNode.isCollision(x, y))
                return gNode;
        }

        for (GConnection gConn : connections) {
            if (gConn.isCollision(x, y))
                return gConn;
        }

        return null;
    }

    public void addNode(String nodeId, double x, double y) {
        nodes.put(nodeId, new GNode(nodeId, x, y));
    }
    public void removeNode(String nodeId) {
        connections.removeIf(gConn -> gConn.node1.nodeId.equals(nodeId) || gConn.node2.nodeId.equals(nodeId));
        nodes.remove(nodeId);
    }

    public void addConnection(String src, String dest) {
        GNode node1 = nodes.get(src);
        GNode node2 = nodes.get(dest);
        connections.add(new GConnection(node1, node2));
    }
    public void removeConnection(GConnection conn) {
        connections.remove(conn);
    }
    public boolean containsConnection(String src, String dest) {
        GNode node1 = nodes.get(src);
        GNode node2 = nodes.get(dest);

        for (GConnection gConn : connections) {
            if (gConn.node1 == node1 && gConn.node2 == node2 ||
                    gConn.node1 == node2 && gConn.node2 == node1)
                return true;
        }

        return false;
    }

    public int addTransmission(String src, String dest, boolean bBlock) {
        GNode node1 = nodes.get(src);
        GNode node2 = nodes.get(dest);

        GTransmission gTrans = new GTransmission(node1, node2, bBlock);
        transmissions.add(gTrans);

        return gTrans.id;
    }
    public boolean containsTransmission(int id) {
        for (GTransmission gTrans : transmissions) {
            if (gTrans.id == id)
                return true;
        }

        return false;
    }

    public static class GNode  {
        private static final double RADIUS = 15, BORDER_WIDTH = 2;
        private String nodeId;
        private double x, y;

        public GNode(String nodeId, double x, double y) {
            this.nodeId = nodeId;
            this.x = x; this.y = y;
        }

        public void draw(GraphicsContext gc) {
            gc.save();
            gc.setFill(Color.BLUE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(BORDER_WIDTH);

            gc.fillOval(x-RADIUS, y-RADIUS, RADIUS*2, RADIUS*2);
            gc.strokeOval(x-RADIUS, y-RADIUS, RADIUS*2, RADIUS*2);

            gc.restore();
        }

        public boolean isCollision(double x, double y) {
            return Math.sqrt(Math.pow(this.x-x, 2) + Math.pow(this.y-y, 2)) <= GNode.RADIUS+BORDER_WIDTH;
        }

        public String getNodeId() { return nodeId; }
    }
    public static class GConnection {
        private static final double WIDTH = 4, COLLISION_WIDTH = WIDTH*1.5;
        private static int Count = 0;
        private int id = Count++;
        private GNode node1, node2;

        public GConnection(GNode node1, GNode node2) {
            this.node1 = node1;
            this.node2 = node2;
        }

        public void draw(GraphicsContext gc) {
            gc.save();
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(WIDTH);

            gc.strokeLine(node1.x, node1.y, node2.x, node2.y);

            gc.restore();
        }

        public boolean isCollision(double x, double y) {
            double x1 = node1.x, y1 = node1.y;
            double x2 = node2.x, y2 = node2.y;

            double vecLen = Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
            double vecX = (x2-x1)/vecLen, vecY = (y2-y1)/vecLen;
            double perX = vecY, perY = -vecX;

            double targetLen;

            // Vec OBB
            double len1 = x1*vecX + y1*vecY, len2 = x2*vecX + y2*vecY;
            targetLen = x*vecX + y*vecY;

            if (targetLen < len1 && targetLen < len2 || targetLen > len1 && targetLen > len2)
                return false;

            // 수직선 OBB
            double perLen = x1*perX + y1*perY;
            targetLen = x*perX + y*perY;

            if (Math.abs(targetLen - perLen) > COLLISION_WIDTH) // WIDTH/2
                return false;

            return true;
        }
    }
    public static class GTransmission {
        private static final double RADIUS = 5, SIZE = 8, BORDER_WIDTH = 1;
        private static double Speed = 5;

        private static final Object MUTEXT = new Object();
        private static int Count = 0;
        private int id;

        private double x, y, dist;
        private double dirX, dirY, len;
        private double destX, destY;
        private boolean bBlock;

        public GTransmission(GNode src, GNode dest, boolean bBLock) {
            synchronized (MUTEXT) {
                id = Count++;
            }

            x = src.x; y = src.y; dist = 0;
            len = Math.sqrt(Math.pow(dest.x-src.x, 2) + Math.pow(dest.y-src.y, 2));
            dirX = (dest.x - src.x)/len; dirY = (dest.y - src.y)/len;
            destX = dest.x; destY = dest.y;

            this.bBlock = bBLock;
        }

        public boolean update() {
            if (dist >= len) return false; // 종료

            dist += Speed;
            if (dist >= len) {
                x = destX;
                y = destY;
            }
            else {
                x += dirX * Speed;
                y += dirY * Speed;
            }

            return true;
        }
        public void draw(GraphicsContext gc) {
            gc.save();
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(BORDER_WIDTH);

            if (bBlock) { // Block
                gc.setFill(Color.RED);
                gc.fillRect(x - SIZE/2, y - SIZE/2, SIZE, SIZE);
                gc.strokeRect(x - SIZE/2, y - SIZE/2, SIZE, SIZE);
            }
            else { // Tx
                gc.setFill(Color.YELLOW);
                gc.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
                gc.strokeOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
            }

            gc.restore();
        }
    }
}

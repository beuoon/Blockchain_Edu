package GUI.Controller;

import GUI.BlockTree;
import GUI.Canvas.NodeContext;
import blockchainCore.BlockchainCore;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.event.*;
import blockchainCore.blockchain.transaction.*;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.Node;
import blockchainCore.utils.Utils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentSkipListSet;

public class MainSceneController implements Initializable, SignalListener {
    @FXML
    private Canvas nodeCanvas, blockCanvas;
    private NodeContext nodeContext;
    private GraphicsContext blockGC;

    @FXML
    private TreeTableView<Pair<String, String>> treeTableView;

    private final BlockchainCore bcCore = new BlockchainCore();
    private final BlockTree blockTree = new BlockTree();
    private Timeline frameTimeline; // Thread 사용시 간혈적으로 freeze 발생

    // Mouse Event
    private double clickPosX, clickPosY;
    private Object clickObject = null;
    private ContextMenu contextMenu = new ContextMenu();

    private Node selectedNode = null;
    private String selectedNodeId = "";
    private ConcurrentSkipListSet<String> nodeBlocks = new ConcurrentSkipListSet<>();
    private final Object MUTEX = new Object();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nodeContext = new NodeContext(nodeCanvas);
        blockGC = blockCanvas.getGraphicsContext2D();

        TreeTableColumn<Pair<String, String>, String> keyCol = new TreeTableColumn<>("Key");
        TreeTableColumn<Pair<String, String>, String> valueCol = new TreeTableColumn<>("Value");

        keyCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("key"));
        valueCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));

        valueCol.setMinWidth(275); valueCol.setMaxWidth(275);

        treeTableView.getColumns().addAll(keyCol, valueCol);

        // TODO: txPool에 있는 tx도 send하는 UTXO에서 처리해줘야 됨
        // TODO: 그래야 유동적인 트랜잭션 생성이 가능함

        frameTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(0),
                        event -> { update(); draw(); }
                ),
                new KeyFrame(Duration.millis(100))
        );
        frameTimeline.setCycleCount(Timeline.INDEFINITE);
        frameTimeline.play();

        SignalHandler.setListener(this);
        nodeCanvas.setOnMousePressed(this::onMousePressed);
        nodeCanvas.setOnMouseClicked(this::onMouseClicked);
        // nodeCanvas.setOnMouseDragged(this::onMouseEvent);
    }
    public void shutdown() {
        frameTimeline.stop();
        bcCore.destroyNodeAll();
    }

    private void update() {
        nodeContext.update();
    }
    private void draw() {
        nodeContext.draw();

        blockGC.setFill(Color.BLUE);
        blockGC.fillRect(0, 0, blockCanvas.getWidth(), blockCanvas.getHeight());
    }


    //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
    // Mouse Event
    private void onMousePressed(MouseEvent event) {
        if (contextMenu.isShowing()) {
            contextMenu.hide();
            return ;
        }

        clickPosX = event.getX();
        clickPosY = event.getY();

        switch (event.getButton()) {
            case PRIMARY:
                clickObject = nodeContext.onClick(clickPosX, clickPosY);
                break;
            case SECONDARY:
                break;
        }
    }
    private void onMouseClicked(MouseEvent event) {
        double x = event.getX(), y = event.getY();
        Object obj = nodeContext.onClick(x, y);

        switch (event.getButton()) {
            case PRIMARY:
                if (obj == null) {
                    if (clickObject == null && event.getClickCount() >= 2) // Create Node
                        createNode(x, y);
                }
                else if (obj instanceof NodeContext.GNode) {
                    if (clickObject instanceof NodeContext.GNode) {
                        if (obj == clickObject) { // Select Node
                            String nodeId = ((NodeContext.GNode) obj).getNodeId();
                            selectNode(nodeId);
                        } else { // Connect both node
                            String src = ((NodeContext.GNode) clickObject).getNodeId();
                            String dest = ((NodeContext.GNode) obj).getNodeId();

                            if (!nodeContext.containsConnection(src, dest)) {
                                bcCore.createConnection(src, dest);
                                nodeContext.addConnection(src, dest);
                            }
                        }
                    }
                }
                break;
            case SECONDARY:
                if (obj instanceof NodeContext.GNode) {
                    onContextMenuRequested(((NodeContext.GNode) obj).getNodeId(),
                            event.getScreenX(), event.getScreenY());
                }
                else if (obj instanceof NodeContext.GConnection)
                    nodeContext.removeConnection((NodeContext.GConnection)obj);
                break;
        }
    }
    private void onContextMenuRequested(String nodeId, double x, double y) {
        if (contextMenu.isShowing())
            contextMenu.hide();

        // create a menu
        contextMenu = new ContextMenu();

        // create menuitems
        MenuItem nodeIdItem = new MenuItem("Node: " + nodeId);
        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem sendItem = new MenuItem("Send");

        nodeIdItem.setDisable(true);
        deleteItem.setOnAction(event -> deleteNode(nodeId));
        // TODO: Create Send Function
        // sendItem.setOnAction(event -> );

        // add menu items to menu
        contextMenu.getItems().addAll(nodeIdItem, deleteItem, sendItem);

        contextMenu.show(nodeCanvas, x, y);
    }

    //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
    // Block chain Event
    @Override
    public void onEvent(SignalType type, Object... arvg) {
        switch (type) {
            case ADD_BLOCK:     addBlock((String)arvg[0], (Block)arvg[1]);                      break;
            case HANDLE_BLOCK:  handleObject((String)arvg[0], (String)arvg[1], true);   break;
            case HANDLE_TX:     handleObject((String)arvg[0], (String)arvg[1], false);  break;
        }
    }

    private void createNode(double x, double y) {
        String nodeId = bcCore.createNode();
        nodeContext.addNode(nodeId, x, y);
    }
    private void selectNode(String nodeId) {
        synchronized (MUTEX) {
            selectedNodeId = nodeId;
            selectedNode = bcCore.getNode(selectedNodeId);
            treeTableView.setRoot(convertTreeView(selectedNode));

            nodeBlocks = new ConcurrentSkipListSet<>();
            for (Block block : selectedNode.getBlockChain().getBlocks())
                nodeBlocks.add(Utils.toHexString(block.getHash()));
            // blockcanvas.setKnownBlock(nodeBlocks);
        }
    }
    private void deleteNode(String nodeId) {
        synchronized (MUTEX) {
            if (selectedNodeId.equals(nodeId)) {
                selectedNodeId = "";
                selectedNode = null;
            }

            nodeContext.removeNode(nodeId);
            bcCore.destoryNode(nodeId);
        }
    }
    private void addBlock(String nodeId, Block block) {
        if (!blockTree.contains(block)) {
            blockTree.put(block);
            // TODO: resetting block position of block chain canvas
        }

        synchronized (MUTEX) {
            if (selectedNodeId.equals(nodeId)) {
                renewalBalance();
                nodeBlocks.add(Utils.toHexString(block.getHash()));
            }
        }
    }
    private void handleObject(String from, String to, boolean bBlock) {
        int id = nodeContext.addTransmission(from, to, bBlock);
        while (nodeContext.containsTransmission(id))
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
    }

    //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
    // Tree View
    private static TreeTableItem convertTreeView(Node node) {
        HashMap<String, Integer> balance = node.getBalances();

        TreeTableItem nodeItem = new TreeTableItem("Node", node.getNodeId());

        TreeTableItem wallets = new TreeTableItem("Wallets", "");
        for (Wallet wallet : node.getWallets()) {
            String address = wallet.getAddress();
            TreeTableItem walletItem = new TreeTableItem("Wallet", address);

            TreeTableItem walletAddress = new TreeTableItem("address", address);
            TreeTableItem walletPubKey = new TreeTableItem("publicKey", Utils.toHexString(wallet.getPublicKey().getEncoded()));
            TreeTableItem walletPriKey = new TreeTableItem("privateKey", Utils.toHexString(wallet.getPrivateKey().getEncoded()));
            TreeTableItem walletBalance = new TreeTableItem("balance", balance.get(address).toString());

            walletItem.getChildren().addAll(walletAddress, walletPubKey, walletPriKey, walletBalance);
            wallets.getChildren().add(walletItem);
        }

        TreeTableItem txPool = new TreeTableItem("Transactions", "");
        for (Transaction tx : node.getTxsFromTxPool()) {
            TreeTableItem txItem = new TreeTableItem("Tx", Utils.toHexString(tx.getId()));

            TreeTableItem vins = new TreeTableItem("vIn", "");
            for (int i = 0; i < tx.getVin().size(); i++) {
                TxInput vin = tx.getVin().get(i);
                TreeTableItem txId = new TreeTableItem("value", Utils.toHexString(vin.getTxId()));
                TreeTableItem vOutIndex = new TreeTableItem("pubKeyHash", Integer.toString(vin.getvOut()));
                TreeTableItem signature = new TreeTableItem("value", Utils.toHexString(vin.getSignature()));
                TreeTableItem pubKey = new TreeTableItem("pubKeyHash", Utils.toHexString(vin.getPubKey().getEncoded()));


                TreeTableItem vinItem = new TreeTableItem("vIn", Integer.toString(i));
                vinItem.getChildren().addAll(txId, vOutIndex, signature, pubKey);

                vins.getChildren().add(vinItem);
            }


            TreeTableItem vouts = new TreeTableItem("vOut", "");
            for (int i = 0; i < tx.getVout().size(); i++) {
                TxOutput vout = tx.getVout().get(i);
                TreeTableItem value = new TreeTableItem("value", Integer.toString(vout.getValue()));
                TreeTableItem pubKeyHash = new TreeTableItem("pubKeyHash", Utils.toHexString(vout.getPublicKeyHash()));

                TreeTableItem voutItem = new TreeTableItem("vOut", Integer.toString(i));
                voutItem.getChildren().addAll(value, pubKeyHash);

                vouts.getChildren().add(voutItem);
            }


            txItem.getChildren().addAll(vins, vouts);
            txPool.getChildren().add(txItem);
        }

        nodeItem.setExpanded(true);
        nodeItem.getChildren().addAll(wallets, txPool);

        return nodeItem;
    }
    private void renewalBalance() {
        HashMap<String, Integer> balance = selectedNode.getBalances();
        TreeTableItem walletsItem = (TreeTableItem) treeTableView.getRoot().getChildren().filtered(t -> t.getValue().getKey().equals("Wallets")).get(0);

        for (TreeItem<Pair<String, String>> walletItem : walletsItem.getChildren()) {
            String address = walletItem.getValue().getValue();

            walletItem.getChildren().removeIf(various -> various.getValue().getKey().equals("balance"));
            walletItem.getChildren().add(new TreeTableItem("balance", balance.get(address).toString()));
        }
    }

    static class TreeTableItem extends TreeItem<Pair<String, String>> {
        public TreeTableItem(String key, String value) {
            super(new Pair<>(key, value));
        }
    }
}

package GUI.Controller;

import GUI.BlockTree;
import blockchainCore.BlockchainCore;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.event.*;
import blockchainCore.blockchain.transaction.*;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.Node;
import blockchainCore.utils.Utils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentSkipListSet;

public class MainSceneController implements Initializable, SignalListener {
    @FXML
    private Canvas nodeCanvas, blockCanvas;
    private GraphicsContext nodeGC, blockGC;

    @FXML
    private TreeTableView<Pair<String, String>> treeTableView;

    private final BlockchainCore bcCore = new BlockchainCore();
    private final BlockTree blockTree = new BlockTree();
    private boolean bFrameLoop = true;

    // Mouse Event
    private double clickPosX, clickPosY;
    private Object clickObject = null;
    private ContextMenu contextMenu = new ContextMenu();

    private Node selectedNode = null;
    private String selectedNodeId = "";
    private ConcurrentSkipListSet<String> nodeBlocks = new ConcurrentSkipListSet<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nodeCanvas.setOnMousePressed(this::onMousePressed);
        nodeCanvas.setOnMouseClicked(this::onMouseClicked);
        // nodeCanvas.setOnMouseDragged(this::onMouseEvent);

        nodeGC = nodeCanvas.getGraphicsContext2D();
        blockGC = blockCanvas.getGraphicsContext2D();

        SignalHandler.setListener(this);



        TreeTableColumn<Pair<String, String>, String> keyCol = new TreeTableColumn<>("Key");
        TreeTableColumn<Pair<String, String>, String> valueCol = new TreeTableColumn<>("Value");

        keyCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("key"));
        valueCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));

        valueCol.setMinWidth(275); valueCol.setMaxWidth(275);

        treeTableView.getColumns().addAll(keyCol, valueCol);

        selectNode(bcCore.createNode());

        // TODO: txPool에 있는 tx는 send하는 UTXO에서 빼야 됨

        new Thread(() -> {
            while (bFrameLoop) {
                update();
                draw();

                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }).start();
        draw();
    }
    public void shutdown() {
        bFrameLoop = false;
        bcCore.destroyNodeAll();
    }

    private void update() {
    }
    private void draw() {
        nodeGC.setFill(Color.BLACK);
        nodeGC.fillRect(0, 0, nodeCanvas.getWidth(), nodeCanvas.getHeight());

        blockGC.setFill(Color.BLUE);
        blockGC.fillRect(0, 0, blockCanvas.getWidth(), blockCanvas.getHeight());
    }


    //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
    // Mouse Event
    private void onMousePressed(MouseEvent event) {
        clickPosX = event.getScreenX();
        clickPosY = event.getScreenY();

        switch (event.getButton()) {
            case PRIMARY:
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                    break;
                }

                // TODO: call onClick of nodeCanvas
                // TODO: set return object to clicked object
                break;
            case SECONDARY:
                // TODO: call onClick of nodeCanvas
                // TODO: set return object to clicked object
                break;
        }
    }
    private void onMouseClicked(MouseEvent event) {
        switch (event.getButton()) {
            case PRIMARY:
                Object obj = null; // nodeCanvas.onClick(event.getScreenX, event.getScreenY);
                // if (obj instanceof CanvasNode)
                // TODO: if returned object for nodeCanvas's onClick is node,
                // TODO: if returned node equals clicked Object, change selected node to clicked node
                // TODO: else, connect both node

                // TODO: if returned object for nodeCanvas's onClick is null
                // TODO: and clicked object is null and clickCount over 2, create node
                bcCore.createNode();
                break;
            case SECONDARY:
                // TODO: returned object for nodeCanvas's onClick equals clicked Object
                // TODO: if that is node, show context menu for that
                onContextMenuRequested(event.getScreenX(), event.getScreenY());
                // TODO: if that is connection line, disconnect
                break;
        }
    }
    private void onContextMenuRequested(double x, double y) {
        if (contextMenu.isShowing())
            contextMenu.hide();

        // create a menu
        contextMenu = new ContextMenu();

        // create menuitems
        MenuItem menuItem1 = new MenuItem("Delete");
        MenuItem menuItem2 = new MenuItem("Send");

        // add menu items to menu
        contextMenu.getItems().add(menuItem1);
        contextMenu.getItems().add(menuItem2);

        contextMenu.show(nodeCanvas, x, y);
    }

    //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
    // Block chain Event
    @Override
    public void onEvent(SignalType type, Object... arvg) {
        switch (type) {
            case ADD_BLOCK:     addBlock((String)arvg[0], (Block)arvg[1]);                          break;
            case HANDLE_BLOCK:  handleBlock((String)arvg[0], (String)arvg[1], (Block)arvg[2]);      break;
            case HANDLE_TX:     handleTx((String)arvg[0], (String)arvg[1], (Transaction) arvg[2]);  break;
        }
    }

    private void selectNode(String nodeId) {
        synchronized (bcCore) {
            selectedNodeId = nodeId;
            selectedNode = bcCore.getNode(selectedNodeId);
            treeTableView.setRoot(convertTreeView(selectedNode));

            nodeBlocks = new ConcurrentSkipListSet<>();
            for (Block block : selectedNode.getBlockChain().getBlocks())
                nodeBlocks.add(Utils.toHexString(block.getHash()));
            // blockcanvas.setKnownBlock(nodeBlocks);
        }
    }
    private void addBlock(String nodeId, Block block) {
        if (!blockTree.contains(block)) {
            blockTree.put(block);
            // TODO: resetting block position of block chain canvas
        }

        synchronized (bcCore) {
            if (selectedNodeId.equals(nodeId)) {
                renewalBalance();
                nodeBlocks.add(Utils.toHexString(block.getHash()));
            }
        }
    }
    private void handleBlock(String from, String to, Block block) {
        // TODO: add Animation at node canvas
    }
    private void handleTx(String from, String to, Transaction tx) {
        // TODO: add Animation at node canvas
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

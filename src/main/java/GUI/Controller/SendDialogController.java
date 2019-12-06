package GUI.Controller;

import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.Node;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ResourceBundle;

public class SendDialogController implements Initializable {
    @FXML
    private TableView<Pair<String, String>> walletTableView;
    @FXML
    private TreeView<String> nodeTreeView;

    @FXML
    private TextField amount;
    @FXML
    private Button sendButton, cancelButton;

    private Pair<Pair<String, String>, Integer> result;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        walletTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("key"));
        walletTableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("value"));

        nodeTreeView.setShowRoot(false);
    }

    @FXML
    private void sendButtonAction() {
        if (walletTableView.getSelectionModel().getSelectedItem() == null ||
                nodeTreeView.getSelectionModel().getSelectedItem() == null ||
                amount.getText().equals("")) {
            return ;
        }

        String src = walletTableView.getSelectionModel().getSelectedItem().getKey();
        String dest = nodeTreeView.getSelectionModel().getSelectedItem().getValue();
        try {
            result = new Pair<>(new Pair<>(src, dest), Integer.parseInt(amount.getText()));
        } catch (NumberFormatException e) {
            return ;
        }

        Stage stage = (Stage)sendButton.getScene().getWindow();
        stage.close();
    }
    @FXML
    private void cancelButtonAction() {
        result = null;

        Stage stage = (Stage)sendButton.getScene().getWindow();
        stage.close();
    }

    public Pair<Pair<String, String>, Integer> getResult() { return result; }

    public void setNodeInfo(String nodeId, ArrayList<Node> nodes) {
        TreeItem<String> rootItem = new TreeItem<>("Nodes");
        rootItem.setExpanded(true);
        nodeTreeView.setRoot(rootItem);

        for (Node node : nodes) {
            TreeItem<String> nodeItem = new TreeItem<>(node.getNodeId());
            nodeItem.setExpanded(true);
            for (Wallet wallet : node.getWallets())
                nodeItem.getChildren().add(new TreeItem<>(wallet.getAddress()));
            rootItem.getChildren().add(nodeItem);

            if (node.getNodeId().equals(nodeId)) {
                HashMap<String, Integer> balances = node.getBalances();

                for (Wallet wallet : node.getWallets()) {
                    String address = wallet.getAddress();
                    int balance = balances.get(address);
                    walletTableView.getItems().add(new Pair<>(address, Integer.toString(balance)));
                }

                walletTableView.getSelectionModel().select(0);
            }
        }
        rootItem.getChildren().sort(Comparator.comparing(t -> Integer.parseInt(t.getValue())));
    }
}
package node.event;

public interface EventHandler<TEventArgs extends EventArgs> {
    public void eventReceived(Object sender, TEventArgs e);
}

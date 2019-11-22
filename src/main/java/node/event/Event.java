package node.event;

import java.util.ArrayList;

public final class Event<TEventArgs extends EventArgs> {
    // Event Handler List
    private ArrayList<EventHandler<TEventArgs>> observerList = new ArrayList<EventHandler<TEventArgs>>();

    // Raise Event
    public void raiseEvent(Object sender, TEventArgs e) {
        for(EventHandler<TEventArgs> handler : this.observerList)
            handler.eventReceived(sender, e);
    }

    // Add Event Handler
    public void addEventHandler(EventHandler<TEventArgs> handler) {
        this.observerList.add(handler);
    }

    // Remove Event Handler
    public void removeEventHandler(EventHandler<TEventArgs> handler) {
        this.observerList.remove(handler);
    }
}
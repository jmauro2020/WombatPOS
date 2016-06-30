package oast.events;

import java.util.EventObject;

public class FinalizedEvent extends EventObject {
    public FinalizedEvent (Object source) {
        super(source);
    }
}

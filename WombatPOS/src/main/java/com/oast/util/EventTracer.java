package com.oast.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class EventTracer {
    private Display display;

    private Map<Integer, String> eventMap = new Hashtable<Integer, String>();
    {
        eventMap.put (SWT.Paint, "Paint");
        eventMap.put (SWT.Selection, "Selection");
        eventMap.put (SWT.DefaultSelection, "DefaultSelection");
        eventMap.put (SWT.Dispose, "Dispose");
        eventMap.put (SWT.FocusIn, "FocusIn");
        eventMap.put (SWT.FocusOut, "FocusOut");
        eventMap.put (SWT.Hide, "Hide");
        eventMap.put (SWT.Show, "Show");
        eventMap.put (SWT.KeyDown, "KeyDown");
        eventMap.put (SWT.KeyUp, "KeyUp");
        eventMap.put (SWT.MouseDown, "MouseDown");
        eventMap.put (SWT.MouseUp, "MouseUp");
        eventMap.put (SWT.MouseDoubleClick, "MouseDoubleClick");
        eventMap.put (SWT.MouseMove, "MouseMove");
        eventMap.put (SWT.Resize, "Resize");
        eventMap.put (SWT.Move, "Move");
        eventMap.put (SWT.Close, "Close");
        eventMap.put (SWT.Activate, "Activate");
        eventMap.put (SWT.Deactivate, "Deactivate");
        eventMap.put (SWT.Iconify, "Iconify");
        eventMap.put (SWT.Deiconify, "Deiconify");
        eventMap.put (SWT.Expand, "Expand");
        eventMap.put (SWT.Collapse, "Collapse");
        eventMap.put (SWT.Modify, "Modify");
        eventMap.put (SWT.Verify, "Verify");
        eventMap.put (SWT.Help, "Help");
        eventMap.put (SWT.Arm, "Arm");
        eventMap.put (SWT.MouseExit, "MouseExit");
        eventMap.put (SWT.MouseEnter, "MouseEnter");
        eventMap.put (SWT.MouseHover, "MouseHover");
        eventMap.put (SWT.Traverse, "Traverse");
    }

    private Listener uniListener = new Listener () {
        public void handleEvent (Event event) {
            String eventName = eventMap.get(event.type);
            if (eventName != null)
                System.out.println ("Event " + eventName + " @ " + event.widget.getClass() + " " + event.widget.toString () + "\n >" + event.toString ());
        }
    };

    public void addHooks () {
        Set<Integer> eventTypes = eventMap.keySet();
        for (Integer eventType : eventTypes)
            display.addFilter(eventType, uniListener);
    }

    public void removeHooks () {
        Set<Integer> eventTypes = eventMap.keySet();
        for (Integer eventType : eventTypes)
            display.removeFilter(eventType, uniListener);
    }

    public EventTracer (Display display) {
        super();
        this.display = display;
    }
}

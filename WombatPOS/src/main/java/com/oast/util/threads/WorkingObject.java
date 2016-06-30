package oast.util.threads;

/*
 *  WorkingObject is kind of synchronization object.
 *  Use it in conjunction with FeederThread and ConsumerThread as follows
 *
 *        WorkingObject o = new WorkingObject ();
 *
 *        FeederThread feeder = new FeederThread (o) {
 *            public void run () {
 *                for (int i = 0; i < 100; i++) {
 *                    String s = "feed value " + String.valueOf (i);
 *                    this.object.put (s);
 *                    System.out.println ("Fed: " + s);
 *                }
 *            }
 *        };
 *
 *        ConsumerThread consumer = new ConsumerThread (o) {
 *            public void run () {
 *                while (true) {
 *                    String s = (String)object.get ();
 *                    System.out.println ("Consumer: " + s);
 *                }
 *            }
 *        };
 */

public class WorkingObject {
    boolean valueSet;
    Object object;
    
    boolean valueWasSet = false;

    /**
     * Flag, shown if value was put first time or not.
     * @return true, if value was put first time, false if value was not put at all.
     */
    public synchronized boolean isValueWasSet() {
        return valueWasSet;
    }
    
    public synchronized Object get () {
        if (!valueSet)
            try {
                wait ();
            } catch (InterruptedException e) {
                
            }

            valueSet = false;
            notify ();

            return object;
    }

    public synchronized void put (Object o) {
        if (valueSet)
            try {
                wait ();
            } catch (InterruptedException e) {
                
            }

            if (!isValueWasSet()) {
                valueWasSet = true;
            }
            this.object = o;
            valueSet = true;
            notify();
    }
}
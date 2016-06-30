package oast.util.threads;
/**
 * This thread`s descendant run itself during constructing.
 * 
 *
 */
public class ConsumerThread extends Thread implements Runnable {
    protected WorkingObject object;
    protected boolean stoppedThread = false;

    public ConsumerThread (WorkingObject object) {
        super("Consumer");
        this.object = object;
        start ();
    }

    public void run() {
        // TODO Auto-generated method stub
        // use get ()
    }

    public void stopThread () {
        stoppedThread = true;
    }
}

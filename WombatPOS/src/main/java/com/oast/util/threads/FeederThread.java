package oast.util.threads;

public class FeederThread extends Thread implements Runnable {
    protected WorkingObject object;
    protected boolean stoppedThread = false;

    public FeederThread (WorkingObject object) {
        super("Feeder");
        this.object = object;
        start ();
    }

    public void run () {
        // TODO use put ()
    }

    public void stopThread () {
        stoppedThread = true;
    }
}

package oast.util.threads;

import java.util.Vector;

public class ThreadPool {
    public class BusyFlag {
        protected Thread busyflag = null;
        protected int busycount = 0;

        public synchronized void getBusyFlag () {
            while (tryGetBusyFlag () == false) {
                try {
                    wait ();
                } catch (Exception e) {}
            }
        }

        public synchronized boolean tryGetBusyFlag () {
            if (busyflag == null) {
                busyflag = Thread.currentThread ();
                busycount = 1;
                return true;
            }

            if (busyflag == Thread.currentThread ()) {
                busycount++;
                return true;
            }
            return false;
        }

        public synchronized void freeBusyFlag () {
            if (getBusyFlagOwner () == Thread.currentThread ()) {
                busycount--;
                if (busycount == 0) {
                    busyflag = null;
                    notify();
                }
            }
        }

        public synchronized Thread getBusyFlagOwner () {
            return busyflag;
        }
    }

    public class CondVar {
        private BusyFlag SyncVar;

        public CondVar () {
            this (new BusyFlag ());
        }

        public CondVar(BusyFlag sv) {
            SyncVar = sv;
        }

        public void cvWait() throws InterruptedException {
            cvTimedWait(SyncVar, 0);
        }

        public void cvWait(BusyFlag sv) throws InterruptedException {
            cvTimedWait(sv, 0);
        }

        public void cvTimedWait(int millis) throws InterruptedException {
            cvTimedWait(SyncVar, millis);
        }

        public void cvTimedWait (BusyFlag sv, int millis) throws InterruptedException {
            int i = 0;
            InterruptedException errex = null;

            synchronized (this) {
                // You must own the lock in order to use this method.
                if (sv.getBusyFlagOwner () != Thread.currentThread ()) {
                    throw new IllegalMonitorStateException ("current thread not owner");
                }

                // Release the lock (completely).
                while (sv.getBusyFlagOwner () == Thread.currentThread ()) {
                    i++;
                    sv.freeBusyFlag ();
                }

                // Use wait() method.
                try {
                    if (millis == 0) {
                        wait ();
                    } else {
                        wait (millis);
                    }
                } catch (InterruptedException iex) {
                    errex = iex;
                }
            }

            // Obtain the lock (return to original state).
            for (; i > 0; i--) {
                sv.getBusyFlag ();
            }

            if (errex != null) throw errex;

            return;
        }

        public void cvSignal () {
            cvSignal (SyncVar);
        }
        public synchronized void cvSignal (BusyFlag sv) {
            // You must own the lock in order to use this method.
            if (sv.getBusyFlagOwner () != Thread.currentThread ()) {
                throw new IllegalMonitorStateException("current thread not owner");
            }
            notify ();
        }

        public void cvBroadcast () {
            cvBroadcast (SyncVar);
        }

        public synchronized void cvBroadcast (BusyFlag sv) {
            // You must own the lock in order to use this method.
            if (sv.getBusyFlagOwner () != Thread.currentThread ()) {
                throw new IllegalMonitorStateException ("current thread not owner");
            }

            notifyAll ();
        }
    }

    class ThreadPoolRequest {
        Runnable target;
        Object lock;
        ThreadPoolRequest(Runnable t, Object l) {
            target = t;
            lock = l;
        }
    }

    class ThreadPoolThread extends Thread {
        ThreadPool parent;
        volatile boolean shouldRun = true;

        ThreadPoolThread (ThreadPool parent, int i) {
            super ("ThreadPoolThread " + i);
            this.parent = parent;
        }

        public void run () {
            ThreadPoolRequest obj = null;
            while (shouldRun) {
                try {
                    parent.cvFlag.getBusyFlag ();
                    while (obj == null && shouldRun) {
                        try {
                            obj = (ThreadPoolRequest)
                            parent.objects.elementAt (0);
                            parent.objects.removeElementAt (0);
                        } catch (ArrayIndexOutOfBoundsException aiobe) {
                            obj = null;
                        } catch (ClassCastException cce) {
                            System.err.println ("Unexpected data");
                            obj = null;
                        }
                        if (obj == null) {
                            try {
                                parent.cvAvailable.cvWait ();
                            } catch (InterruptedException ie) {
                                return;
                            }
                        }
                    }
                } finally {
                    parent.cvFlag.freeBusyFlag ();
                }

                if (!shouldRun)
                    return;

                obj.target.run ();
                try {
                    parent.cvFlag.getBusyFlag ();
                    nObjects--;
                    if (nObjects == 0)
                        parent.cvEmpty.cvSignal ();
                } finally {
                    parent.cvFlag.freeBusyFlag ();
                }

                if (obj.lock != null) {
                    synchronized (obj.lock) {
                        obj.lock.notify ();
                    }
                }
                obj = null;
            }
        }
    }

    Vector objects;
    int nObjects = 0;
    CondVar cvAvailable, cvEmpty;
    BusyFlag cvFlag;
    ThreadPoolThread poolThreads [];
    boolean terminated = false;

    public ThreadPool (int n) {
        cvFlag = new BusyFlag ();
        cvAvailable = new CondVar (cvFlag);
        cvEmpty = new CondVar (cvFlag);
        objects = new Vector ();
        poolThreads = new ThreadPoolThread [n];
        for (int i = 0; i < n; i++) {
            poolThreads [i] = new ThreadPoolThread (this, i);
            poolThreads [i].start ();
        }
    }

    private void add (Runnable target, Object lock) {
        try {
            cvFlag.getBusyFlag ();
            if (terminated)
                throw new IllegalStateException ("Thread pool has shut down");
            objects.addElement (new ThreadPoolRequest (target, lock));
            nObjects++;
            cvAvailable.cvSignal ();
        } finally {
            cvFlag.freeBusyFlag ();
        }
    }

    public void addRequest (Runnable target) {
        add (target, null);
    }

    public void addRequestAndWait (Runnable target)    throws InterruptedException {
        Object lock = new Object ();
        synchronized (lock) {
            add (target, lock);
            lock.wait ();
        }
    }

    public void waitForAll (boolean terminate) throws InterruptedException {
        try {
            cvFlag.getBusyFlag ();

            while (nObjects != 0)
                cvEmpty.cvWait ();

            if (terminate) {
                for (int i = 0; i < poolThreads.length; i++)
                    poolThreads [i].shouldRun = false;
                cvAvailable.cvBroadcast ();
                terminated = true;
            }
        } finally {
            cvFlag.freeBusyFlag ();
        }
    }

    public void waitForAll () throws InterruptedException {
        waitForAll (false);
    }
}


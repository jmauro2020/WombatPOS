package com.oast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import oast.events.FinalizedEvent;

import org.eclipse.swt.widgets.Text;

public class IOStreams {
    OutputStream os;
    InputStream is;
    String type;
    static Text control;
    String user;
    String password;
    boolean stopped;
    boolean errorOccured = false;
    String buffer;

    IOStreams (InputStream is, OutputStream os, String type) {
        this.is = is;
        this.os = os;
        this.type = type;
    }

    public String readLine () {
        return buffer;
    }

    public boolean isStopped () {
        return stopped;
    }

    public boolean getErrorStatus () {
        return !errorOccured;
    }

    public void setLoginData (final String usern, final String passwordn) {
        user = usern;
        password = passwordn;
    }

    public void setOwnedControl (Text c) {
        control = c;
    }

    public void start () {
        InputStreamReader isr = new InputStreamReader (is);
        BufferedReader br = new BufferedReader (isr);
        stopped = false;

        try {
            PrintWriter output;
            String line = null;

            while (true) {
                if (!stopped) {
                    line = br.readLine ();
                    if (line != null) {
                        if (line.indexOf ("Initialization Sequence Completed") != -1) {
                            /* synchronized (control) {
                                Oast.swtf.swtFrameShell.getDisplay ().asyncExec (new Runnable () {
                                    public void run () { if (control != null) control.append ("> Initialization Sequence Completed\n"); } } );
                            } */
                            SwtFrame.notifyFinalizers (new FinalizedEvent (this));

                            // break;
                        }
                    } else
                        break;
                } else
                    break;

                buffer = line;
                synchronized (control) {
                    Oast.getSwtFrame().swtFrameShell.getDisplay ().asyncExec (new Runnable () {
                        public void run () { if (!stopped && control != null) control.append ("> " + buffer + "\n"); } } );
                }

                if (Oast.getConfigurationManager ().isDebugOpenVPN ())
                    System.out.println ("> " + line);

                if (line.indexOf ("[EHOSTUNREACH|EHOSTUNREACH]: No route to host (code=113)") != -1) {
                    Oast.showErrorMessage ("No route to host");
                    Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_ERROR);
                    Oast.getOastConnection ().setStatusMessage (Oast.CONNECTION_ERROR,
                    Oast.getOastConnection ().getStatusMessage (Oast.CONNECTION_ERROR) + ": No route to host");
                    errorOccured = true;
                    break;
                }

/*                if (line.indexOf ("Connection reset") != -1) {
                    Oast.showErrorMessage ("Connection reset");
                    Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_ERROR);
                    Oast.getOastConnection ().setStatusMessage (Oast.CONNECTION_ERROR,
                    Oast.getOastConnection ().getStatusMessage (Oast.CONNECTION_ERROR) + ": Connection reset");
                    Oast.getSwtFrame().swtFrameShell.getDisplay ().asyncExec (new Runnable () {
                        public void run () {
                            Oast.setDisconnected ();
                        }
                    });

                    errorOccured = true;
                    // break;
                }
*/
                if (line.indexOf ("AUTH: Received AUTH_FAILED control message") != -1) {
                    Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_ERROR);
                    Oast.getOastConnection ().setStatusMessage (Oast.CONNECTION_ERROR,
                    Oast.getOastConnection ().getStatusMessage (Oast.CONNECTION_ERROR) + ": Authorization failed");
                    errorOccured = true;
                    Oast.showErrorMessage ("Authentication failed. Incorrect username/password or your subscription has expired");
                    break;
                }

                if (line.indexOf ("Exiting") != -1) {
                    errorOccured = true;
                    break;
                }

                if (line.indexOf ("Need password(s)") != -1) {
                    int retries = 0;
                    boolean success = false;

                    while ((retries < 10) && (!success)) {
                        try {
                            Socket ClientSocket = new Socket ("127.0.0.1", Oast.getConfigurationManager ().getManagementPort ());
                            InputStream sin = ClientSocket.getInputStream ();
                            OutputStream sout = ClientSocket.getOutputStream ();
                            InputStreamReader insr = new InputStreamReader (sin);
                            BufferedReader reader = new BufferedReader (insr);
                            String s = null;
                            char [] input = password.toCharArray ();
                            String pw = "";

                            output = new PrintWriter (sout, true);
                            while ((s = reader.readLine ()) != null && !stopped) {
                                if (s.indexOf ("PASSWORD:Need 'Auth' username/password") != -1) {
                                    output.println ("username Auth " + user);
                                    if (reader.readLine ().indexOf ("SUCCESS") != -1)
                                        output.println ("password Auth " + password);
                                    if (reader.readLine ().indexOf ("SUCCESS") != -1)
                                        break;
                                }
                                System.out.println (s);
                            }
                            success = true;
                        } catch (IOException se) {
                            System.out.println ("Connecting to management interface... retrying.");
                            ++retries;
                            try {
                                Thread.sleep (2500);
                            } catch (InterruptedException d) { }
                        }
                    }
                }
                try {
                    Thread.sleep (100);
                } catch (InterruptedException sxc) {
                    
                }
            }
        } catch (IOException ioe) {
            // ioe.printStackTrace ();
        }
        stopped = true;
    }

    public void stop () {
        stopped = true;
    }
}

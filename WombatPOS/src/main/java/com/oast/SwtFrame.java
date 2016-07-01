package com.oast;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import com.oast.config.LoginStorage;
import com.oast.config.SwtInputBox;
import com.oast.connection.OastConnectionManager;
import oast.events.FinalizedEvent;
import oast.events.FinalizedListener;
import oast.geoip.Country;
import oast.geoip.GeoIPService;
import com.oast.util.HttpRequest;
import oast.util.threads.ConsumerThread;
import oast.util.threads.FeederThread;
import oast.util.threads.WorkingObject;
import oast.util.ui.AnimatedGif;
import oast.util.ui.widgets.SwtCustomButton;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.checks.Checker;

public class SwtFrame {
    Shell swtFrameShell;
    private UserInputCluster cluster;
    private boolean hidden;
    private static boolean showNotification;
    private int textBoxHeight = 0;
    private Display swtFrameDisp;
    private GeoIPService geoipService;
    private OastConnectionManager connectionManager = null;
    private IOStreams ios;
    private boolean interrupted;
    private boolean noHide;
    Object textBox;
    private static MessageBox messageBox;
    private Label ipLabel = null;
    private List<Checker> checkers = new ArrayList<Checker>();
    private FrameType currentFrame;
    static Vector<FinalizedListener> finalizedListeners = new Vector<FinalizedListener> ();
    
    public SwtFrame (Display swtFrameDisplay, GeoIPService geoipService, List<Checker> checkers) {
        this.swtFrameDisp = swtFrameDisplay;
        this.geoipService = geoipService;
        this.checkers = checkers;
        constructLoginFrame (swtFrameDisplay);
    }
    

    
    public void addFinalizedListener (FinalizedListener listener) {
        finalizedListeners.addElement (listener);
    }

    public void removeFinalizedListener (FinalizedListener listener) {
        finalizedListeners.removeElement (listener);
    }

    public static void notifyFinalizers (FinalizedEvent event) {
        int ls = finalizedListeners.size ();

        for (int i = 0; i < ls; i++) {
            FinalizedListener listener = (FinalizedListener) finalizedListeners.elementAt(i);
            listener.actionFinalized (event);
        }
    }

    private void delay () {
        try {
            Thread.sleep (300);
        } catch (InterruptedException e) {}
    }

    private void cleanUpFrame () {
        Control control [] = swtFrameShell.getChildren ();

        for (int i = 0; i < control.length; i++) {
            System.out.println ("Disposing " + control [i].toString ());

            // Persists trough all the frames/steps
            if (control [i].toString ().equals ("Text {}")) {
                ((Text)control [i]).setText ("");
                continue;
            }

            // Persists trough all the frames/steps
            if (control [i].toString ().indexOf ("Label {Your") != -1 ||
                    control [i].toString ().indexOf ("Label {Can\'t") != -1 ||
                    control [i].toString ().indexOf ("Label {Detecting") != -1)
                continue;

            control [i].dispose ();
        }
    }

    private Shell createShell (final Display display) {
        Shell shell = new Shell (display, SWT.DIALOG_TRIM | SWT.MIN | SWT.CLOSE);

        shell.setImage (Oast.oastResources.getImageResource ("oast-ico-16x16"));
        shell.setText ("OAST");
        shell.setSize (387, 374);

        Monitor primary = display.getPrimaryMonitor ();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();

        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;

        shell.setLocation (x, y);
        shell.addShellListener (new ShellAdapter () {
            public void shellDeactivated (ShellEvent e) {
                if (showNotification)
                    Oast.getOastConnection ().displayStatus ();
            }

            public void shellIconified (ShellEvent e) {
                if (noHide) {
                    e.doit = false;
                    return;
                }

                if (showNotification)
                    Oast.getOastConnection ().displayStatus ();
                hideShell ();
                Oast.enableTrayItemMenu ("Show oast window", true);
            }

            public void shellClosed (ShellEvent e) {
                if (!exitFrame (true))
                    e.doit = false;
            }
        });

        /* sets focus to Shell when focus comes to text output control
         * need this because:
         * 1) need red color for error message 
         * 2) when text control not enabled - cannot display red color, so keep it enabled
         * 3) when text control enabled - do not want to see text cursor inside it
         * So every time when focus comes to text output control - set it to Shell
         * (BTW SWT.NO_FOCUS did not work for Text control)
         */
        display.addFilter(SWT.FocusIn, new Listener() {
            public void handleEvent(Event event) {
                if (event.widget == getTextElement()) {
                    swtFrameShell.forceFocus();
                }
            }});
        
        return shell;
    }

    public class UserInputCluster extends Composite {
        public Group group;
        public Text userName;
        public Text userPasswd;
        public Button btnRemember;
        public Label labelUsr;
        public Label labelPwd;

        public UserInputCluster (Composite c, int style) {
            super (c, style);

            this.setLayout (new GridLayout ());
            group = new Group (this, style);
            GridLayout gl = new GridLayout ();
            gl.numColumns = 3;
            group.setLayout (gl);

            GridData gd = new GridData (GridData.HORIZONTAL_ALIGN_FILL);
            group.setLayoutData (gd);

            labelUsr = new Label (group, SWT.NORMAL);
            labelUsr.setImage (Oast.oastResources.getImageResource ("user.name"));
            labelUsr.setLayoutData (new GridData ());
            ((GridData)labelUsr.getLayoutData ()).horizontalSpan = 3;

            userName = new Text (group, SWT.SINGLE);
            userName.setTextLimit (80);
            userName.setLayoutData (new GridData (GridData.FILL_HORIZONTAL));
            userName.setFocus ();

            // Dummy labels
            // Yeh... I know it's ugly and I'm a disgusting pig (c), but as opposite to Linus I'm not proud of this :(
            Label l = new Label (group, SWT.NULL);
            l = new Label (group, SWT.NULL);

            labelPwd = new Label (group, SWT.NORMAL);
            labelPwd.setImage (Oast.oastResources.getImageResource ("password"));
            labelPwd.setLayoutData (new GridData ());
            ((GridData)labelPwd.getLayoutData ()).horizontalSpan = 3;

            userPasswd = new Text (group, SWT.SINGLE);
            userPasswd.setEchoChar ('*');
            userPasswd.setTextLimit (80);
            userPasswd.setLayoutData (new GridData (GridData.FILL_HORIZONTAL));

            btnRemember = new Button (group, SWT.CHECK);
            btnRemember.setLayoutData (new GridData ());
            ((GridData)btnRemember.getLayoutData ()).horizontalIndent = 15;

            if (Oast.getConfigurationManager ().isStoreLogData ()) {
                btnRemember.setSelection (true);
                LoginStorage ls = new LoginStorage (Oast.getConfigurationManager ());
                userName.setText (ls.retrieveUsername ());
                userPasswd.setText (ls.retrievePassword ());
            }

            btnRemember.addSelectionListener (new SelectionAdapter () {
                public void widgetSelected (SelectionEvent e) {
                    if (btnRemember.getSelection ()) {
                        int response;
                        boolean notifyChg = showNotification;

                        if (notifyChg)                // prevent notification balloons from popup
                            enableNotifications (false);
                        messageBox = new MessageBox (swtFrameShell, SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.YES | SWT.NO);
                        messageBox.setText ("Vulnerability warning");
                        messageBox.setMessage ("Storing username and password may be dangerous as this leads to potential security issues.\nProceed anyway?");
                        response = messageBox.open ();

                        if (response == SWT.NO)
                            btnRemember.setSelection (false);
                        else {
                            Oast.getConfigurationManager ().setStoreLogData (true);

                            LoginStorage ls = new LoginStorage (Oast.getConfigurationManager ());

                            ls.storeUsername (userName.getText ());
                            ls.storePassword (userPasswd.getText ());
                            ls.storeLoginData ();
                        }

                        if (notifyChg)
                            enableNotifications (true);
                    }
                }
            });

            Label label = new Label (group, SWT.NORMAL);
            label.setImage (Oast.oastResources.getImageResource ("save"));
        }
    }

    /*
     * Construct an instance of the user input cluster
     */
    private UserInputCluster showUserInput (Shell shell) {
        return (new UserInputCluster (shell, SWT.NORMAL));
    }

    private Listener keyFilter;

    private void constructLoginFrame (final Display display) {
        currentFrame = FrameType.LOGIN_FRAME;
        
        if (swtFrameShell == null) {
            swtFrameShell = createShell (display);
            ipLabel = new Label (swtFrameShell, SWT.NORMAL);
            ipLabel.setLayoutData (new GridData (GridData.FILL_HORIZONTAL));
            createTextBoxBegin (display, swtFrameShell);
            swtFrameShell.setLayout (new GridLayout ());
            swtFrameShell.setVisible (true);
            buildUpMenu (swtFrameShell);
        } else {
            cleanUpFrame ();
        }

        cleanupRunnerThreads ();
        
//        Comment this to allow red color usage
//        if (Oast.getConfigurationManager ().isUseCustomWidgets ())
//            ((SwtFancyText)textBox).getContainedControl ().setEnabled (false);
//        else
//            ((Text)textBox).setEnabled (false);

        swtFrameShell.setSize (387, 374);
        ipLabel.setText ("Detecting your real IP...");
        setMenuItemDisabled ();
        cluster = showUserInput (swtFrameShell);

        keyFilter = new Listener () {
            public void handleEvent (Event event) {
                if (event.keyCode != 13)
                    return;

                handleConnect(display);
            }
        };

        swtFrameShell.getDisplay ().addFilter (SWT.KeyDown, keyFilter);

        cluster.group.pack ();
        cluster.pack ();
        cluster.setLayoutData (new GridData (GridData.HORIZONTAL_ALIGN_CENTER));

        if (Oast.getConfigurationManager ().isDebugConsole ())
            System.out.println ("cluster.getBounds ().height = " + cluster.getBounds ().height +
                    "\ncluster.btnRemember.getBounds ().height = " + cluster.btnRemember.getBounds ().height +
                    "\ncluster.userPasswd.getBounds ().height = " + cluster.userPasswd.getBounds ().height +
                    "\ncluster.labelUsr.getBounds ().height = " + cluster.labelUsr.getBounds ().height +
                    "\ncluster.getBorderWidth = " + cluster.getBorderWidth ());

        int sampleHeight = middle (cluster.btnRemember.getBounds ().height, cluster.labelUsr.getBounds ().height, cluster.userPasswd.getBounds ().height);

        if (Oast.getConfigurationManager ().isDebugConsole ())
            System.out.println ("sampleHeight = " + sampleHeight);

        cluster.group.setLayoutData (new GridData (355, sampleHeight * 5));

        if (Oast.getConfigurationManager ().isDebugConsole ()) {
            System.out.println ("swtFrameShell = " + swtFrameShell.getBounds ().height);
        }

        Composite composite = new Composite (swtFrameShell, SWT.NONE);
        composite.setLayout (new RowLayout ());
        ((RowLayout)composite.getLayout ()).spacing = 6;
        ((RowLayout)composite.getLayout ()).marginLeft = 0;
        ((RowLayout)composite.getLayout ()).marginRight = 0;

        final SwtCustomButton swtBtnOk = new SwtCustomButton (composite,
        Oast.oastResources.getImageResource ("ok.norm"),
        Oast.oastResources.getImageResource ("ok.norm"), Oast.oastResources.getImageResource ("ok.rol"));
        swtBtnOk.setLayoutData (new RowData ());
        swtBtnOk.setVisible (true);
         
        final SwtCustomButton swtBtnSET = new SwtCustomButton (composite,
        Oast.oastResources.getImageResource ("set"),
        Oast.oastResources.getImageResource ("set"), Oast.oastResources.getImageResource ("setr"));
        swtBtnSET.setLayoutData (new RowData ());
        swtBtnSET.setVisible (true);
        
        final SwtCustomButton swtBtnHIDE = new SwtCustomButton (composite,
        Oast.oastResources.getImageResource ("hide"),
        Oast.oastResources.getImageResource ("hide"), Oast.oastResources.getImageResource ("hider"));
        swtBtnHIDE.setLayoutData (new RowData ());
        swtBtnHIDE.setVisible (true);

        final SwtCustomButton swtBtnCLOSE = new SwtCustomButton (composite,
        Oast.oastResources.getImageResource ("close"),
        Oast.oastResources.getImageResource ("close"), Oast.oastResources.getImageResource ("closer"));
        swtBtnCLOSE.setLayoutData (new RowData ());
        swtBtnCLOSE.setVisible (true);
        
        if (Oast.getConfigurationManager ().isDebugConsole ())
            System.out.println ("swtBtnOk.getLocation ().x = " + swtBtnOk.getLocation ().x + "\nswtBtnOk.getLocation ().y = " + swtBtnOk.getLocation ().y);

        swtBtnOk.addMouseListener (new MouseAdapter () {
            public void mouseDown (MouseEvent e) {
                handleConnect(display);
            }
        });

        swtBtnSET.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                handleSettingsDialog();
            }
        });
        
        swtBtnHIDE.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                handleHideItem();
            }
        });
        
        swtBtnCLOSE.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
                handleQuitItem();
            }
        });
        
        swtFrameShell.pack ();
        setIPAddressLine ();
        swtFrameShell.open ();
        createTextBoxEnd (display, swtFrameShell);
        
        runCheckers();
    }

    private void handleConnect(final Display display) {
        final String pwd = cluster.userPasswd.getText ();
        final String usr = cluster.userName.getText ();
        final boolean needAuth = cluster.userPasswd.isEnabled() && cluster.userName.isEnabled();

        if (needAuth && (usr.length () == 0 || pwd.length () == 0)) {
            boolean notifyChg = showNotification;

            if (notifyChg)                // prevent notification balloons from popup
                enableNotifications (false);

            messageBox = new MessageBox (swtFrameShell, SWT.ICON_ERROR | SWT.APPLICATION_MODAL | SWT.OK);
            noHide = true;
            messageBox.setText ("Error: missing login data");
            messageBox.setMessage ("Neither user name nor password were provided.\nPlease enter your user name and password in order to continue");
            messageBox.open ();
            noHide = false;

            if (notifyChg)
                enableNotifications (true);
            return;
        }

        if (Oast.getConfigurationManager ().isStoreLogData ()) {
            LoginStorage ls = new LoginStorage (Oast.getConfigurationManager ());

            ls.storeUsername (usr);
            ls.storePassword (pwd);
            ls.storeLoginData ();
        }

        swtFrameShell.getDisplay ().removeFilter (SWT.KeyDown, keyFilter);
        constructConnectionFrame (display, usr, pwd, needAuth);
    }
    
    void runCheckers() {
        getTextElement().setText("");
        for (Checker checker: checkers) {
            try {
                boolean success = checker.validate(this);

                if (! success) {
                    String message = checker.getMessage();
                    if (message != null) {
                        appendErrorMessage(message);
                    }
                }

                if (! checker.coninueValidation()) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void appendErrorMessage(String message) {
        final Text text = getTextElement();
        text.setForeground(new Color(text.getDisplay(), 255, 0, 0));
        text.append(message + "\n");
    }

    private Text getTextElement() {
        final Text text;
        if (textBox instanceof Text) {
            text = (Text) textBox;
        } else if (textBox instanceof SwtFancyText) {
            text = ((SwtFancyText) textBox).getContainedControl();
        } else {
            throw new IllegalStateException("Unknown object as text field");
        }
        return text;
    }
    
    public UserInputCluster getUserInputCluster() {
        return cluster;
    }
    
    public void showLoginFrame () {
        constructLoginFrame (swtFrameDisp);
    }

    private void constructConnectionFrame (final Display display, final String usr, final String pwd, final boolean needAuth) {
        currentFrame = FrameType.CONNECTION_FRAME;
        
        cleanUpFrame ();
        swtFrameShell.setSize (387, 374);

        if (Oast.getConfigurationManager ().isUseCustomWidgets ()) {
            ((SwtFancyText)textBox).getContainedControl ().setEnabled (true);
            ((SwtFancyText)textBox).getContainedControl ().setForeground(new Color(display, 0, 0, 0));
        } else {
            ((Text)textBox).setEnabled (true);
            ((Text)textBox).setForeground(new Color(display, 0, 0, 0));
        }

        final Label label = new Label (swtFrameShell, SWT.NORMAL);
        label.setImage (Oast.oastResources.getImageResource ("conn.progress"));

        label.setSize (swtFrameShell.getBounds ().width - 10, 15);
        label.setLocation (5, textBoxHeight + 20);

        int tbh, tbw;

        if (Oast.getConfigurationManager ().isUseCustomWidgets ()) {
            tbh = ((SwtFancyText)textBox).getContainedControl ().getBounds().height;
            tbw = ((SwtFancyText)textBox).getContainedControl ().getBounds().width;
        } else {
            tbh = ((Text)textBox).getBounds ().height;
            tbw = ((Text)textBox).getBounds ().width;
        }

        final Composite container = new Composite (swtFrameShell, SWT.NORMAL);
        container.setLayoutData (new GridData (tbw, 32));

        final AnimatedGif ag = new AnimatedGif ();

        Composite composite = new Composite (swtFrameShell, SWT.NONE);
        composite.setLayout (new RowLayout ());

        final SwtCustomButton swtBtnHide = new SwtCustomButton (composite,
                Oast.oastResources.getImageResource ("hide.norm"),
                Oast.oastResources.getImageResource ("hide.norm"),
                Oast.oastResources.getImageResource ("hide.roll"));
        swtBtnHide.setLayoutData (new RowData ());
        swtBtnHide.setVisible (true);

        final SwtCustomButton swtBtnDisconn = new SwtCustomButton (composite,
                Oast.oastResources.getImageResource ("abort"),
                Oast.oastResources.getImageResource ("abort"),
                Oast.oastResources.getImageResource ("abort.roll"));
        swtBtnDisconn.setLayoutData (new RowData ());
        swtBtnDisconn.setVisible (true);
        ((RowLayout)composite.getLayout ()).marginLeft = 0;
        ((RowLayout)composite.getLayout ()).marginRight = 0;        
        ((RowLayout)composite.getLayout ()).spacing = 7;

        final OastConnectionManager ocm = new OastConnectionManager (Oast.getConfigurationManager ());
        final FinalizedListener finalized = new FinalizedListener () {
            public void actionFinalized (FinalizedEvent e) {
                if (!ocm.getErrorStatus ()) {
                    cleanupRunnerThreads ();

                    Thread t;

                    // Cleanup before we start. It may be possible, that we got here from
                    // IOStreams in case of connection break, when these threads should still
                    // run.
                    if (Oast.getOastThreadPool ().get ("Consumer") != null) {
                        t = (Thread)Oast.getOastThreadPool ().get ("Consumer");
                        if (t.isAlive ()) {
                            System.out.println ("Reaping UI threads: stopThread() called for thread \"Consumer\"");
                            t.stop ();
                        }
                    }

                    if (Oast.getOastThreadPool ().get ("Feeder") != null) {
                        t = (Thread)Oast.getOastThreadPool ().get ("Feeder");
                        if (t.isAlive ()) {
                            System.out.println ("Reaping UI threads: stopThread() called for thread \"Feeder\"");
                            t.stop ();
                        }
                    }

                    Oast.setAliveConnection (true);

                    // OMG. This makes me really sick...
                    //  ,  .   -  ...
                    //    ?   ?
                    asyncSetLabelText ("Detecting your faked IP...");

                    final WorkingObject o = new WorkingObject ();
 
                    // StShadow >> run thread non at ui = we are not interesting in ui display
                    Thread nonUiThread = new Thread ("UI_thread") {
                        public void run () {
                            FeederThread feeder = new FeederThread (o) {
                                public void run () {
                                    Oast.getOastThreadPool ().put ("Feeder", this);

                                    while (!this.stoppedThread) { 
                                        int fackedDelay = Oast.getConfigurationManager().getFackedIpDelay() * 1000;
                                        String outStatusMEssage = "Thread feeder: sleeping for "+ fackedDelay / 1000 + " seconds";
                                        String httpUrl = Oast.getConfigurationManager().getIpUrl();
                                        String response = null;

                                        asyncSetLabelText ("Detecting your faked IP...");
                                        HttpRequest request = new HttpRequest (httpUrl); // old url was "http://www.whatismyip.com/automation/n09230945.asp"
                                        response = request.doRequest ();
                                        System.out.println ("Thread feeder: alive, issuing IP discovery request");
                                        this.object.put (response);
                                        try {
                                            System.out.println (outStatusMEssage);
                                            sleep (fackedDelay);
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                }
                            };

                            ConsumerThread uiUpdater = new ConsumerThread (o) {
                                public void run () {
                                    Oast.getOastThreadPool ().put ("Consumer", this);
                                    
                                    while (!this.stoppedThread) { 
                                        String result = (String)this.object.get ();
                                        System.out.println ("Thread consumer: alive, updating IP");
                                        if (result != null) {
                                            asyncSetLabelText ("Your faked IP: " + result + " (" + getCountryTextByIP(result) + ")");

                                            Oast.getOastConnection ().setStatusMessage (Oast.CONNECTION_OK,    "Your faked IP:\n" + result);
                                            Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_OK);

                                            if (showNotification)
                                                Oast.getOastConnection ().displayStatus ();

                                            asyncSetToolTipText ("Online\n" + "Your IP: " + result + "\nCountry: " + getCountryTextByIP(result));
                                        } else {
                                            asyncSetLabelText ("Can\'t detect IP. Check your connection");
                                            // until connection will be established again - turn ui to offline mode
                                            Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_ERROR);
                                            asyncSetToolTipText ("OAST OFFLINE");
                                            if (showNotification)
                                                Oast.getOastConnection ().displayStatus ();
                                            asyncEnableTrayItemMenu ("Disconnect", false);
                                            Oast.setDisconnected();
                                            asyncSetConnectionProgressImage(label);
                                        }
                                    }
                                }
                            };
                        }
                    };
                    nonUiThread.start();
                    // St.Shadow >> This code is terrible. Sorry :( 
                    // I promise rewrite it when I`ll be in good mood
                    // StShadow >> now it is better, but not perfect
                    
                    // StSh >> also do not need ui until...
                    new Thread( new Runnable() {
                    	public void run() {
                    		// just wait, when we detect faked ip first time.
                    		while (!o.isValueWasSet()) {
                    			try {
                    				Thread.sleep(50);
                    			} catch (InterruptedException e) {
                    				e.printStackTrace();
                    			}
                    		}
                    		// StSh >> ... we want update ui
                    		swtFrameShell.getDisplay ().asyncExec(new Runnable() {
								public void run() {
									Oast.getOastConnection().setCurrentStatus(
											Oast.CONNECTION_OK);
									Oast.enableTrayItemMenu("Disconnect", true);
									Image img = Oast.oastResources.getImageResource("conn.ok");
									if (img != null) {
										label.setSize(img.getBounds().width, 13);
										label.setImage(img);
									}
									//ag.stop ();
									ag.play(container, "ui/progress.done.gif");
									Oast.setConnected();
								}
							});
                    	}
                    }).start();
                }
            }
        };

        addFinalizedListener (finalized);

        connectionManager = ocm;
        ocm.connect ();
        ag.play (container, "ui/progress.gif");

        if (!(swtFrameShell.getDisplay () == null || swtFrameShell.getDisplay ().isDisposed ())) {
            Thread connector = new Thread ("Connector") {
                public void run () {
                    ios = new IOStreams (ocm.getInputStream(), ocm.getOutputStream(), "NET");
                    ios.setLoginData (usr, pwd);

                    // grace timeout before stuff gets constructed, just to work around some
                    // arcane UI flaws on Linux when using custom widget SwtFancyText
                    delay ();

                    if (Oast.getConfigurationManager ().isUseCustomWidgets ()) {
                        ios.setOwnedControl (((SwtFancyText)textBox).getContainedControl ());
                    } else {
                        ios.setOwnedControl ((Text)textBox);
                    }
                    ios.start ();
                }
            };

            connector.setDaemon (true);
            connector.start ();
        }

        swtBtnDisconn.addMouseListener (new MouseAdapter () {
            public void mouseDown (MouseEvent e) {
                Oast.setAliveConnection (false);
                interrupted = true;

                if (!exitFrame (false)) {
                    Oast.setAliveConnection (true);
                    interrupted = false;
                    return;
                }

                Oast.enableTrayItemMenu ("Disconnect", false);
                removeFinalizedListener (finalized);

                Thread t;
                if (Oast.getOastThreadPool ().get ("Consumer") != null) {
                    t = (Thread)Oast.getOastThreadPool ().get ("Consumer");
                    if (t.isAlive ()) {
                        System.out.println ("Reaping UI threads: stopThread() called for thread \"Consumer\"");
                        t.stop ();
                    }
                }

                if (Oast.getOastThreadPool ().get ("Feeder") != null) {
                    t = (Thread)Oast.getOastThreadPool ().get ("Feeder");
                    if (t.isAlive ()) {
                        System.out.println ("Reaping UI threads: stopThread() called for thread \"Feeder\"");
                        t.stop ();
                    }
                }

//                ag.stop ();
//                ag.image.dispose ();
                Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_INFO);
                asyncSetToolTipText ("OAST OFFLINE");
                constructLoginFrame (display);
                setMenuItemDisabled ();
                Oast.setDisconnected ();
                interrupted = false;        // clear interruption flag
            }
        });

        swtBtnDisconn.addDisposeListener (new DisposeListener () {
            public void widgetDisposed (DisposeEvent e) {
                removeFinalizedListener (finalized);
            }
        });
/*                // to avoid "spurious" progress bars we link here swtBtnDisconn and AnimatedGif
                // to destroy the latter upon frame cleanup
                
                ag.stop ();
                ag.image.dispose ();
            }
        });
*/
        swtBtnHide.addMouseListener (new MouseAdapter () {
            public void mouseDown (MouseEvent e) {
                hideShell ();
                Oast.enableTrayItemMenu ("Show oast window", true);
            }
        });

        swtFrameShell.pack ();
        swtFrameShell.open ();
    }


    public void exitOast (boolean exit) {
        if (connectionManager != null) {
            if (connectionManager.getProcess () != null) {
                ios.stop ();
                connectionManager.getProcess ().destroy ();
            }
        }

        if (exit) {
            Oast.item.dispose ();
            System.exit (0);
        }
    }

    // return false if exit was cancelled
    public boolean exitFrame(boolean exitApp) {
        int response;
        boolean notifyChg = showNotification;

        if (!Oast.getConfigurationManager ().isWarnOnExit ()) {
            exitOast (exitApp);
            return true;
        }

        if (notifyChg)                // prevent notification balloons from popup
            enableNotifications (false);

        String msg; 

        if (!Oast.isAliveConnection ())
            msg = "Exit oast?";
        else
            msg = "If you exit oast all active connections will be closed.\nDo you want to continue?";
        if (!Oast.isAliveConnection () && interrupted) {
            msg = "Interrupt connection?";
        }

        messageBox = new MessageBox (swtFrameShell, SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.YES | SWT.NO);
        noHide = true;
        messageBox.setText ("Warning");
        messageBox.setMessage (msg);
        response = messageBox.open ();
        noHide = false;

        if (response == SWT.NO)
            return false;

        Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_INFO);
        asyncSetToolTipText ("OAST OFFLINE");

        if (notifyChg)
            enableNotifications (true);
        exitOast (exitApp);

        return true;
    }

    private void buildUpMenu (Shell shell) {
        final Menu menu = new Menu (shell, SWT.BAR);

        shell.setMenuBar (menu);

        MenuItem fileItem = new MenuItem (menu, SWT.CASCADE);
        fileItem.setText ("&File");
        fileItem.setAccelerator (SWT.MOD1 + 'F');

        Menu subMenu = new Menu (shell, SWT.DROP_DOWN);
        fileItem.setMenu (subMenu);

        MenuItem item = new MenuItem (subMenu, SWT.PUSH);
        item.setText ("&Hide\tCtrl+H");
        item.setAccelerator (SWT.CTRL + 'H');

        item.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                handleHideItem();
            }
        });

        item = new MenuItem (subMenu, SWT.SEPARATOR);
        item = new MenuItem (subMenu, SWT.PUSH);
        item.setText ("Quit\tAlt+F4");
        item.setAccelerator (SWT.MOD1 + SWT.F4);

        item.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                handleQuitItem();
            }
        });

        MenuItem optsItem = new MenuItem (menu, SWT.CASCADE);
        optsItem.setText ("&Options");
        optsItem.setAccelerator (SWT.MOD1 + 'O');

        Menu opts_subMenu = new Menu (shell, SWT.DROP_DOWN);
        optsItem.setMenu (opts_subMenu);

        MenuItem opts_item;

        opts_item = new MenuItem (opts_subMenu, SWT.PUSH);
        opts_item.setText ("&Settings");

        opts_item.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                handleSettingsDialog();
            }
        });

        MenuItem helpItem = new MenuItem (menu, SWT.CASCADE);
        helpItem.setText ("&Help");
        Menu help_subMenu = new Menu (shell, SWT.DROP_DOWN);
        helpItem.setMenu (help_subMenu);

        MenuItem help_item = new MenuItem (help_subMenu, SWT.PUSH);
        help_item.setText ("About");
        help_item.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                enableNotifications (false);    // Don't be noisy, when main window gets deactivated
                                                // while About dialog is displayed

                SwtAboutDialog aboutDialog = new SwtAboutDialog (swtFrameShell);
                aboutDialog.getContainedShell ().addDisposeListener (new DisposeListener () {
                    public void widgetDisposed (DisposeEvent e) {
                        // enable notifications only upon aboutDialog destroying
                        if (Oast.getConfigurationManager ().isShowNotifications ())
                            enableNotifications (true);
                    }
                });

                aboutDialog.getContainedShell ().update ();
            }
        });

        menu.setVisible (true);
    }

    //  action for "Quit" menu item or button
    private void handleQuitItem() {
        exitFrame (true);        
    }
    
    // action for "Hide" menu item or button
    private void handleHideItem() {
        hideShell ();
        Oast.enableTrayItemMenu ("Show oast window", true);
    }
    
    // action for "Settings" menu item or button
    private void handleSettingsDialog() {
        enableNotifications (false);
        SwtInputBox swti = new SwtInputBox (swtFrameDisp);
        swti.getContainedShell ().addDisposeListener (new DisposeListener () {
            public void widgetDisposed (DisposeEvent e) {
                // enable notifications only upon aboutDialog destroying
                if (Oast.getConfigurationManager ().isShowNotifications ())
                    enableNotifications (true);
                if (FrameType.LOGIN_FRAME == currentFrame) {
                    runCheckers();
                }
            }
        });
        swti.getContainedShell ().update ();
    }
    
    public Object getTextBox (final Shell shell, final Display display, boolean useExtWidget) {
        Object text;
        Text t;

        if (useExtWidget) {
            boolean gradient = Oast.getConfigurationManager ().isUseGradient (); 

            if (Oast.getConfigurationManager ().isDebugConsole ())
                System.out.println ("swtFancyTextUseGradient: " + gradient);
            SwtFancyText swtFText = new SwtFancyText (shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.H_SCROLL | SWT.NO_FOCUS,
                Oast.getOastResources().getImageResourceAsImageData("back.anonymator"), true, true, gradient);
            swtFText.getContainedControl ().setForeground (new Color (swtFText.getContainedControl ().getDisplay (), new RGB (1, 1, 1)));
            t = swtFText.getContainedControl ();
            text = swtFText;
        } else {
            Text commText = new Text (shell, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.NO_FOCUS);
            t = commText;
            t.setEditable (false);
            text = t;
        }

        t.setLayoutData (new GridData (350, 150));

        return text;
    }

    private void createTextBoxBegin (final Display display, final Shell shell) {
        if (Oast.getConfigurationManager ().isUseCustomWidgets ()) {
            textBox = getTextBox (shell, display, true);
            ((SwtFancyText)textBox).getContainedControl ().setVisible (true);
        } else {
            textBox = getTextBox (shell, display, false);
            ((Text)textBox).setVisible (true);
        }
    }

    private void createTextBoxEnd (final Display display, final Shell shell) {
        if (Oast.getConfigurationManager ().isUseCustomWidgets ()) {
            ((SwtFancyText)textBox).reScale ();
            ((SwtFancyText)textBox).getContainedControl ().redraw ();
            textBoxHeight = ((SwtFancyText)textBox).getContainedControl ().getBounds ().height;
        } else
            textBoxHeight = ((Text)textBox).getBounds ().height;
    }

    public boolean isVisible () {
        return !hidden;
    }

    public void setVisible (boolean visible) {
        swtFrameShell.setVisible (visible);
        hidden = !visible;
    }

    public static void enableNotifications (boolean enabled) {
        showNotification = enabled;
    }

    public int max (int a, int b) {
        if (a > b)
            return a;
        else
            return b;
    }

    public int min (int a, int b) {
        if (a < b)
            return a;
        else
            return b;
    }

    public int middle (int a, int b, int c) {
        if (a == 0)
            return min (b, c);
        if (b == 0)
            return min (a, c);
        if (c == 0)
            return min (a, b);
        return min (max (a, b), c);
    }

    public void hideShell () {
        swtFrameShell.setVisible (false);
        hidden = true;
    }

    public void setMenuItemDisabled () {
        Oast.enableTrayItemMenu (Oast.SHOW_OAST_WINDOW_TEXT, false);
        Oast.showWindowItemEnabled = false;
    }

    public synchronized Label getIPLabel () {
        return ipLabel;
    }

    public void asyncSetLabelText (final String s) {
        swtFrameShell.getDisplay ().asyncExec (new Runnable () {
            public void run () {
                getIPLabel ().setText (s);
            }
        });
    }

    public void asyncSetToolTipText (final String s) {
        swtFrameShell.getDisplay ().asyncExec (new Runnable () {
            public void run () {
                Oast.getTrayItem ().setToolTipText (s);
            }
        });
    }

    public void asyncEnableTrayItemMenu (final String mItem, final boolean state) {
        swtFrameShell.getDisplay ().asyncExec (new Runnable () {
            public void run () {
                Oast.enableTrayItemMenu (mItem, state);
            }
        });
    }
    
    public void asyncSetConnectionProgressImage(final Label label) {
        swtFrameShell.getDisplay().asyncExec(new Runnable (){
           public void run() {
               label.setImage (Oast.oastResources.getImageResource ("conn.progress"));
           }
        });
    }

    private static boolean stopRunner;

    private void cleanupRunnerThreads () {
        Thread t;

        if (Oast.getOastThreadPool ().get ("Runner_Consumer") != null) {
            t = (Thread)Oast.getOastThreadPool ().get ("Runner_Consumer");
            if (t.isAlive ()) {
                System.out.println ("Reaping UI threads: stopThread() called for thread \"Runner_Consumer\"");
                t.stop ();
            }
            Oast.getOastThreadPool ().remove ("Runner_Consumer");
        }

        if (Oast.getOastThreadPool ().get ("Runner_Feeder") != null) {
            t = (Thread)Oast.getOastThreadPool ().get ("Runner_Feeder");
            if (t.isAlive ()) {
                System.out.println ("Reaping UI threads: stopThread() called for thread \"Runner_Feeder\"");
                t.stop ();
            }
            Oast.getOastThreadPool ().remove ("Runner_Feeder");
        }

        if (Oast.getOastThreadPool ().get ("IP_runner") != null) {
            t = (Thread)Oast.getOastThreadPool ().get ("IP_runner");
            if (t.isAlive ()) {
                System.out.println ("Reaping UI threads: stopThread() called for thread \"IP_runner\"");
                t.stop ();
            }
            Oast.getOastThreadPool ().remove ("IP_runner");
        }
    }

    private void setIPAddressLine () {
        cleanupRunnerThreads ();

        Thread runner = new Thread ("IP_runner") {
            public void run () {
                stopRunner = false;
                Oast.getOastThreadPool ().put ("IP_runner", this);

                while (!stopRunner) {
                    asyncSetLabelText("Detecting your real IP...");
                    int realDelay = Oast.getConfigurationManager().getRealIpDelay() * 1000;
                    String outMessage = "IP_runner: sleeping for " + realDelay / 1000  + " seconds";
                    final String httpUrl = Oast.getConfigurationManager().getIpUrl();
                    WorkingObject object = new WorkingObject ();

                    FeederThread f = new FeederThread (object) {
                        public void run () {
                            String response = null;

                            Oast.getOastThreadPool ().put ("Runner_Feeder", this);
                            HttpRequest request = new HttpRequest (httpUrl); //"http://www.whatismyip.com/automation/n09230945.asp"
                            response = request.doRequest ();
                            this.object.put (response);
                        }
                    };

                    ConsumerThread c = new ConsumerThread (object) {
                        public void run () {
                            Oast.getOastThreadPool ().put ("Runner_Consumer", this);
                            String result = (String)this.object.get ();
                            System.out.println ("Unhidden IP " + result);

                            if (result == null) {
                                delay();
                                asyncSetLabelText ("Can\'t detect IP. Check your connection"); }
                            else {
                                asyncSetLabelText ("Your real IP: " + result + " " + "(" + getCountryTextByIP(result) + ")");
                                // TODO StSh quick fix
                                //stopRunner = true;
                            }
                        }
                    };

                    try {
                        System.out.println (outMessage);
                        Thread.sleep(realDelay);
                    } catch (InterruptedException ex) { }
                }

                cleanupRunnerThreads ();
            }
        };

        runner.start ();
    }
    
    public OastConnectionManager getConnectionManager () {
        return connectionManager;
    }
    
    private String getCountryTextByIP(String ip) {
        Country country = geoipService.getCountryByIP(ip);
        if (country == null) {
            return "(Country unavailable)";
        } else {
            return country.getDisplayName(Locale.US);
        }
    }
    
    private enum FrameType {
        LOGIN_FRAME, CONNECTION_FRAME
    }
}

package com.oast;import java.io.IOException;import java.util.ArrayList;import java.util.Hashtable;import java.util.List;import com.oast.config.OastConfigurationManager;import com.oast.connection.OastConnectionStatus;import oast.geoip.GeoIPService;import oast.geoip.GeoIPServiceImpl;import com.oast.util.EventTracer;import com.oast.util.ResourceContainer;import org.eclipse.swt.SWT;import org.eclipse.swt.events.MenuDetectEvent;import org.eclipse.swt.events.MenuDetectListener;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.graphics.Point;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Menu;import org.eclipse.swt.widgets.MenuItem;import org.eclipse.swt.widgets.MessageBox;import org.eclipse.swt.widgets.Shell;import org.eclipse.swt.widgets.Text;import org.eclipse.swt.widgets.ToolTip;import org.eclipse.swt.widgets.Tray;import org.eclipse.swt.widgets.TrayItem;import com.checks.Checker;import com.checks.OpenVPNAvailableChecker;import com.checks.OpenVPNConfigChecker;import com.checks.UserPassConfChecker;/*   #######################      #######################      #######################      #######################     #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      #######################      ##                   ##      ##                   ##      ##                                     ### ##                   ##      #######################      #######################                ### ##                   ##      ##                   ##                           ##                ###                #######################      ##                   ##      #######################                ###  2008-2009 f0x, chlen.nigera 2009-present st.shadow, chlen.nigera */public class Oast {    public static final String SHOW_OAST_WINDOW_TEXT = "Show oast window";    public static final int WINDOWS = 0;    public static final int UNIX = 1;    public static final int MACOS = 2;    public static final int CONNECTION_OK = 0;    public static final int CONNECTION_ERROR = 1;    public static final int CONNECTION_INFO = 2;        public static final int UNDEFINED_VALUE = -17;    static final String Version = "2.4";    private static SwtFrame swtf = null;    ToolTip tip;    static OastConfigurationManager oastConfMgr;    static ResourceContainer oastResources;    static TrayItem item;    static private final Display display = new Display ();    public static final int DEFAULT_DELAY = 60;    static Shell shell;    static Menu menu;    public static boolean showWindowItemEnabled;    private static boolean aliveConnection;    private static OastConnectionStatus ocs;    static private EventTracer et = null;    private static Hashtable<String, Object> oastThreads;    private static GeoIPService geoipService;    public static final String getVersion () {        return Version;    }    public synchronized static OastConnectionStatus getOastConnection () {        return ocs;    }    public static boolean isAliveConnection () {        return aliveConnection;    }    public static void setAliveConnection (final boolean aliveConn) {        aliveConnection = aliveConn;    }    private MenuDetectListener getMenuDetectListener (final Menu menu) {        return new MenuDetectListener () {            public void menuDetected (MenuDetectEvent event) {                processEvent (display, menu);            }        };    }    public static TrayItem getTrayItem () {        return item;    }        public static ResourceContainer getOastResources() {        return oastResources;    }    private static void initSwtFrame() {        swtf = new SwtFrame(display, geoipService, getCheckersForSwtFrame());                if (getConfigurationManager().isUseCustomWidgets())            ((SwtFancyText)swtf.textBox).setBackground(oastResources.getImageResourceAsImageData("back.anonymator"));        else            ((Text)swtf.textBox).setBackgroundImage(oastResources.getImageResource("back.anonymator"));        setAliveConnection (false);    }                static boolean isSwtFrameInitialized() {        return (swtf != null);    }        static SwtFrame getSwtFrame() {        if (swtf == null) {            initSwtFrame();        }        return swtf;    }        private void handleShowOastCommand () {        SwtFrame swtFrame = getSwtFrame();        if (!swtFrame.isVisible()) {            swtFrame.setVisible(true);            swtFrame.swtFrameShell.setMinimized(false);            swtFrame.swtFrameShell.forceActive();            swtFrame.swtFrameShell.forceFocus();        }        enableTrayItemMenu (SHOW_OAST_WINDOW_TEXT, false);    }        private void initTray (final Shell shell, Tray tray) {        item = new TrayItem (tray, SWT.NONE);        setDisconnected ();        menu = new Menu (shell, SWT.POP_UP);        MenuItem menuItem = new MenuItem (menu, SWT.PUSH);        menuItem.setText ("Disconnect");        menuItem.addSelectionListener (new SelectionAdapter () {            public void widgetSelected (SelectionEvent e) {                breakConnection ();            }        });        menuItem.setEnabled (false);        menuItem = new MenuItem (menu, SWT.SEPARATOR);        // Restore minimized/hidden window        menuItem = new MenuItem (menu, SWT.PUSH);        menuItem.setText (SHOW_OAST_WINDOW_TEXT);        menuItem.addSelectionListener (new SelectionAdapter () {            public void widgetSelected (SelectionEvent e) {                handleShowOastCommand ();            }        });        menuItem = new MenuItem (menu, SWT.SEPARATOR);        menuItem = new MenuItem (menu, SWT.PUSH);        menuItem.setText ("Quit");        menuItem.addSelectionListener(new SelectionAdapter() {                        public void widgetSelected(SelectionEvent e) {                if (isSwtFrameInitialized()) {                    SwtFrame swtFrame = getSwtFrame();                    if (!swtFrame.exitFrame(true))                        return;                    swtFrame.swtFrameShell.dispose();                }                item.dispose();                System.exit(0);            }                    });        item.addListener (SWT.DefaultSelection, new Listener () {                        public void handleEvent (Event event) {                if (isSwtFrameInitialized() && getSwtFrame().isVisible()) {                    getSwtFrame().hideShell ();                    enableTrayItemMenu (SHOW_OAST_WINDOW_TEXT, true);                } else {                    handleShowOastCommand ();                }            }                    });        item.addMenuDetectListener (getMenuDetectListener (menu));        enableTrayItemMenu (SHOW_OAST_WINDOW_TEXT, true);    }    public void showTray () {        shell = new Shell (display);        Tray tray = display.getSystemTray ();        if (tray != null)            initTray (shell, tray);        if (oastConfMgr.isStartOpen ()) {            SwtFrame swtFrame = getSwtFrame();            if (getConfigurationManager ().isUseCustomWidgets ())                ((SwtFancyText)swtFrame.textBox).setBackground (oastResources.getImageResourceAsImageData ("back.anonymator"));            else                ((Text)swtFrame.textBox).setBackgroundImage (oastResources.getImageResource ("back.anonymator"));            swtFrame.setVisible(true);            enableTrayItemMenu (SHOW_OAST_WINDOW_TEXT, false);        }        if (oastConfMgr.isDebugUI ()) {            if (oastConfMgr.isTraceUIEvents ()) {                et = new EventTracer (display);                et.addHooks ();                EventTracer et = new EventTracer (display);                System.out.println (" - Starting with UI debug option turned on: adding mouse event filter");            }        }        getTrayItem ().setToolTipText ("OAST OFFLINE");        while (!shell.isDisposed ()) {            if (!display.readAndDispatch ()) {                display.sleep ();            }        }        display.dispose ();    }    public static synchronized void breakConnection() {        if (!isSwtFrameInitialized())            return;        SwtFrame swtFrame = getSwtFrame();        if (!swtFrame.exitFrame(false))            return;        swtFrame.showLoginFrame();        enableTrayItemMenu("Disconnect", false);        ocs.setCurrentStatus(CONNECTION_INFO);        getTrayItem().setToolTipText("OAST OFFLINE");        setDisconnected();    }    private void processEvent (final Display display, final Menu menu) {        Point cursorLocation = display.getCursorLocation ();        menu.setLocation (cursorLocation.x, cursorLocation.y);        menu.setVisible (true);    }    /**     * determine the runtime environment     */    public static final int getEnv () {        if (System.getProperty ("os.name").equals ("Linux")) {            return UNIX;        } else            return WINDOWS;    }    /**     * returns an instance of the configuration manager     */    public static OastConfigurationManager getConfigurationManager () {        return oastConfMgr;    }    private static void loadResources () {        oastResources = new ResourceContainer (display, true);        oastResources.addLocation ("ui");    }    public static void setConnected () {        display.asyncExec(new Runnable() {            @Override            public void run() {                item.setImage (oastResources.getImageResource ("oast.connected"));            }        });    }    public static void setDisconnected () {        display.asyncExec(new Runnable() {            @Override            public void run() {                item.setImage (oastResources.getImageResource ("oast.disconnected"));            }        });    }    public static synchronized void showErrorMessage (final String message) {        getSwtFrame().swtFrameShell.getDisplay ().asyncExec (new Runnable () {            public void run () {                MessageBox messageBox = null;                messageBox = new MessageBox (getSwtFrame().swtFrameShell, SWT.APPLICATION_MODAL | SWT.ICON_ERROR | SWT.OK);                messageBox.setText ("Error");                messageBox.setMessage (message);                messageBox.open ();            }        });    }    public static void enableTrayItemMenu (final String itemName, final boolean enabled) {        int i;        for (i = 0; i < Oast.menu.getItemCount (); i++) {            if (Oast.menu.getItem (i).getText ().indexOf (itemName) == 0)                if (!Oast.menu.getItem (i).isEnabled () == enabled) {                    Oast.menu.getItem (i).setEnabled (enabled);                    break;                }        }    }    static void initialize () {        if (getEnv () == UNIX) {            try {                System.out.println ("Cleanup previous OpenVPN instances");                Runtime.getRuntime ().exec ("sh ./cleanup");            } catch (IOException ex) { ex.printStackTrace (); }        }        loadResources ();        oastConfMgr = new OastConfigurationManager ("oast.conf");        oastConfMgr.loadConfiguration ();        if (oastConfMgr.getOpenVPNDir () == null) {            if (oastConfMgr.isDebugConsole ())                System.out.println ("openvpn folder was not set explicitly. Trying to guess for platform " + Oast.getEnv ());            oastConfMgr.setOpenVPNDir (oastConfMgr.guessOpenVPNDir ());            if (oastConfMgr.isDebugConsole ())                System.out.println ("It seems, that openvpn executable resides here: " + oastConfMgr.getOpenVPNDir ());        }        setAliveConnection (false);        ocs = new OastConnectionStatus (SWT.ICON_WORKING, SWT.ICON_ERROR, SWT.ICON_INFORMATION,                "Connection established", "Could not establish connection", "You are not connected now.",                "Connected", "Error", "Disconnected");        ocs.setCurrentStatus (CONNECTION_INFO);        oastThreads = new Hashtable<String, Object>();        try {            geoipService = new GeoIPServiceImpl();        } catch (IOException ioe) {            throw new RuntimeException(ioe);        }    }    public static synchronized Hashtable<String, Object> getOastThreadPool () {        return oastThreads;    }        private static List<Checker> getCheckersForSwtFrame() {        List<Checker> checkers = new ArrayList<Checker>();        checkers.add(new OpenVPNAvailableChecker());        checkers.add(new OpenVPNConfigChecker());        checkers.add(new UserPassConfChecker());        return checkers;    }        /**     * @param args     * @throws IOException      */    public static void main (String [] args) {        initialize ();        Oast oast = new Oast ();        oast.showTray ();        if (et != null)            et.removeHooks ();        oastResources.clearAllResources ();    }}
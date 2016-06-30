package com.oast.config;

import java.io.*;

import com.oast.Oast;

// TODO: need per-option storeLogData flag
public class OastConfigurationManager {
    private short managementPort = 9000;
    private boolean showNotifications;
    private boolean useCustomWidgets;
    private boolean logFacility;
    private boolean initialized;
    private boolean debugConsole;
    private boolean debugUI;
    private boolean debugOpenVPN;
    private boolean warnOnExit;
    private String configFile;
    private boolean startOpen;
    private String configurationFile;
    private boolean useGradient;
    private String confUname;
    private String confPwd;
    private boolean storeLogData;
    private String openVPNDir = null;
    private boolean traceUIEvents;
    private String ipUrl;
    private int realIpDelay = Oast.DEFAULT_DELAY;
    private int fackedIpDelay = Oast.DEFAULT_DELAY;

    final static char CONFIG_DELIMITER = ' ';

    public OastConfigurationManager (final String confFile) {
        configurationFile = confFile;
        initialized = false;
    }

    public String getOastConfigurationFile () {
        return configurationFile;
    }

    /**
     * returns value indicating whether configuration manager was initialized with values read from configuration file or not
     */
    public boolean isInitialized () {
        return initialized;
    }

    private void debugOption (final String opName, boolean op) {
        System.out.println (opName + " = " + op);
    }

    private void debugOption (final String opName, String op) {
        System.out.println (opName + " = " + op);
    }

    private void debugOption (final String opName, short op) {
        System.out.println (opName + " = " + op);
    }

    public void dumpConfigurationToStdout () {
        if (isDebugConsole ()) {
            System.out.println (" -- current configuration fetched from file " + configurationFile + "\n");
            debugOption ("ui.useCustomWidgets", useCustomWidgets);
            debugOption ("sys.managementPort", managementPort);
            debugOption ("ui.showNotifications", showNotifications);
            debugOption ("sys.logFacility", debugConsole);
            debugOption ("debug.debugConsole", debugConsole);
            debugOption ("debug.debugUI", debugUI);
            debugOption ("debug.debugOpenVPN", debugOpenVPN);
            debugOption ("ui.warnOnExit", warnOnExit);
            debugOption ("sys.configFile", configFile);
            debugOption ("ui.startOpen", startOpen);
            debugOption ("ui.useGradient", useGradient);
            debugOption ("sys.confUname", confUname);
            debugOption ("sys.confPwd", confPwd);
            debugOption ("sys.storeLogData", storeLogData);
            debugOption ("openVPNDir", openVPNDir);
            debugOption ("debug.traceUIEvents", traceUIEvents);
        }
    }

    /**
     * loads Oast configuration stored in the configuration file
     */
    public void loadConfiguration () {
        // TODO StShadow >> code is terrible. need rewrite it
        try {
            FileReader reader = new FileReader (configurationFile);
            BufferedReader buffer = new BufferedReader (reader);
            String l;

            while ((l = buffer.readLine ()) != null) {
                String name = "", value = "";
                int idx = l.indexOf (CONFIG_DELIMITER);

                if (idx > 0) {
                    name = l.substring (0, idx).trim ();
                    value = l.substring (idx + 1).trim ();
                    // props.put(name, value);
                } else
                    continue;

                if (name.equals ("sys.openVPNDir")) {
                    openVPNDir = value;
                }

                if (name.equals ("ui.showTrayNotification")) {
                    if (value.equals ("true"))
                        showNotifications = true;
                    else
                        showNotifications = false;
                    continue;
                }

                if (name.equals ("ui.swtFancyTextUseGradient")) {
                    if (value.equals ("true"))
                        useGradient = true;
                    else
                        useGradient = false;
                    continue;
                }

                if (name.equals ("ui.startOpen")) {
                    if (value.equals ("true"))
                        startOpen = true;
                    else
                        startOpen = false;
                    continue;
                }

                if (name.equals ("sys.storeLogData")) {
                    storeLogData = Boolean.parseBoolean (value);
                }

                if (name.equals ("ui.useCustomWidgets")) {
                    if (value.equals ("true"))
                        useCustomWidgets = true;
                    else
                        useCustomWidgets = false;
                    continue;
                }

                if (name.equals ("debug.debugUI")) {
                    if (value.equals ("true"))
                        debugUI = true;
                    else
                        debugUI = false;
                    continue;
                }

                if (name.equals ("debug.debugOpenVPN")) {
                    if (value.equals ("true"))
                        debugOpenVPN = true;
                    else
                        debugOpenVPN = false;
                    continue;
                }

                if (name.equals ("sys.logFacility")) {
                    if (value.equals ("true"))
                        logFacility = true;
                    else
                        logFacility = false;
                    continue;
                }

                if (name.equals ("debug.debugConsole")) {
                    if (value.equals ("true"))
                        debugConsole = true;
                    else
                        debugConsole = false;
                    continue;
                }

                if (name.equals ("ui.warnOnExit")) {
                    if (value.equals ("true"))
                        warnOnExit = true;
                    else
                        warnOnExit = false;
                    continue;
                }

                if (name.equals ("debug.traceUIEvents")) {
                    if (value.equals ("true"))
                        traceUIEvents = true;
                    else
                        traceUIEvents = false;
                }

                if (name.equals ("sys.managementPort")) {
                    managementPort = Short.parseShort (value);
                    continue;
                }

                if (name.equals ("ui.showTrayNotification")) {
                    if (value.equals ("true"))
                        showNotifications = true;
                    else
                        showNotifications = false;
                    continue;
                }

                if (name.equals ("sys.configFile")) {
                    configFile = value;
                }

                if (name.equals ("sys.confUname")) {
                    confUname = value;
                }

                if (name.equals ("sys.confPwd")) {
                    confPwd = value;
                } else if ("sys.ipUrl".equals(name)) {
                    ipUrl = value;
                } else if ("sys.realIpDelay".equals(name)) {
                    // TODO check config file too
                    realIpDelay = Integer.parseInt(value);
                } else if ("sys.fackedIpDelay".equals(name)) {
                    fackedIpDelay = Integer.parseInt(value);
                }
            }

            buffer.close ();
            reader.close ();

            initialized = true;
        } catch (Exception e){
            initialized = false;
        }

        dumpConfigurationToStdout ();
    }

    /**
     * saves the current settings to the configuration file
     */
    public void storeConfiguration () {
        try {
            FileWriter saver = new FileWriter (configurationFile);

            if (startOpen)
                saver.write ("ui.startOpen true\n");
            else
                saver.write ("ui.startOpen false\n");

            if (warnOnExit)
                saver.write ("ui.warnOnExit true\n");
            else
                saver.write ("ui.warnOnExit false\n");

            if (showNotifications)
                saver.write ("ui.showTrayNotification true\n");
            else
                saver.write ("ui.showTrayNotification false\n");

            if (useCustomWidgets)
                saver.write ("ui.useCustomWidgets true\n");
            else
                saver.write ("ui.useCustomWidgets false\n");

            if (useGradient)
                saver.write ("ui.swtFancyTextUseGradient true\n");
            else
                saver.write ("ui.swtFancyTextUseGradient false\n");

            if (debugOpenVPN)
                saver.write ("debug.debugOpenVPN true\n");
            else
                saver.write ("debug.debugOpenVPN false\n");

            if (debugUI)
                saver.write ("debug.debugUI true\n");
            else
                saver.write ("debug.debugUI false\n");

            if (debugConsole)
                saver.write ("debug.debugConsole true\n");
            else
                saver.write ("debug.debugConsole false\n");

            if (traceUIEvents)
                saver.write ("debug.traceUIEvents true\n");
            else
                saver.write ("debug.traceUIEvents false\n");

            if (configFile != null)
                saver.write ("sys.configFile " + configFile + "\n");

            if (confUname != null)
                saver.write ("sys.confUname " + confUname + "\n");

            if (confPwd != null)
                saver.write ("sys.confPwd " + confPwd + "\n");

            if (storeLogData)
                saver.write ("sys.storeLogData true\n");
            else
                saver.write ("sys.storeLogData false\n");

            if (logFacility)
                saver.write ("sys.logFacility true\n");
            else
                saver.write ("sys.logFacility false\n");

            if (openVPNDir != null)
                saver.write ("sys.openVPNDir " + openVPNDir + "\n");

            saver.write ("sys.managementPort " + String.valueOf (managementPort));
            saver.write("\nsys.ipUrl" + " " + ipUrl);
            saver.write("\nsys.fackedIpDelay " + fackedIpDelay);
            saver.write("\nsys.realIpDelay " + realIpDelay);
            saver.close ();
        } catch (Exception e2) {
            System.out.println ("Error saving new default configuration to " + configurationFile + " - " + e2);
        }
    }

    /**
     * obtain OpenVPN control port
     */
    public short getManagementPort () {
        return managementPort;
    }

    /**
     * get showNotifications value read from configuration file
     */
    public boolean isShowNotifications () {
        return showNotifications;
    }

    /**
     * @param value determines whether tray notificaions shall be shown (true) or not (false)
     */
    public void setShowNotifications (boolean value) {
        showNotifications = value;
    }

    public boolean isUseCustomWidgets () {
        return useCustomWidgets;
    }

    public void setWarnOnExit (boolean warn) {
        warnOnExit = warn;
    }

    public boolean isLogFacility () {
        return logFacility;
    }

    public boolean isWarnOnExit () {
        return warnOnExit;
    }

    public boolean isStoreLogData() {
        return storeLogData;
    }

    public void setStoreLogData (final boolean flag) {
        storeLogData = flag;
    }

    public boolean isStartOpen() {
        return startOpen;
    }

    public void setStartOpen(boolean value) {
        startOpen = value;
    }

    public boolean isDebugConsole () {
        return debugConsole;
    }

    public boolean isDebugUI () {
        return debugUI;
    }

    public boolean isDebugOpenVPN () {
        return debugOpenVPN;
    }

    public boolean isUseGradient () {
        return useGradient;
    }

    public void setConfUname (final String uname) {
        confUname = uname;
    }

    public String getConfUname () {
        return confUname;
    }

    public void setConfPwd (final String pwd) {
        confPwd = pwd;
    }

    public String getConfPwd () {
        return confPwd;
    }

    public String getConfigFile () {
        return configFile;
    }

    public void setConfigFile (final String file) {
        configFile = file;
    }

    public String getOpenVPNDir () {
        return openVPNDir;
    }

    public void setOpenVPNDir (final String openvpnDir) {
        openVPNDir = openvpnDir;
    }

    public String guessOpenVPNDir () {
        String result = null;

        switch (Oast.getEnv ()) {
        case Oast.UNIX: {
            Runtime re = Runtime.getRuntime ();
            Process p;
            try {
                p = re.exec ("which openvpn");
                InputStreamReader in = new InputStreamReader (p.getInputStream ());
                BufferedReader r = new BufferedReader (in);
                result = r.readLine ();
                r.close ();
                in.close ();
            } catch (IOException e) {
                System.out.println ("FATAL: A linux system lacks \"which\". There\'s going something terrible!\nBail out.");
                e.printStackTrace();
                System.exit (1);
            }
        }
        break;

        case Oast.WINDOWS: {
            
        }
        break;
        }
        return result;
    }

    public boolean isTraceUIEvents () {
        return traceUIEvents;
    }

    public String getIpUrl() {
        return ipUrl;
    }
    
    public void setIpUrl(String value) {
        ipUrl = value;
    }

    public int getRealIpDelay() {
        return realIpDelay;
    }

    public void setRealIpDelay(int defaultDelay) {
        realIpDelay = defaultDelay;
    }

    public void setFackedIpDelay(int defaultDelay) {
        fackedIpDelay = defaultDelay;
    }

    public int getFackedIpDelay() {
        return fackedIpDelay;
    }
}

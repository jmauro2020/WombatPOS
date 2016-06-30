package com.oast.connection;

import java.io.InputStream;
import java.io.OutputStream;

import com.oast.Oast;
import com.oast.config.OastConfigurationManager;

public class OastConnectionManager {
    private OastConnectionStatus connectionStatus;
    private Runtime runtimeEnv;
    private Process process;
    private boolean errStatus;
    OastConfigurationManager configMan;
    InputStream in;
    OutputStream out;

    public OastConnectionManager (OastConfigurationManager ocm) {
        runtimeEnv = Runtime.getRuntime ();
        connectionStatus = new OastConnectionStatus ();
        configMan = ocm;
    }

    public void connect () {
        String cfg;

        if (Oast.getConfigurationManager ().getConfigFile () != null)
            cfg = Oast.getConfigurationManager ().getConfigFile ();
        else
            cfg = "openvpn.conf";

        String ovx = "openvpn";

        if (Oast.getConfigurationManager ().getOpenVPNDir () != null)
            ovx = Oast.getConfigurationManager ().getOpenVPNDir ();        // override ovx here with the value guessed or set by the user explicitly

        String file = checkConfigFile (cfg);
        System.out.println ("Trying configuration: " + file);
        String command = ovx + " --config " + file + " --management 127.0.0.1 " + configMan.getManagementPort () + " --management-query-passwords";
        if (Oast.getConfigurationManager ().isDebugOpenVPN ())
            System.out.println ("Trying out: " + command);

        try {
            process = runtimeEnv.exec (command);
            in = process.getInputStream ();
            out = process.getOutputStream ();
            errStatus = false;
        } catch (Exception e) {
            errStatus = true; 
            e.printStackTrace ();
            Oast.getOastConnection ().setCurrentStatus (Oast.CONNECTION_ERROR);
            Oast.getOastConnection ().setStatusMessage (Oast.CONNECTION_ERROR,
            Oast.getOastConnection ().getStatusMessage (Oast.CONNECTION_ERROR) + ": " + e.getMessage ());
        }
    }

    public void disconnect () {
        process.destroy ();
    }

    public OastConnectionStatus getConnectionStatus () {
        return connectionStatus;
    }

    public Process getProcess () {
        return process;
    }

    public boolean getErrorStatus () {
        return errStatus;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    private String checkConfigFile (final String c) {
        if (c.indexOf (" ") != -1) {
            if (Oast.getEnv () == Oast.UNIX || Oast.getEnv () == Oast.MACOS)
                return c.replace (" ", "\\ ");
            else
                return ("\"" + c + "\"");
        } else
            return c;
    }
}

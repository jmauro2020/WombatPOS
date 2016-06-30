package com.oast.config;

import oast.util.Base64Coder;

public class LoginStorage {
    final OastConfigurationManager ocm;

    private String cPwd;
    private String cUname;

    public LoginStorage (final OastConfigurationManager ocm) {
        this.ocm = ocm;
    }

    public String retrievePassword () {
        cPwd = ocm.getConfPwd ();
        return Base64Coder.decodeString (Base64Coder.decodeString (cPwd));
    }

    public String retrieveUsername () {
        cUname = ocm.getConfUname ();
        return Base64Coder.decodeString (Base64Coder.decodeString (cUname));
    }

    public void storePassword (final String pwd) {
        cPwd = Base64Coder.encodeString (Base64Coder.encodeString (pwd));
    }

    public void storeUsername (final String uname) {
        cUname = Base64Coder.encodeString (Base64Coder.encodeString (uname));
    }

    public void storeLoginData () {
        ocm.setConfUname (cUname);
        ocm.setConfPwd (cPwd);
        ocm.storeConfiguration ();
    }
}

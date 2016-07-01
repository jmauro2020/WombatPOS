package com.oast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/*
* MakeHttpRequest.java
*
* Created on May 15, 2007, 8:40 PM by Irvin Owens Jr
*/

public class HttpRequest {
    private String myUri;

    public HttpRequest (String uri) {
        this.myUri = uri;
    }

    public String doRequest () {
        String req = null;

        try {
            req = makeHttpRequest ();
        } catch (IOException ex) {
            ex.printStackTrace ();
        }

        return req;
    }

    private String makeHttpRequest () throws IOException {
        String str = null;
        URL hp = new URL (this.myUri);
        URLConnection conn = hp.openConnection ();

        conn.addRequestProperty ("User-Agent","WelshCorgi/1.0(WC 1.0; " + System.getProperty ("os.version") + "; " + System.getProperty ("os.version") + "; " + System.getProperty ("os.arch") + ") Corgi/1.0.0.0");
        InputStream input = conn.getInputStream ();
        conn.connect ();
        String mime = conn.getContentType ();

        if (mime.equals ("html") || mime.equals ("text") || mime.equals ("text/html")) {
            // Get response data.
            BufferedReader inputData = new BufferedReader (new InputStreamReader (input));
            StringBuilder sb = new StringBuilder ();
            while (null != ((str = inputData.readLine ()))){
                sb.append (str);
            }
            str = sb.toString ();
        }

        return str;
    }
}

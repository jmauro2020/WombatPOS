package com.oast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class SwtAboutDialog {
    private Shell dialog;
    private final String vBanner = "\n\nOpenVPN Agent and Setup Tool\n" + 
    "Version " + Oast.getVersion () + "\n\n\n" +
    "e-mail: oast.dev@gmail.com"
    ;


    private void loadTextFromFile (final String file, Text text) {
        StringBuffer buffer = new StringBuffer ();

        try {
               FileReader fileReader = new FileReader (file);
               BufferedReader in = new BufferedReader (fileReader);
               String string;

               while (true) {
                   string = in.readLine ();
                   if (string == null)
                       break;
                   string = string + "\n";
                   buffer.append (string);
               }
        } catch (IOException e) {
               e.printStackTrace ();
               System.exit (1);
        }

        text.setText (new String (buffer));
    }

    public SwtAboutDialog (Shell shell) {
        dialog = new Shell (shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setImage (Oast.oastResources.getImageResource ("oast-ico-16x16"));
        dialog.setText ("OAST: About");
        dialog.setSize (405, 385);

        Monitor primary = dialog.getMonitor ();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = dialog.getBounds ();

        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;

        dialog.setMinimized (false);
        dialog.setMaximized (false);
        dialog.setLocation (x, y);
        dialog.setVisible (true);

        Label banner = new Label (dialog, SWT.NONE);
        banner.setImage (Oast.oastResources.getImageResource ("banner"));
        banner.setLayoutData (new GridData (400, 120));
        banner.setSize (400, 120);

        CTabFolder folder = new CTabFolder (dialog, SWT.NONE);
        folder.setSimple (false);
        folder.setLocation (0, 121);
        folder.setSize (400, 235);

        CTabItem item = new CTabItem (folder, SWT.NONE);
        item.setText ("About Oast");

        Composite group = new Composite (folder, SWT.BORDER);
        group.setLayout (new GridLayout (1, false));

        GridData gd;

        Label label = new Label (group, SWT.CENTER);
        label.setText (vBanner);
        label.setVisible (true);
        gd = new GridData ();
        gd.widthHint = 385;
        label.setLayoutData (gd);

        Link link = new Link (group, SWT.NONE);
        link.setText ("<a href=\"http://sourceforge.net/projects/oast/\">http://sourceforge.net/projects/oast/</a>");
        link.setToolTipText("OAST on SourceForge");
        link.setVisible (true);
        link.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                        openLinkInBrowser(e.text);
                    }
            });
        gd = new GridData (GridData.CENTER);
        link.setLayoutData (gd);
        
//        link = new Link (group, SWT.CENTER);

        gd.horizontalAlignment = SWT.CENTER;
        link.setLayoutData (gd);

        item.setControl (group);
        item = new CTabItem (folder, SWT.NONE);
        item.setText ("Legal Copyright");

        Text text = new Text (folder, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setLayoutData (new GridData (395, 235));
        text.setLocation (0, 121);
        text.setSize (395, 235);
        text.setEditable (false);

        loadTextFromFile ("COPYING", text);
        item.setControl (text);


        dialog.open ();
    }

    public Shell getContainedShell () {
        return dialog;
    }
    
    public void openLinkInBrowser(String link) {
        Program.launch(link);
    }
}

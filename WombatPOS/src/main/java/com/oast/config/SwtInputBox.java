package com.oast.config;

import com.oast.Oast;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class SwtInputBox {
    private final class RangeChecker extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            Text source = ((Text)e.getSource());
            
            String value = source.getText(); 
            if (value != null) {
                int delay = Oast.DEFAULT_DELAY;
                try {
                    delay = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    MessageBox warnBox = new MessageBox(swtInputShell, SWT.APPLICATION_MODAL | SWT.ICON_ERROR | SWT.OK);
                    warnBox.setText("Format Error!");
                    warnBox.setMessage(RANGE_MESSAGE);
                    warnBox.open();
                    source.setText(String.valueOf(delay));
                }
                if ( 30 >= delay || delay > 900) {
                    MessageBox warnBox = new MessageBox(swtInputShell, SWT.APPLICATION_MODAL | SWT.ICON_ERROR | SWT.OK);
                    warnBox.setText("Range Error!");
                    warnBox.setMessage(RANGE_MESSAGE);
                    warnBox.open();
                    delay = Oast.DEFAULT_DELAY;
                    source.setText(String.valueOf(delay));
                }
            }
        }
    }

    private static final String RANGE_MESSAGE = "Please, enter correct integer value greater than 30, but less or equal than 900.";
    private Shell swtInputShell;
    // FIXME: declare internal variable showNotifications to avoid interference with SwtFrame
    // when Cancel pressed and changes should get lost
    private boolean hideMainWindow = false, warnOnExit = true, showNotifications = true;

    public Shell getContainedShell () {
        return swtInputShell;
    }

    public SwtInputBox (Display swtFrameDisplay) {
        swtInputShell = new Shell (swtFrameDisplay, SWT.DIALOG_TRIM);
        swtInputShell.setImage (Oast.getOastResources().getImageResource ("oast-ico-16x16"));
        swtInputShell.setText ("OAST: Settings");
        swtInputShell.setSize (250, 300);
        swtInputShell.setLayout (new GridLayout ());
        ((GridLayout)swtInputShell.getLayout ()).numColumns = 1;

        if (Oast.getConfigurationManager ().isInitialized ()) {
            showNotifications = Oast.getConfigurationManager ().isShowNotifications ();
            hideMainWindow = !Oast.getConfigurationManager ().isStartOpen ();
            warnOnExit = Oast.getConfigurationManager ().isWarnOnExit ();
        }

        Composite composite;

        composite = new Composite (swtInputShell, SWT.NONE);
        composite.setLayout (new GridLayout ());
        ((GridLayout)composite.getLayout ()).numColumns = 1;

        Button button = new Button (composite, SWT.CHECK);
        button.setText ("Hide main window on startup");
        button.setSelection (hideMainWindow);

        button.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                hideMainWindow = !hideMainWindow;
            }
        });

        button = new Button (composite, SWT.CHECK);
        button.setText ("Show tray notifications");
        button.setSelection (showNotifications);

        button.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                showNotifications = !showNotifications;
            }
        });

        button = new Button (composite, SWT.CHECK);
        button.setText ("Warn on exit");
        button.setSelection (warnOnExit);

        button.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                warnOnExit = !warnOnExit;
            }
        });

        
        composite = new Composite (swtInputShell, SWT.NONE);
        composite.setLayout (new GridLayout ());
        ((GridLayout)composite.getLayout ()).numColumns = 2;
        
        Label label;

        /// 1
        label = new Label (composite, SWT.NORMAL);
        label.setText ("OpenVPN configuration file");
        label.setLayoutData (new GridData ());
        ((GridData)label.getLayoutData ()).horizontalSpan = 2;

        final Text config = new Text (composite, SWT.SINGLE);
        config.setTextLimit (255); // max path, that work good on windows systems
        config.setSize (250, 20);
        config.setLayoutData (new GridData (GridData.HORIZONTAL_ALIGN_BEGINNING|GridData.GRAB_HORIZONTAL));
        ((GridData)config.getLayoutData ()).minimumWidth = 250;
        if (Oast.getConfigurationManager ().getConfigFile () != null)
            config.setText (Oast.getConfigurationManager ().getConfigFile ());
        else
            config.setText ("openvpn.conf");

        button = constructBrowseButton (composite, config);
        
        // ip label
        Label ipLabel = new Label(composite, SWT.NORMAL);
        ipLabel.setText("Detection IP URL:"); // TODO externalise me
        ipLabel.setLayoutData(new GridData());
        ((GridData)ipLabel.getLayoutData()).horizontalSpan = 2;
        
        final Text ipUrl = new Text(composite, SWT.SINGLE);
        ipUrl.setTextLimit(255);
        // ipUrl.setSize(310 , 20);
        ipUrl.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING|GridData.GRAB_HORIZONTAL));
        ((GridData)ipUrl.getLayoutData()).minimumWidth = 310;
        ((GridData)ipUrl.getLayoutData()).horizontalSpan = 2;
        if (Oast.getConfigurationManager().getIpUrl() != null) {
            ipUrl.setText(Oast.getConfigurationManager().getIpUrl());
        } else {
            ipUrl.setText("http://www.whatismyip.com/automation/n09230945.asp");
        }
        ipUrl.setToolTipText("Be carefull. At present \"save\" method do not perform any validation");

        // update time composite
        composite = new Composite(swtInputShell, SWT.NONE);
        composite.setLayout (new GridLayout ());
        ((GridLayout)composite.getLayout ()).numColumns = 3;
        
        // update real ip
        Label realLabel = new Label(composite, SWT.NORMAL);
        realLabel.setText("Update your real IP (check internet connection) each");
        realLabel.setLayoutData(new GridData());
        //((GridData)realLabel.getLayoutData ()).verticalSpan = 2;
        
        final Text realIpValue = new Text(composite, SWT.SINGLE);
        realIpValue.setLayoutData (new GridData (GridData.HORIZONTAL_ALIGN_BEGINNING|GridData.GRAB_HORIZONTAL));
        ((GridData)realIpValue.getLayoutData ()).minimumWidth = 25;
        ((GridData)realIpValue.getLayoutData ()).widthHint = 25;
        realIpValue.setTextLimit(3);
        realIpValue.setToolTipText("30 < value <= 900");
        if (Oast.getConfigurationManager().getRealIpDelay() == Oast.UNDEFINED_VALUE) {
            Oast.getConfigurationManager().setRealIpDelay(Oast.DEFAULT_DELAY);
        }
        realIpValue.setText(String.valueOf(Oast.getConfigurationManager().getRealIpDelay()));
        realIpValue.addFocusListener(new RangeChecker());
        
        Label secLabel = new Label(composite, SWT.NORMAL);
        secLabel.setText("secs");
      
        // the same for faked ip
        Label fackedLabel = new Label(composite, SWT.NORMAL);
        fackedLabel.setText("Update your faked IP (check anonymity) each");
        fackedLabel.setLayoutData(new GridData());
        //((GridData)fackedLabel.getLayoutData ()).verticalSpan = 2;
        
        final Text fackedIpValue = new Text(composite, SWT.SINGLE);
        fackedIpValue.setLayoutData (new GridData (GridData.HORIZONTAL_ALIGN_BEGINNING|GridData.GRAB_HORIZONTAL));
        ((GridData)fackedIpValue.getLayoutData ()).minimumWidth = 25;
        ((GridData)fackedIpValue.getLayoutData ()).widthHint = 25;
        fackedIpValue.setTextLimit(3);
        fackedIpValue.setToolTipText("30 < value <= 900");
        if (Oast.getConfigurationManager().getFackedIpDelay() == Oast.UNDEFINED_VALUE) {
            Oast.getConfigurationManager().setFackedIpDelay(Oast.DEFAULT_DELAY);
        }
        fackedIpValue.setText(String.valueOf(Oast.getConfigurationManager().getFackedIpDelay()));
        
        fackedIpValue.addFocusListener(new RangeChecker());
        
        
        secLabel = new Label(composite, SWT.NORMAL);
        secLabel.setText("secs");
      
        
        /// !1
        
        composite = new Composite (swtInputShell, SWT.NONE);
        composite.setLayout (new GridLayout ());
        ((GridLayout)composite.getLayout ()).numColumns = 2;

        button = new Button (composite, SWT.PUSH);
        button.setText ("      OK      ");
        button.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                Oast.getConfigurationManager ().setStartOpen (!hideMainWindow);
                Oast.getConfigurationManager ().setShowNotifications (showNotifications);
                Oast.getConfigurationManager ().setWarnOnExit (warnOnExit);
                Oast.getConfigurationManager ().setConfigFile (config.getText ());
                Oast.getConfigurationManager ().setIpUrl(ipUrl.getText());
                Oast.getConfigurationManager().setRealIpDelay(Integer.parseInt(realIpValue.getText()));
                Oast.getConfigurationManager().setFackedIpDelay(Integer.parseInt(fackedIpValue.getText()));
                Oast.getConfigurationManager ().storeConfiguration ();
                swtInputShell.dispose ();
            }
        });

        button = new Button (composite, SWT.PUSH);
        button.setText ("    Cancel    ");
        button.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                swtInputShell.dispose ();
            }
        });

        swtInputShell.pack ();
        swtInputShell.open ();
    }

    Button constructBrowseButton (final Composite c, final Text text) {
        Button t = new Button (c, SWT.PUSH);
        t.setText ("Browse...");
        t.addSelectionListener (new SelectionAdapter () {
            public void widgetSelected (SelectionEvent e) {
                FileDialog f = new FileDialog (swtInputShell);
                String file = f.open ();

                if (file != null)
                    text.setText (file);
            }
        });

        return t;
    }
}

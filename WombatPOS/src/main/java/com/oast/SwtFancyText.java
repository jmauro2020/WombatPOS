package com.oast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Display;

// FIXME: drawable text-on-background scrolling 

public class SwtFancyText {
    private Image textImage;
    private Image fullImage;
    private int width;            // background picture width
    private int height;            // .. height
    private boolean readOnly;
    static Text text;
    private int origX = 0, origY = 0;
    private boolean drawText = false;
    private boolean useGradient;
    private Display displayDev;
    private Composite composite;

    public void setBackground (ImageData id) {
        createBackgroundImage (composite, id);
    }

    private void setupSwtFancyText (Composite c, ImageData id, int opts, boolean background) {
        text = new Text (c, opts);

        if (background)
            createBackgroundImage (c, id);

        text.addPaintListener (new PaintListener () {
            public void paintControl (PaintEvent e) {
                superPaintControl (e);
            }
        });

        text.addDisposeListener (new DisposeListener () {
            public void widgetDisposed (DisposeEvent e) {
                if (textImage != null)
                    textImage.dispose ();
                if (fullImage != null)
                    fullImage.dispose ();
            }
        });
    }

    public SwtFancyText (Composite c, int opts, ImageData imageData, boolean readonly, boolean background) {
        setupSwtFancyText (c, imageData, opts, background);
        composite = c;
        readOnly = readonly;
        text.setEditable (!readOnly);
        makeUpScrollbars ();
    }

    public SwtFancyText (Composite c, int opts, ImageData imageData, boolean background) {
        composite = c;
        setupSwtFancyText (c, imageData, opts, background);
        makeUpScrollbars ();
    }

    public SwtFancyText (Composite c, int opts, ImageData imageData, boolean background, boolean readonly, boolean gradient) {
        composite = c;
        useGradient = gradient;
        setupSwtFancyText (c, imageData, opts, background);
        readOnly = readonly;
        text.setEditable (!readOnly);
        makeUpScrollbars ();
    }

    private void makeUpScrollbars () {
        if (text.getVerticalBar () != null) {
            ScrollBar scrollBar = text.getVerticalBar();
            text.getVerticalBar ().addSelectionListener (new SelectionAdapter () {
                public void widgetSelected (SelectionEvent e) {
                    System.out.println("vertical bar: " + e);
                    origY = text.getVerticalBar ().getSelection () * (text.getVerticalBar ().getMaximum () - text.getVerticalBar ().getThumb ())
                        / Math.max (1, text.getVerticalBar ().getMaximum() - text.getVerticalBar ().getThumb());
                    text.redraw ();
                }
            });
        }

        if (text.getHorizontalBar () != null) {
            text.getHorizontalBar ().addSelectionListener (new SelectionAdapter () {
                public void widgetSelected (SelectionEvent e) {
                    origX = text.getHorizontalBar ().getSelection () * (text.getHorizontalBar ().getMaximum () - text.getHorizontalBar ().getThumb ())
                        / Math.max (1, text.getHorizontalBar ().getMaximum() - text.getHorizontalBar ().getThumb());
                    text.redraw ();
                }
            });
        }
    }

    private void  createBackgroundImage (Composite c, ImageData imageData) {
        width = imageData.width;
        height = imageData.height;

        if (!useGradient)
            imageData.alpha = (byte)255;
        else {
            byte [] alphaData = new byte [height * width];

            for (int y = 0; y < height; y++) {
                byte [] alphaRow = new byte [width];
                for (int x = 0; x < width; x++)
                    alphaRow [x] = (byte) (height / 4);
                System.arraycopy (alphaRow, 0, alphaData, y * width, width);
            }
            imageData.alphaData = alphaData;
        }

        if (drawText) {
            textImage =  new Image (c.getDisplay (), imageData);
            fullImage = new Image (c.getDisplay (), textImage, SWT.IMAGE_COPY);
        } else
            fullImage = new Image (c.getDisplay (), imageData);
    }

    public void superPaintControl (PaintEvent e) {
        if (fullImage == null)
            return;
        e.gc.drawImage (fullImage, 0, 0);
        if (drawText)
            e.gc.drawText (text.getText (), -origX, -origY, true);
    }

    public void loadTextFromFile (String fileName) {
        StringBuffer buffer = new StringBuffer ();

        try {
            FileReader fileReader = new FileReader (fileName);
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

    public Text getContainedControl () {
        return text;
    }

    public Image getBackgroundImage () {
        return fullImage;
    }

    /* Rescale background image if it doesn't fit into Text box */
    public void reScale () {
        if (fullImage == null)
            return;

        Rectangle r = text.getBounds ();

        if (height > r.height || height < r.height || width > r.width || width < r.width) {
            if (Oast.getConfigurationManager ().isDebugConsole ()) {
                System.out.println ("SwtFancyText: image bounds aren\'t equal to those of the Text:\n" +
                        "Image: width = " + width + "; height = " + height + "\nText: text.getBounds().width = " + r.width +
                        "; text.getBounds().height = " + r.height + "\n using reScale() method, loss of quality is possible.");
            }

            Image img = new Image (displayDev, fullImage.getImageData ().scaledTo (r.width - 25, r.height));
            fullImage = img;
        }
    }
}

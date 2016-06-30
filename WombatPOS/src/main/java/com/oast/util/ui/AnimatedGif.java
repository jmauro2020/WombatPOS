package oast.util.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class AnimatedGif {
    static Display display;
    static GC shellGC;
    static Color shellBackground;
    static ImageLoader loader;
    static ImageData [] imageDataArray;
    static Thread animateThread;
    static Image image;
    static final boolean useGIFBackground = false;
    static int originx, originy;
    private static boolean stop;

    public AnimatedGif () { }

    public static synchronized void setLocation (final int x, final int y) {
        originx = x;
        originy = y;
    }

    public static Point getLocation () {
        return new Point (originx, originy);
    }

    public static synchronized void play (final Composite parent, String fileName) {
        stop = false;
        display = parent.getDisplay ();
        shellGC = new GC (parent);
        shellBackground = parent.getBackground ();
        loader = new ImageLoader ();
        image = new Image (display, fileName);

        final int x = originx;
        final int y = originy;

        image.dispose ();
        image = null;

        try {
            imageDataArray = loader.load (fileName);
            if (imageDataArray.length >= 0) {
                animateThread = new Thread ("Animation") {
                    public void run () {
                        /*
                         * Create an off-screen image to draw on, and fill it
                         * with the shell background.
                         */
                        Image offScreenImage = new Image (display, loader.logicalScreenWidth, loader.logicalScreenHeight);
                        GC offScreenImageGC = new GC (offScreenImage);
                        offScreenImageGC.setBackground (shellBackground);
                        offScreenImageGC.fillRectangle (0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);

                        try {
                            /*
                             * Create the first image and draw it on the
                             * off-screen image.
                             */
                            int imageDataIndex = 0;
                            ImageData imageData = imageDataArray [imageDataIndex];

                            if (image != null && !image.isDisposed ())
                                image.dispose ();
                            image = new Image (display, imageData);
                            offScreenImageGC.drawImage (image, 0, 0, imageData.width, imageData.height,    imageData.x, imageData.y, imageData.width, imageData.height);

                            /*
                             * Now loop through the images, creating and drawing
                             * each one on the off-screen image before drawing
                             * it on the shell.
                             */
                            int repeatCount = loader.repeatCount;

                            while (loader.repeatCount == 0 || repeatCount > 0) {
                                if (parent.isDisposed () || isStopped ())
                                    break;

                                switch (imageData.disposalMethod) {
                                    case SWT.DM_FILL_BACKGROUND:
                                        /*
                                         * Fill with the background color before
                                         * drawing.
                                         */
                                        Color bgColor = null;

                                        if (useGIFBackground && loader.backgroundPixel != -1) {
                                            bgColor = new Color (display, imageData.palette.getRGB (loader.backgroundPixel));
                                        }
                                        offScreenImageGC.setBackground (bgColor != null ? bgColor : shellBackground);
                                        offScreenImageGC.fillRectangle (imageData.x, imageData.y, imageData.width, imageData.height);
                                        if (bgColor != null)
                                            bgColor.dispose ();
                                        break;

                                    case SWT.DM_FILL_PREVIOUS:
                                        /*
                                         * Restore the previous image before
                                         * drawing.
                                         */
                                        offScreenImageGC.drawImage (image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);
                                        break;
                                }

                                imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
                                imageData = imageDataArray [imageDataIndex];
                                // prevent exception during reconnection.
                                if (image != null) {
                                    image.dispose ();
                                }
                                image = new Image (display, imageData);
                                offScreenImageGC.drawImage (image, 0, 0, imageData.width, imageData.height, imageData.x, imageData.y, imageData.width, imageData.height);

                                /* Draw the off-screen image to the shell. */
                                shellGC.drawImage (offScreenImage, x, y);

                                /*
                                 * Sleep for the specified delay time (adding
                                 * commonly-used slow-down fudge factors).
                                 */
                                try {
                                    int ms = imageData.delayTime * 10;
                                    if (ms < 20)
                                        ms += 30;
                                    if (ms < 30)
                                        ms += 10;
                                    Thread.sleep (ms);
                                } catch (InterruptedException e) {
                                    // ...
                                }

                                /*
                                 * If we have just drawn the last image,
                                 * decrement the repeat count and start again.
                                 */
                                if (imageDataIndex == imageDataArray.length)
                                    repeatCount--;
                            }

                            stop = false;
                        } catch (SWTException ex) {
                            System.out.println ("There was an error animating the GIF");
                        } finally {
                            if (offScreenImage != null && !offScreenImage.isDisposed ())
                                offScreenImage.dispose ();
                            if (offScreenImageGC != null && !offScreenImageGC.isDisposed ())
                                offScreenImageGC.dispose ();
                            if (image != null && !image.isDisposed ())
                                image.dispose ();
                        }
                    }
                };
                animateThread.setDaemon (true);
                animateThread.start ();
            }
        } catch (SWTException ex) {
            System.out.println ("There was an error loading the GIF");
            ex.printStackTrace ();
        }
    }

    public static synchronized void stop () {
        stop = true;
    }

    public static boolean isStopped () {
        return stop;
    }
}

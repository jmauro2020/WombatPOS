package oast.util.ui.widgets;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class SwtCustomButton extends Canvas {
    private Image btnNormal;
    private Image btnPressed;
    private Image btnRollover;
    private Image drawableImage;

    public SwtCustomButton (Composite parent, Image normal, Image pressed, Image rollover) {
        super (parent, 0);
        btnNormal = normal;
        btnPressed = pressed;
        btnRollover = rollover;
        drawableImage = btnNormal;

        setSize (drawableImage.getBounds ().width, drawableImage.getBounds ().height);

        addPaintListener (new PaintListener () {
            public void paintControl (PaintEvent e) {
                superPaintControl (e);
            }
        });

        addMouseTrackListener (new MouseTrackAdapter () {
            public void mouseEnter (MouseEvent e) {
                drawableImage = btnRollover;
                redraw ();
            }

            public void mouseExit (MouseEvent e) {
                drawableImage = btnNormal;
                redraw ();
            }
        });

        addMouseListener (new MouseAdapter () {
            public void mouseDown (MouseEvent e) {
                drawableImage = btnPressed;
                redraw ();
            }

            public void mouseUp (MouseEvent e) {
                drawableImage = btnNormal;
                redraw ();
            }
        });

//        addDisposeListener (new DisposeListener () {
//            public void widgetDisposed (DisposeEvent e) {
//                if (btnNormal != null)
//                    btnNormal.dispose ();
//                if (btnPressed != null)
//                    btnPressed.dispose ();
//                if (btnRollover != null)
//                    btnRollover.dispose ();
//            }
//        });
    }

    public void superPaintControl (PaintEvent e) {
        e.gc.drawImage (drawableImage, 0, 0);
    }

    public Point computeSize (int wHint, int hHint, boolean changed) {
         int width = 0, height = 0;

         if (drawableImage != null) {
             Rectangle bounds = drawableImage.getBounds ();
             width = bounds.width;
             height = bounds.height;
         }

         return new Point (width, height + 2);     
    }
}

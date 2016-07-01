package com.oast.util;

import java.util.*;
import org.eclipse.swt.graphics.*;

public class ResourceContainer {
    private boolean rcLoadResourcesOnDemand = false;
    private Hashtable<String, Image> rcHashImages;
    private Hashtable<String, String> rcHashStrings;
    private List<String> rcLocations;
    private static Device rcDevice;

    private static final String[] imageFileExtensions = new String[] {
        ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    };

    private static final int IF_EXTNR = 5;  
    public static final int LOAD_OK = 1;
    public static final int LOAD_FAILED = 0;

    private void createContainer (final Device device) {
        rcHashImages = new Hashtable<String, Image> ();
        rcLocations = new ArrayList<String> ();
        rcDevice = device;
    }

    public ResourceContainer (final Device device, final boolean loadOnDemand) {
        rcLoadResourcesOnDemand = loadOnDemand;
        createContainer (device);
    }

    public ResourceContainer (final Device device) {
        createContainer (device);
    }

    public void addLocation (final String folder) {
        rcLocations.add (folder);
    }

    public int loadImageResource (final String idString, final String fromWhere) {
        Image image;
        int result = LOAD_FAILED;

        try {
            image = new Image (rcDevice, fromWhere);
            rcHashImages.put (idString, image);
            result = LOAD_OK;
        } catch (Exception e) {
            
        }

        return result;
    }

    public ImageData getImageResourceAsImageData (final String idString) {
        Image image = getImageResource (idString);

        if (image != null)
            return image.getImageData ();
        else
            return null;
    }

    public void dumpResources () {
        Iterator<String> it = rcHashImages.keySet ().iterator ();

        System.out.println (" -- loaded resources: images\n");
        while (it.hasNext ()) {
            String itemKey = it.next ();

            System.out.println (itemKey + " : " +
                    rcHashImages.get (itemKey).getImageData ().width +
                    "x" + rcHashImages.get (itemKey).getImageData ().height +
                    "x" + rcHashImages.get (itemKey).getImageData ().depth +
                    ", type = " + rcHashImages.get (itemKey).type);
        }
        System.out.println ("  - end of dump -");
    }
    
    public Image getImageResource (final String idString) {
        Image image;
        int success = LOAD_FAILED;

        if ((image = rcHashImages.get (idString)) == null) {
            if (rcLoadResourcesOnDemand) {
                for (String s : rcLocations) {
                    if (!s.endsWith ("/"))
                        s = s + "/";
                    for (int i = 0; i < IF_EXTNR; i++) {
                        success = loadImageResource (idString, s + idString + imageFileExtensions [i]);
                        if (success == LOAD_OK)
                            break;
                    }

                    if (success == LOAD_OK) {
                        break;
                    }
                }

                if (success == LOAD_FAILED)
                    return null;
                else
                    image = rcHashImages.get (idString);
            }
        }

        return image;
    }

    public void removeImageResource (final String idString) {
        rcHashImages.remove (idString);
    }

    public void clearAllResources () {
        rcHashImages.clear ();
        rcHashStrings.clear ();
    }
}

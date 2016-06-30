package oast.geoip;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

import com.maxmind.geoip.LookupService;

public class GeoIPServiceImpl implements GeoIPService {
    private final LookupService countryLookup;
    
    public GeoIPServiceImpl() throws IOException {
        final String defaultDBFile = defaultDBFileLocation();
        if (defaultDBFile == null) {
            throw new IllegalStateException("No default GeoIP database file found");
        }
        countryLookup = initLookupService(defaultDBFile);
    }

    public GeoIPServiceImpl(String geoipDBFileName) throws IOException {
        countryLookup = initLookupService(geoipDBFileName);
    }
    
    private LookupService initLookupService(String fileName) throws IOException {
        File dbFile = new File(fileName);
        LookupService lookupService = new LookupService(dbFile);
        return lookupService;
    }

    public Country getCountryByIP(String ipAddress) {
        try {
            return getCountryByIP(InetAddress.getByName(ipAddress));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not convert string to ip: " + ipAddress, e);
        }
    }
    
    public Country getCountryByIP(InetAddress ipAddress) {
        com.maxmind.geoip.Country country = countryLookup.getCountry(ipAddress);
        return Country.getInstance(country.getCode());
    }
    
    private static String defaultDBFileLocation() {
        final URL geoDBFileURL = GeoIPServiceImpl.class.getResource("GeoIP.dat");
        if (geoDBFileURL == null) {
            return null;
        }

        try {
            final URI geoDBFileURI = geoDBFileURL.toURI();
            if (!geoDBFileURI.getScheme().equals("file")) {
                // class file loaded not from plain directory on filesystem, e.g. from jar, network, etc.
                return null;
            }
            final File geoDBFile = new File(geoDBFileURI);
            return geoDBFile.getPath();
        } catch (URISyntaxException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static void main(String[] args) throws IOException {
        GeoIPService geoip = new GeoIPServiceImpl();
        System.out.println(geoip.getCountryByIP("151.38.39.114").getDisplayName());
        System.out.println(geoip.getCountryByIP("12.25.205.51").getDisplayName(Locale.US));
        System.out.println(geoip.getCountryByIP("64.81.104.131").getDisplayName());
        System.out.println(geoip.getCountryByIP("200.21.225.82").getDisplayName(Locale.US));
        System.out.println(geoip.getCountryByIP("10.20.30.40"));
        System.out.println(geoip.getCountryByIP("127.0.0.1"));
    }
}

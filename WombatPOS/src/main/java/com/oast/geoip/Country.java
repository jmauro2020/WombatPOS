package oast.geoip;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Country {
    private static final Map<String, Country> COUNTRIES_CACHE = new HashMap<String, Country>();
    private final Locale locale;

    /**
     * Construct country instance backed by given locale.
     * @param locale Locale object with non-empty country info.
     */
    private Country(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("Locale should not be null");
        }
        if (locale.getCountry() == null || locale.getCountry().trim().length() == 0) {
            throw new IllegalArgumentException("Locale's country should be defined.");
        }
        this.locale = locale;
    }

    /**
     * @param countryCode 2-letter country code.
     * @return Country instance matching specified code or null if not found.
     */
    public static Country getInstance(String countryCode) {
        synchronized (COUNTRIES_CACHE) {
            Country country = COUNTRIES_CACHE.get(countryCode);
            if (country == null) {
                Locale locale = findLocaleByCountryCode(countryCode);
                if (locale != null) {
                    country = new Country(locale);
                    COUNTRIES_CACHE.put(countryCode, country);
                }
            }
            return country;
        }
    }
    
    public String getDisplayName() {
        return locale.getDisplayCountry();
    }

    public String getDisplayName(Locale inLocale) {
        return locale.getDisplayCountry(inLocale);
    }

    public String getCode() {
        return locale.getCountry();
    }
    
    public String getISO3Code() {
        return locale.getISO3Country();
    }
    
    /**
     * @param countryCode 2-letter country code.
     * @return some random locale with given country code or null if not found
     */
    private static Locale findLocaleByCountryCode(String countryCode) {
        Locale[] allLocales = Locale.getAvailableLocales();
        for (Locale locale: allLocales) {
            if (locale.getCountry().equals(countryCode)) {
                return locale;
            }
        }
        return null;
    }

}

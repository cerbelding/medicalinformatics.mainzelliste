package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class MainzellisteCallbackUtil {

    /**
     * Checks the Callback Url. Currently following checks are processed:
     * <p>Checks if the callback address does match callback.allowedFormat from Mainzelliste config file. Otherwise throws {@link InvalidTokenException}</p>
     * <p>Checks if the callback url is a valid {@link URI}. Otherwise throws an {@link InvalidTokenException}</p>
     *
     * @param callback The CallbackUrl which should be checked
     */
    public static void checkCallbackUrl(String callback) {
        if (!Pattern.matches(
                Config.instance.getProperty("callback.allowedFormat"),
                callback)) {
            throw new InvalidTokenException("Callback address " + callback
                    + " does not conform to allowed format!");
        }
        try {
            @SuppressWarnings("unused")
            URI callbackURI = new URI(callback);
        } catch (URISyntaxException e) {
            throw new InvalidTokenException("Callback address " + callback
                    + " is not a valid URI!");
        }
    }
}

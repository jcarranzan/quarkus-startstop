package io.quarkus.ts.startstop.utils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebpageTester {
    private static final Logger LOGGER = Logger.getLogger(WebpageTester.class.getName());

    /**
     * Patiently try to wait for a web page and examine it
     *
     * @param url             address
     * @param timeoutS        in seconds
     * @param stringToLookFor string must be present on the page
     */
    public static long testWeb(String url, long timeoutS, String stringToLookFor, boolean measureTime) throws InterruptedException, IOException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url must not be empty");
        }
        if (timeoutS < 0) {
            throw new IllegalArgumentException("timeoutS must be positive");
        }
        if (StringUtils.isBlank(stringToLookFor)) {
            throw new IllegalArgumentException("stringToLookFor must contain a non-empty string");
        }
        String webPage = "";

        long now = System.currentTimeMillis();
        final long startTime = now;
        boolean found = false;
        long foundTimestamp = -1L;
        LOGGER.info("Starting to test web page at " + url + " for string `" + stringToLookFor + "` with timeout of " + timeoutS + " seconds.");
        while (now - startTime < 1000 * timeoutS) {
            try {
                URLConnection c = URI.create(url).toURL().openConnection();
                c.setRequestProperty("Accept", "*/*");
                c.setConnectTimeout(500);
                LOGGER.debug("Attempting to connect to " + url);

                try (InputStream in = c.getInputStream();
                     Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.toString())) {
                    scanner.useDelimiter("\\A");
                    webPage = scanner.hasNext() ? scanner.next() : "";
                } catch (Exception e) {
                    LOGGER.debug("Waiting `" + stringToLookFor + "' to appear on " + url + " Exception : " + e.getMessage());
                }
                if (webPage.contains(stringToLookFor)) {
                    found = true;
                    if (measureTime) {
                        foundTimestamp = System.currentTimeMillis();
                    }
                    LOGGER.info("String: " + stringToLookFor + " found on the page.");
                    break;
                } else {
                    LOGGER.debug("String not found on the page. Retrying...");
                }
            } catch (IOException e) {
                LOGGER.debug("Connection attempt failed with exception: " + e.getMessage());
            }

            if (!measureTime) {
                Thread.sleep(500);
            } else {
                LockSupport.parkNanos(100000);
            }
            now = System.currentTimeMillis();
        }

        String failureMessage = "Timeout " + timeoutS + "s was reached. " +
                (StringUtils.isNotBlank(webPage) ? webPage + " must contain string: " : "Empty webpage does not contain string: ") +
                "`" + stringToLookFor + "'";
        if (!found) {
            LOGGER.info(failureMessage);
        }
        assertTrue(found, failureMessage);
        return foundTimestamp - startTime;
    }
}

package org.leralix.tan.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.leralix.lib.data.PluginVersion;
import org.leralix.tan.TownsAndNations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles plugin version tracking and update checking.
 * Extracts version concerns from the main plugin class.
 */
public class VersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionService.class);
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String GITHUB_API_URL =
        "https://api.github.com/repos/leralix/towns-and-nations/releases/latest";
    private static final PluginVersion CURRENT_VERSION = new PluginVersion(0, 16, 0);
    private static final PluginVersion MINIMUM_SUPPORTING_DYNMAP = new PluginVersion(0, 14, 0);

    private PluginVersion latestVersion;

    public PluginVersion getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public PluginVersion getMinimumSupportingDynmap() {
        return MINIMUM_SUPPORTING_DYNMAP;
    }

    public PluginVersion getLatestVersion() {
        return latestVersion;
    }

    public boolean isLatestVersion() {
        if (latestVersion == null) {
            return true;
        }
        return !CURRENT_VERSION.isOlderThan(latestVersion);
    }

    @SuppressWarnings("unused")
    public void checkForUpdate() {
        if (!TownsAndNations.getPlugin().getConfig().getBoolean("CheckForUpdate", true)) {
            LOGGER.info("[TaN] Update check is disabled");
            latestVersion = CURRENT_VERSION;
            return;
        }
        try {
            URL url = java.net.URI.create(GITHUB_API_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    latestVersion = extractVersionFromResponse(response.toString());
                    if (CURRENT_VERSION.isOlderThan(latestVersion)) {
                        LOGGER.info("[TaN] A new version is available : {0}", latestVersion);
                    } else {
                        LOGGER.info("[TaN] Towns and Nation is up to date: " + CURRENT_VERSION);
                    }
                }
            } else {
                LOGGER.info("[TaN] An error occurred while trying to accesses github API.");
                LOGGER.info("[TaN] Error log : " + con.getInputStream());
            }
        } catch (Exception e) {
            LOGGER.warn("[TaN] An error occurred while trying to check for updates.");
            latestVersion = CURRENT_VERSION;
        }
    }

    private PluginVersion extractVersionFromResponse(String response) {
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        String version = jsonResponse.get("tag_name").getAsString();
        return new PluginVersion(version);
    }
}

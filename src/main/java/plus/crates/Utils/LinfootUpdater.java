package plus.crates.Utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import plus.crates.CratesPlus;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LinfootUpdater {
    private final CratesPlus cratesPlus;
    private final String branch;
    private LinfootUpdater.UpdateResult result = LinfootUpdater.UpdateResult.FAILED;
    private String version;

    public enum UpdateResult {
        NO_UPDATE,
        FAILED,
        SNAPSHOT_UPDATE_AVAILABLE,
        UPDATE_AVAILABLE
    }

    public LinfootUpdater(CratesPlus cratesPlus, String branch) {
        if (branch.equalsIgnoreCase("spigot"))
            branch = "release";
        this.cratesPlus = cratesPlus;
        this.branch = branch;
        doCheck();
    }

    private void doCheck() {
        String url = "https://api.connorlinfoot.com/v2/resource/" + branch + "/cratesplus/";
        String data;
        try {
            data = doCurl(url);
            Object parsed = new JSONParser().parse(data);
            if (!(parsed instanceof JSONObject obj)) {
                cratesPlus.getLogger().warning("Update service returned an unexpected response.");
                return;
            }
            if (obj.get("version") != null) {
                String newestVersion = obj.get("version") + "." + obj.get("snapshot");
                String currentVersion = cratesPlus.getDescription().getVersion().replaceAll("-SNAPSHOT-", "."); // Changes 4.0.0-SNAPSHOT-4 to 4.0.0.4
                if (Integer.parseInt(newestVersion.replace(".", "")) > Integer.parseInt(currentVersion.replace(".", ""))) {
                    if (branch.equalsIgnoreCase("snapshot")) {
                        result = UpdateResult.UPDATE_AVAILABLE;
                        version = obj.get("version").toString();
                    } else {
                        result = UpdateResult.SNAPSHOT_UPDATE_AVAILABLE;
                        version = obj.get("version") + "-SNAPSHOT-" + obj.get("snapshot");
                    }
                } else {
                    result = UpdateResult.NO_UPDATE;
                }
            }
        } catch (IOException | ParseException | NumberFormatException exception) {
            cratesPlus.getLogger().warning("Unable to check for updates: " + exception.getMessage());
        }
    }

    public UpdateResult getResult() {
        return result;
    }

    public String getVersion() {
        return version;
    }

    public String doCurl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);
        con.setConnectTimeout(10_000);
        con.setReadTimeout(10_000);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "CratesPlus update checker");

        int status = con.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("update service returned HTTP " + status);
        }
        String contentType = con.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            throw new IOException("update service returned " + (contentType == null ? "an unknown content type" : contentType));
        }
        try (InputStream input = con.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            con.disconnect();
        }
    }

}

package at.chaosfield.updaterUpdater;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class UpdaterUpdater {

    public static final String apiUrl = "https://api.github.com/repos/XDjackieXD/PackUpdate/releases";

    public static void main(String[] args) {
        new UpdaterUpdater(args);
    }

    public UpdaterUpdater(String[] args) {

        String version = null;

        String mmcDir = System.getenv("INST_MC_DIR");

        if (mmcDir == null) {
            System.err.println("This program is intended to run as MultiMC Pre-Launch Hook");
            System.exit(1);
        }

        File baseFile = new File(mmcDir);
        File packupdateDir = new File(baseFile, "packupdate");
        if (!packupdateDir.exists()) {
            if (!packupdateDir.mkdirs()) {
                System.err.println("Could not create packupdate data directory");
            }
        }

        File packupdateFile = new File(packupdateDir, "PackUpdate.jar");
        File propertyFile = new File(packupdateDir, "updater.properties");

        Properties prop = new Properties();
        prop.setProperty("beta", "false");

        try {
            if (propertyFile.exists()) {
                prop.load(new FileReader(propertyFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read settings, exiting");
        }

        boolean beta = prop.getProperty("beta").equalsIgnoreCase("true");

        try {

            if (packupdateFile.exists()) {
                Manifest manifest = new JarFile(packupdateFile).getManifest();
                version = manifest.getMainAttributes().getValue("Implementation-Version");
            }


        } catch (IOException e) {
            System.out.println("[PackUpdate Updater] Warning: could not find original PackUpdate. Downloading it now.");
        }

        try {
            if (version == null || hasUpdate(version, beta)) {
                System.out.println("PackUpdate Update available, downloading...");
                if (!downloadPackUpdate(packupdateFile.getPath(), beta)) {
                    System.err.println("[PackUpdate Updater] Update Failed.");
                }
            }
        } catch (IOException e) {
            System.err.println("[PackUpdate Updater] Update Failed.");
            e.printStackTrace();
        }

        try {
            prop.store(new FileWriter(propertyFile), "PackUpdate Updater settings");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not store properties");
        }

        String mainClass = null;

        try {
            if (packupdateFile.exists()) {
                Manifest manifest = new JarFile(packupdateFile).getManifest();
                mainClass = manifest.getMainAttributes().getValue("Main-Class");
            } else {
                System.err.println("[PackUpdate Updater] Everything is broken, call for help!");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[PackUpdate Updater] Everything is broken, call for help!");
            System.exit(1);
        }

        try {
            JarRunner.addFile(packupdateFile);
            Method mainMethod = ClassLoader.getSystemClassLoader().loadClass(mainClass).getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            System.out.println("[PackUpdate Updater] Execution of PackUpdater failed");
            e.printStackTrace();
        }

    }

    public static JSONObject getReleaseForBranch(boolean beta) throws IOException {
         JSONArray data = getJSON(apiUrl);
         for (Object foo: data) {
             JSONObject release = (JSONObject) foo;
             if (release.getBoolean("draft")) {
                 continue;
             }

             if (beta || !release.getBoolean("prerelease")) {
                 return release;
             }
         }
         throw new RuntimeException("No releases found");
    }

    public static boolean hasUpdate(String version, boolean beta) throws IOException {
        JSONObject jsonObject = getReleaseForBranch(beta);
        return !(jsonObject.get("tag_name")).equals(version);
    }

    public static boolean downloadPackUpdate(String path, boolean beta) throws IOException {
        JSONObject jsonRelease = getReleaseForBranch(beta);

        for (Object asset: jsonRelease.getJSONArray("assets")) {
            if (((JSONObject) asset).getString("name").startsWith("PackUpdate")) {

                FileUtils.copyURLToFile(new URL(((JSONObject) asset).getString("browser_download_url")), new File(path));

                if (getLength(path) == ((JSONObject) asset).getLong("size")) {
                    return true;
                }

            }
        }
        return false;
    }

    private static JSONArray getJSON(String url) throws IOException {
        BufferedReader release = new BufferedReader(new BufferedReader(new InputStreamReader(new URL(url).openStream())));

        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = release.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }

        return new JSONArray(responseStrBuilder.toString());
    }

    public static long getLength(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }
}

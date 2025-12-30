package com.winlator.fexcore;

import app.gamenative.R;

import android.content.Context;
import com.winlator.container.Shortcut;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.container.Container;
import com.winlator.contents.ContentProfile;
import com.winlator.contents.ContentsManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DefaultVersion;
import com.winlator.core.FileUtils;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import timber.log.Timber;

public final class FEXCoreManager {

    FEXCoreManager() {
    }

    /**
     * Delete existing FEXCore config files for the given container.
     * Removes container-level config files and ensures Steam app configs exist.
     */
    public static void deleteConfigFiles(Context context, String containerId) {
        try {
            ImageFs imageFs = ImageFs.find(context);
            // Delete container-level config
            File containerConfig = new File(imageFs.home_path + "-" + containerId + "/.fex-emu/Config.json");
            if (containerConfig.exists()) {
                containerConfig.delete();
            }
            // Ensure AppConfig directory exists and create Steam config files
            File appConfigDir = new File(context.getFilesDir(), "imagefs/home/xuser/.fex-emu/AppConfig");
            if (!appConfigDir.exists()) {
                appConfigDir.mkdirs();
            }
            
            // Steam FEXCore config JSON content
            String steamConfigJson = "{\n" +
                "  \"Config\": {\n" +
                "    \"Multiblock\": \"0\",\n" +
                "    \"MaxInst\": \"5000\",\n" +
                "    \"HostFeatures\": \"off\",\n" +
                "    \"SmallTSCScale\": \"1\",\n" +
                "    \"TSOEnabled\": \"1\",\n" +
                "    \"VectorTSOEnabled\": \"1\",\n" +
                "    \"MemcpySetTSOEnabled\": \"0\",\n" +
                "    \"HalfBarrierTSOEnabled\": \"1\",\n" +
                "    \"VolatileMetadata\": \"1\",\n" +
                "    \"HideHypervisorBit\": \"0\",\n" +
                "    \"X87ReducedPrecision\": \"1\",\n" +
                "    \"MonoHacks\": \"0\",\n" +
                "    \"SilentLog\": \"1\",\n" +
                "    \"OutputLog\": \"stdout\",\n" +
                "    \"SMCChecks\": \"1\",\n" +
                "    \"ForceSVEWidth\": \"0\",\n" +
                "    \"ProfileStats\": \"0\",\n" +
                "    \"O0\": \"0\"\n" +
                "  }\n" +
                "}";
            
            // Create Steam config files
            String[] steamExes = {"steamservice.exe.json", "steamwebhelper.exe.json", "steam.exe.json"};
            for (String exeName : steamExes) {
                File configFile = new File(appConfigDir, exeName);
                FileUtils.writeString(configFile, steamConfigJson);
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to delete FEXCore config files");
        }
    }

    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, Container container) {
        String[] originalItems = context.getResources().getStringArray(R.array.fexcore_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        if (container != null)
            AppUtils.setSpinnerSelectionFromValue(spinner, container.getFEXCoreVersion());
        else
            AppUtils.setSpinnerSelectionFromValue(spinner, DefaultVersion.FEXCORE);
    }

    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, Shortcut shortcut) {
        String[] originalItems = context.getResources().getStringArray(R.array.fexcore_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        AppUtils.setSpinnerSelectionFromValue(spinner, shortcut.getExtra("fexcoreVersion", shortcut.container.getFEXCoreVersion()));
    }
}

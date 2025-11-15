package com.winlator.inputcontrols;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.JsonReader;

import com.winlator.PrefManager;
import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class InputControlsManager {
    private final Context context;
    private ArrayList<ControlsProfile> profiles;
    private int maxProfileId;
    private boolean profilesLoaded = false;

    public InputControlsManager(Context context) {
        this.context = context;
    }

    public static File getProfilesDir(Context context) {
        File profilesDir = new File(context.getFilesDir(), "profiles");
        if (!profilesDir.isDirectory()) profilesDir.mkdir();
        return profilesDir;
    }

    public ArrayList<ControlsProfile> getProfiles() {
        return getProfiles(false);
    }

    public ArrayList<ControlsProfile> getProfiles(boolean ignoreTemplates) {
        if (!profilesLoaded) loadProfiles(ignoreTemplates);
        return profiles;
    }

    // Get templates only
    public ArrayList<ControlsProfile> getTemplates() {
        ArrayList<ControlsProfile> allProfiles = getProfiles(false);
        ArrayList<ControlsProfile> templates = new ArrayList<>();
        for (ControlsProfile profile : allProfiles) {
            if (profile.isTemplate()) {
                templates.add(profile);
            }
        }
        return templates;
    }

    // Get global profiles only (not locked to any game, not templates)
    public ArrayList<ControlsProfile> getGlobalProfiles() {
        ArrayList<ControlsProfile> allProfiles = getProfiles(true); // ignore templates
        ArrayList<ControlsProfile> globalProfiles = new ArrayList<>();
        for (ControlsProfile profile : allProfiles) {
            if (!profile.isLockedToGame()) {
                globalProfiles.add(profile);
            }
        }
        return globalProfiles;
    }

    // Get profiles for a specific container (global + locked to this container)
    public ArrayList<ControlsProfile> getProfilesForContainer(String containerId) {
        ArrayList<ControlsProfile> allProfiles = getProfiles(true); // ignore templates
        ArrayList<ControlsProfile> containerProfiles = new ArrayList<>();
        for (ControlsProfile profile : allProfiles) {
            if (!profile.isLockedToGame() || containerId.equals(profile.getLockedToContainer())) {
                containerProfiles.add(profile);
            }
        }
        return containerProfiles;
    }

    // Clone a profile with new name and optional game lock
    public ControlsProfile cloneProfile(ControlsProfile source, String newName, String lockedToContainer) throws Exception {
        // Find next available ID
        int maxId = 0;
        for (ControlsProfile profile : getProfiles(false)) {
            if (profile.id > maxId) maxId = profile.id;
        }
        int newId = maxId + 1;

        // Create new profile with cloned settings
        ControlsProfile newProfile = new ControlsProfile(context, newId);
        newProfile.setName(newName);
        newProfile.setCursorSpeed(source.getCursorSpeed());

        // Copy sensitivity settings
        newProfile.setPhysicalStickDeadZone(source.getPhysicalStickDeadZone());
        newProfile.setPhysicalStickSensitivity(source.getPhysicalStickSensitivity());
        newProfile.setPhysicalDpadDeadZone(source.getPhysicalDpadDeadZone());
        newProfile.setVirtualStickDeadZone(source.getVirtualStickDeadZone());
        newProfile.setVirtualStickSensitivity(source.getVirtualStickSensitivity());
        newProfile.setVirtualDpadDeadZone(source.getVirtualDpadDeadZone());

        // Copy touchpad gesture settings
        newProfile.setEnableTapToClick(source.isEnableTapToClick());
        newProfile.setEnableTwoFingerRightClick(source.isEnableTwoFingerRightClick());

        // Copy mouse/touch behavior
        newProfile.setDisableTouchpadMouse(source.isDisableTouchpadMouse());
        newProfile.setTouchscreenMode(source.isTouchscreenMode());

        // Set game lock (can be different from source)
        newProfile.setLockedToContainer(lockedToContainer);

        // Read source profile file and copy elements/controllers arrays directly from JSON
        // (no need to load elements with InputControlsView)
        File sourceFile = ControlsProfile.getProfileFile(context, source.id);
        if (sourceFile.exists()) {
            try {
                org.json.JSONObject sourceJson = new org.json.JSONObject(com.winlator.core.FileUtils.readString(sourceFile));
                if (sourceJson.has("elements")) {
                    org.json.JSONArray elements = sourceJson.getJSONArray("elements");
                    // Save to new profile file with elements
                    File newFile = ControlsProfile.getProfileFile(context, newId);
                    org.json.JSONObject newJson = new org.json.JSONObject();
                    newJson.put("id", newId);
                    newJson.put("name", newName);
                    newJson.put("cursorSpeed", source.getCursorSpeed());
                    newJson.put("physicalStickDeadZone", source.getPhysicalStickDeadZone());
                    newJson.put("physicalStickSensitivity", source.getPhysicalStickSensitivity());
                    newJson.put("physicalDpadDeadZone", source.getPhysicalDpadDeadZone());
                    newJson.put("virtualStickDeadZone", source.getVirtualStickDeadZone());
                    newJson.put("virtualStickSensitivity", source.getVirtualStickSensitivity());
                    newJson.put("virtualDpadDeadZone", source.getVirtualDpadDeadZone());
                    newJson.put("enableTapToClick", source.isEnableTapToClick());
                    newJson.put("enableTwoFingerRightClick", source.isEnableTwoFingerRightClick());
                    newJson.put("disableTouchpadMouse", source.isDisableTouchpadMouse());
                    newJson.put("touchscreenMode", source.isTouchscreenMode());
                    if (lockedToContainer != null) {
                        newJson.put("lockedToContainer", lockedToContainer);
                    }
                    newJson.put("elements", elements);
                    if (sourceJson.has("controllers")) {
                        newJson.put("controllers", sourceJson.getJSONArray("controllers"));
                    }
                    com.winlator.core.FileUtils.writeString(newFile, newJson.toString());
                }
            } catch (Exception e) {
                throw new Exception("Failed to clone profile: " + e.getMessage());
            }
        } else {
            // No elements to copy, just save basic profile
            newProfile.save();
        }

        // Reload profiles to include the new one
        profilesLoaded = false;
        loadProfiles(false);

        return newProfile;
    }

    private void copyAssetProfilesIfNeeded() {
        File profilesDir = InputControlsManager.getProfilesDir(context);
        if (FileUtils.isEmpty(profilesDir)) {
            FileUtils.copy(context, "inputcontrols/profiles", profilesDir);
            return;
        }

        PrefManager.init(context);
        String newVersion = String.valueOf(AppUtils.getVersionCode(context));
        String oldVersion = PrefManager.getString("inputcontrols_app_version", "0");
        if (oldVersion == newVersion) return;
        PrefManager.putString("inputcontrols_app_version", newVersion);

        File[] files = profilesDir.listFiles();
        if (files == null) return;

        try {
            AssetManager assetManager = context.getAssets();
            String[] assetFiles = assetManager.list("inputcontrols/profiles");
            for (String assetFile : assetFiles) {
                String assetPath = "inputcontrols/profiles/"+assetFile;
                ControlsProfile originProfile = loadProfile(context, assetManager.open(assetPath));

                File targetFile = null;
                for (File file : files) {
                    ControlsProfile targetProfile = loadProfile(context, file);
                    if (originProfile.id == targetProfile.id && originProfile.getName().equals(targetProfile.getName())) {
                        targetFile = file;
                        break;
                    }
                }

                if (targetFile != null) {
                    FileUtils.copy(context, assetPath, targetFile);
                }
            }
        }
        catch (IOException e) {}
    }

    public void loadProfiles(boolean ignoreTemplates) {
        File profilesDir = InputControlsManager.getProfilesDir(context);
        copyAssetProfilesIfNeeded();

        ArrayList<ControlsProfile> profiles = new ArrayList<>();
        File[] files = profilesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                ControlsProfile profile = loadProfile(context, file);
                if (!(ignoreTemplates && profile.isTemplate())) profiles.add(profile);
                maxProfileId = Math.max(maxProfileId, profile.id);
            }
        }

        Collections.sort(profiles);
        this.profiles = profiles;
        profilesLoaded = true;
    }

    public ControlsProfile createProfile(String name) {
        ControlsProfile profile = new ControlsProfile(context, ++maxProfileId);
        profile.setName(name);

        // Add default controller configuration
        ExternalController defaultController = profile.addController("*");
        defaultController.setName("Default Physical Controller");

        // Add default button bindings
        addDefaultControllerBindings(defaultController);

        profile.save();
        profiles.add(profile);
        return profile;
    }

    private void addDefaultControllerBindings(ExternalController controller) {
        // Gamepad buttons (KeyEvent keycodes)
        addBinding(controller, 96, Binding.GAMEPAD_BUTTON_A);      // KEYCODE_BUTTON_A
        addBinding(controller, 97, Binding.GAMEPAD_BUTTON_B);      // KEYCODE_BUTTON_B
        addBinding(controller, 99, Binding.GAMEPAD_BUTTON_X);      // KEYCODE_BUTTON_X
        addBinding(controller, 100, Binding.GAMEPAD_BUTTON_Y);     // KEYCODE_BUTTON_Y
        addBinding(controller, 102, Binding.GAMEPAD_BUTTON_L1);    // KEYCODE_BUTTON_L1
        addBinding(controller, 103, Binding.GAMEPAD_BUTTON_R1);    // KEYCODE_BUTTON_R1
        addBinding(controller, 104, Binding.GAMEPAD_BUTTON_L2);    // KEYCODE_BUTTON_L2
        addBinding(controller, 105, Binding.GAMEPAD_BUTTON_R2);    // KEYCODE_BUTTON_R2
        addBinding(controller, 106, Binding.GAMEPAD_BUTTON_L3);    // KEYCODE_BUTTON_THUMBL
        addBinding(controller, 107, Binding.GAMEPAD_BUTTON_R3);    // KEYCODE_BUTTON_THUMBR
        addBinding(controller, 108, Binding.GAMEPAD_BUTTON_START); // KEYCODE_BUTTON_START
        addBinding(controller, 109, Binding.GAMEPAD_BUTTON_SELECT);// KEYCODE_BUTTON_SELECT

        // D-Pad
        addBinding(controller, 19, Binding.GAMEPAD_DPAD_UP);       // KEYCODE_DPAD_UP
        addBinding(controller, 20, Binding.GAMEPAD_DPAD_DOWN);     // KEYCODE_DPAD_DOWN
        addBinding(controller, 21, Binding.GAMEPAD_DPAD_LEFT);     // KEYCODE_DPAD_LEFT
        addBinding(controller, 22, Binding.GAMEPAD_DPAD_RIGHT);    // KEYCODE_DPAD_RIGHT

        // Left Analog Stick (axis codes)
        addBinding(controller, -3, Binding.GAMEPAD_LEFT_ANALOG_UP);    // AXIS_Y_NEGATIVE
        addBinding(controller, -4, Binding.GAMEPAD_LEFT_ANALOG_DOWN);  // AXIS_Y_POSITIVE
        addBinding(controller, -1, Binding.GAMEPAD_LEFT_ANALOG_LEFT);  // AXIS_X_NEGATIVE
        addBinding(controller, -2, Binding.GAMEPAD_LEFT_ANALOG_RIGHT); // AXIS_X_POSITIVE

        // Right Analog Stick (axis codes)
        addBinding(controller, -7, Binding.GAMEPAD_RIGHT_ANALOG_UP);    // AXIS_RZ_NEGATIVE
        addBinding(controller, -8, Binding.GAMEPAD_RIGHT_ANALOG_DOWN);  // AXIS_RZ_POSITIVE
        addBinding(controller, -5, Binding.GAMEPAD_RIGHT_ANALOG_LEFT);  // AXIS_Z_NEGATIVE
        addBinding(controller, -6, Binding.GAMEPAD_RIGHT_ANALOG_RIGHT); // AXIS_Z_POSITIVE
    }

    private void addBinding(ExternalController controller, int keyCode, Binding binding) {
        ExternalControllerBinding controllerBinding = new ExternalControllerBinding();
        controllerBinding.setKeyCode(keyCode);
        controllerBinding.setBinding(binding);
        controller.addControllerBinding(controllerBinding);
    }

    public ControlsProfile duplicateProfile(ControlsProfile source) {
        String newName;
        for (int i = 1;;i++) {
            newName = source.getName() + " ("+i+")";
            boolean found = false;
            for (ControlsProfile profile : profiles) {
                if (profile.getName().equals(newName)) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }

        int newId = ++maxProfileId;
        File newFile = ControlsProfile.getProfileFile(context, newId);

        try {
            JSONObject data = new JSONObject(FileUtils.readString(ControlsProfile.getProfileFile(context, source.id)));
            data.put("id", newId);
            data.put("name", newName);
            if (data.has("template")) data.remove("template");
            FileUtils.writeString(newFile, data.toString());
        }
        catch (JSONException e) {}

        ControlsProfile profile = loadProfile(context, newFile);
        profiles.add(profile);
        return profile;
    }

    public void removeProfile(ControlsProfile profile) {
        File file = ControlsProfile.getProfileFile(context, profile.id);
        if (file.isFile() && file.delete()) profiles.remove(profile);
    }

    public ControlsProfile importProfile(JSONObject data) {
        try {
            if (!data.has("id") || !data.has("name")) return null;

            // Ensure profiles are loaded
            if (profiles == null || !profilesLoaded) {
                loadProfiles(false);
            }

            // Load template defaults from controls-3.icp to ensure all required fields exist
            JSONObject templateDefaults = loadTemplateDefaults();

            // Get the original name from the imported profile
            String originalName = data.getString("name");
            String uniqueName = originalName;

            // Check for duplicate names and add numbering if needed
            int counter = 2;
            boolean nameExists = true;
            while (nameExists) {
                nameExists = false;
                for (ControlsProfile profile : profiles) {
                    if (profile.getName().equals(uniqueName)) {
                        nameExists = true;
                        uniqueName = originalName + " (" + counter + ")";
                        counter++;
                        break;
                    }
                }
            }

            // Merge with template defaults - ensure all required fields exist
            if (templateDefaults != null) {
                // Copy all default fields if they don't exist in imported data
                if (!data.has("cursorSpeed")) data.put("cursorSpeed", templateDefaults.optDouble("cursorSpeed", 1.0));
                if (!data.has("physicalStickDeadZone")) data.put("physicalStickDeadZone", templateDefaults.optDouble("physicalStickDeadZone", 0.15));
                if (!data.has("physicalStickSensitivity")) data.put("physicalStickSensitivity", templateDefaults.optDouble("physicalStickSensitivity", 3.0));
                if (!data.has("physicalDpadDeadZone")) data.put("physicalDpadDeadZone", templateDefaults.optDouble("physicalDpadDeadZone", 0.15));
                if (!data.has("virtualStickDeadZone")) data.put("virtualStickDeadZone", templateDefaults.optDouble("virtualStickDeadZone", 0.15));
                if (!data.has("virtualStickSensitivity")) data.put("virtualStickSensitivity", templateDefaults.optDouble("virtualStickSensitivity", 3.0));
                if (!data.has("virtualDpadDeadZone")) data.put("virtualDpadDeadZone", templateDefaults.optDouble("virtualDpadDeadZone", 0.3));
                if (!data.has("enableTapToClick")) data.put("enableTapToClick", templateDefaults.optBoolean("enableTapToClick", true));
                if (!data.has("enableTwoFingerRightClick")) data.put("enableTwoFingerRightClick", templateDefaults.optBoolean("enableTwoFingerRightClick", true));
                if (!data.has("disableTouchpadMouse")) data.put("disableTouchpadMouse", templateDefaults.optBoolean("disableTouchpadMouse", false));
                if (!data.has("touchscreenMode")) data.put("touchscreenMode", templateDefaults.optBoolean("touchscreenMode", false));

                // If controllers array is missing, use template default
                if (!data.has("controllers") && templateDefaults.has("controllers")) {
                    data.put("controllers", templateDefaults.getJSONArray("controllers"));
                }
            }

            // Update the name in the JSON data if it was changed
            if (!uniqueName.equals(originalName)) {
                data.put("name", uniqueName);
            }

            int newId = ++maxProfileId;
            File newFile = ControlsProfile.getProfileFile(context, newId);
            data.put("id", newId);
            FileUtils.writeString(newFile, data.toString());
            ControlsProfile newProfile = loadProfile(context, newFile);

            profiles.add(newProfile);
            return newProfile;
        }
        catch (JSONException e) {
            return null;
        }
    }

    private JSONObject loadTemplateDefaults() {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("inputcontrols/profiles/controls-3.icp");
            String content = FileUtils.readString(context, "inputcontrols/profiles/controls-3.icp");
            inputStream.close();
            return new JSONObject(content);
        } catch (Exception e) {
            return null;
        }
    }

    public File exportProfile(ControlsProfile profile) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // Sanitize profile name for use in filename
        String safeFilename = sanitizeProfileName(profile.getName());
        File destination = new File(downloadsDir, "Winlator/profiles/" + safeFilename + ".icp");
        FileUtils.copy(ControlsProfile.getProfileFile(context, profile.id), destination);
        MediaScannerConnection.scanFile(context, new String[]{destination.getAbsolutePath()}, null, null);
        return destination.isFile() ? destination : null;
    }

    /**
     * Validates a profile name for safety and usability.
     * @param name The profile name to validate
     * @return null if valid, error message if invalid
     */
    public static String validateProfileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Profile name cannot be empty";
        }

        String trimmed = name.trim();

        // Check length (max 100 characters for usability)
        if (trimmed.length() > 100) {
            return "Profile name must be 100 characters or less";
        }

        // Check for filesystem-unsafe characters
        String unsafeChars = "/\\:*?\"<>|";
        for (char c : unsafeChars.toCharArray()) {
            if (trimmed.indexOf(c) >= 0) {
                return "Profile name cannot contain: / \\ : * ? \" < > |";
            }
        }

        // Check for control characters or newlines
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                return "Profile name cannot contain control characters or newlines";
            }
        }

        // Check for leading/trailing dots (Windows issue)
        if (trimmed.startsWith(".") || trimmed.endsWith(".")) {
            return "Profile name cannot start or end with a dot";
        }

        return null; // Valid
    }

    /**
     * Sanitizes a profile name by removing/replacing unsafe characters.
     * Use this for auto-generated names or when validation fails.
     */
    public static String sanitizeProfileName(String name) {
        if (name == null) return "Unnamed Profile";

        String sanitized = name.trim();

        // Replace unsafe characters with underscore
        sanitized = sanitized.replaceAll("[/\\\\:*?\"<>|]", "_");

        // Remove control characters
        sanitized = sanitized.replaceAll("\\p{Cntrl}", "");

        // Remove leading/trailing dots
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        // Trim to max length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100).trim();
        }

        // Fallback if everything was removed
        if (sanitized.isEmpty()) {
            return "Unnamed Profile";
        }

        return sanitized;
    }

    public static ControlsProfile loadProfile(Context context, File file) {
        try {
            return loadProfile(context, new FileInputStream(file));
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

    public static ControlsProfile loadProfile(Context context, InputStream inStream) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
            int profileId = 0;
            String profileName = null;
            float cursorSpeed = Float.NaN;
            float physicalStickDeadZone = 0.15f;
            float physicalStickSensitivity = 3.0f;
            float physicalDpadDeadZone = 0.15f;
            float virtualStickDeadZone = 0.15f;
            float virtualStickSensitivity = 3.0f;
            float virtualDpadDeadZone = 0.3f;
            boolean enableTapToClick = true;
            boolean enableTwoFingerRightClick = true;
            boolean disableTouchpadMouse = false;
            boolean touchscreenMode = false;
            String lockedToContainer = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();

                if (name.equals("id")) {
                    profileId = reader.nextInt();
                }
                else if (name.equals("name")) {
                    profileName = reader.nextString();
                }
                else if (name.equals("cursorSpeed")) {
                    cursorSpeed = (float) reader.nextDouble();
                }
                else if (name.equals("physicalStickDeadZone")) {
                    physicalStickDeadZone = (float) reader.nextDouble();
                }
                else if (name.equals("physicalStickSensitivity")) {
                    physicalStickSensitivity = (float) reader.nextDouble();
                }
                else if (name.equals("physicalDpadDeadZone")) {
                    physicalDpadDeadZone = (float) reader.nextDouble();
                }
                else if (name.equals("virtualStickDeadZone")) {
                    virtualStickDeadZone = (float) reader.nextDouble();
                }
                else if (name.equals("virtualStickSensitivity")) {
                    virtualStickSensitivity = (float) reader.nextDouble();
                }
                else if (name.equals("virtualDpadDeadZone")) {
                    virtualDpadDeadZone = (float) reader.nextDouble();
                }
                else if (name.equals("enableTapToClick")) {
                    enableTapToClick = reader.nextBoolean();
                }
                else if (name.equals("enableTwoFingerRightClick")) {
                    enableTwoFingerRightClick = reader.nextBoolean();
                }
                else if (name.equals("disableTouchpadMouse")) {
                    disableTouchpadMouse = reader.nextBoolean();
                }
                else if (name.equals("touchscreenMode")) {
                    touchscreenMode = reader.nextBoolean();
                }
                else if (name.equals("lockedToContainer")) {
                    lockedToContainer = reader.nextString();
                }
                else {
                    reader.skipValue();
                }
            }

            ControlsProfile profile = new ControlsProfile(context, profileId);
            profile.setName(profileName);
            profile.setCursorSpeed(cursorSpeed);
            profile.setPhysicalStickDeadZone(physicalStickDeadZone);
            profile.setPhysicalStickSensitivity(physicalStickSensitivity);
            profile.setPhysicalDpadDeadZone(physicalDpadDeadZone);
            profile.setVirtualStickDeadZone(virtualStickDeadZone);
            profile.setVirtualStickSensitivity(virtualStickSensitivity);
            profile.setVirtualDpadDeadZone(virtualDpadDeadZone);
            profile.setEnableTapToClick(enableTapToClick);
            profile.setEnableTwoFingerRightClick(enableTwoFingerRightClick);
            profile.setDisableTouchpadMouse(disableTouchpadMouse);
            profile.setTouchscreenMode(touchscreenMode);
            profile.setLockedToContainer(lockedToContainer);
            return profile;
        }
        catch (IOException e) {
            return null;
        }
    }

    public ControlsProfile getProfile(int id) {
        for (ControlsProfile profile : getProfiles()) if (profile.id == id) return profile;
        return null;
    }
}

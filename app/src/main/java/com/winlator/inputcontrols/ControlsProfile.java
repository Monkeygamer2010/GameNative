package com.winlator.inputcontrols;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.winlator.core.FileUtils;
import com.winlator.widget.InputControlsView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ControlsProfile implements Comparable<ControlsProfile> {
    public final int id;
    private String name;
    private float cursorSpeed = 1.0f;

    // Physical controller sensitivity settings
    private float physicalStickDeadZone = 0.15f;
    private float physicalStickSensitivity = 3.0f;
    private float physicalDpadDeadZone = 0.15f;

    // Virtual (on-screen) controls sensitivity settings
    private float virtualStickDeadZone = 0.15f;
    private float virtualStickSensitivity = 3.0f;
    private float virtualDpadDeadZone = 0.3f;

    // Touchpad gesture settings (tap to click, multi-finger gestures)
    private boolean enableTapToClick = true;
    private boolean enableTwoFingerRightClick = true;

    // Mouse and touch behavior settings
    private boolean disableTouchpadMouse = false;
    private boolean touchscreenMode = false;

    // Game-specific profile locking (null = global profile, "STEAM_123" = locked to game)
    private String lockedToContainer = null;

    private final ArrayList<ControlElement> elements = new ArrayList<>();
    private final ArrayList<ExternalController> controllers = new ArrayList<>();
    private final List<ControlElement> immutableElements = Collections.unmodifiableList(elements);
    private boolean elementsLoaded = false;
    private boolean controllersLoaded = false;
    private boolean virtualGamepad = false;
    private final Context context;
    private GamepadState gamepadState;

    public ControlsProfile(Context context, int id) {
        this.context = context;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCursorSpeed() {
        return cursorSpeed;
    }

    public void setCursorSpeed(float cursorSpeed) {
        this.cursorSpeed = cursorSpeed;
    }

    // Physical controller sensitivity getters/setters
    public float getPhysicalStickDeadZone() {
        return physicalStickDeadZone;
    }

    public void setPhysicalStickDeadZone(float physicalStickDeadZone) {
        this.physicalStickDeadZone = physicalStickDeadZone;
    }

    public float getPhysicalStickSensitivity() {
        return physicalStickSensitivity;
    }

    public void setPhysicalStickSensitivity(float physicalStickSensitivity) {
        this.physicalStickSensitivity = physicalStickSensitivity;
    }

    public float getPhysicalDpadDeadZone() {
        return physicalDpadDeadZone;
    }

    public void setPhysicalDpadDeadZone(float physicalDpadDeadZone) {
        this.physicalDpadDeadZone = physicalDpadDeadZone;
    }

    // Virtual controls sensitivity getters/setters
    public float getVirtualStickDeadZone() {
        return virtualStickDeadZone;
    }

    public void setVirtualStickDeadZone(float virtualStickDeadZone) {
        this.virtualStickDeadZone = virtualStickDeadZone;
    }

    public float getVirtualStickSensitivity() {
        return virtualStickSensitivity;
    }

    public void setVirtualStickSensitivity(float virtualStickSensitivity) {
        this.virtualStickSensitivity = virtualStickSensitivity;
    }

    public float getVirtualDpadDeadZone() {
        return virtualDpadDeadZone;
    }

    public void setVirtualDpadDeadZone(float virtualDpadDeadZone) {
        this.virtualDpadDeadZone = virtualDpadDeadZone;
    }

    // Touchpad gesture getters/setters
    public boolean isEnableTapToClick() {
        return enableTapToClick;
    }

    public void setEnableTapToClick(boolean enableTapToClick) {
        this.enableTapToClick = enableTapToClick;
    }

    public boolean isEnableTwoFingerRightClick() {
        return enableTwoFingerRightClick;
    }

    public void setEnableTwoFingerRightClick(boolean enableTwoFingerRightClick) {
        this.enableTwoFingerRightClick = enableTwoFingerRightClick;
    }

    // Mouse and touch behavior getters/setters
    public boolean isDisableTouchpadMouse() {
        return disableTouchpadMouse;
    }

    public void setDisableTouchpadMouse(boolean disableTouchpadMouse) {
        this.disableTouchpadMouse = disableTouchpadMouse;
    }

    public boolean isTouchscreenMode() {
        return touchscreenMode;
    }

    public void setTouchscreenMode(boolean touchscreenMode) {
        this.touchscreenMode = touchscreenMode;
    }

    // Game locking getters/setters
    public String getLockedToContainer() {
        return lockedToContainer;
    }

    public void setLockedToContainer(String lockedToContainer) {
        this.lockedToContainer = lockedToContainer;
    }

    public boolean isLockedToGame() {
        return lockedToContainer != null && !lockedToContainer.isEmpty();
    }

    public boolean isVirtualGamepad() {
        return virtualGamepad;
    }

    public void setVirtualGamepad(boolean isVirtualGamepad) {
        virtualGamepad = isVirtualGamepad;
    }

    public GamepadState getGamepadState() {
        if (gamepadState == null) gamepadState = new GamepadState();
        return gamepadState;
    }

    public ExternalController addController(String id) {
        ExternalController controller = getController(id);
        if (controller == null) {
            // Check if controller exists in the static list
            controller = ExternalController.getController(id);
            // If still null, create a new controller with the given id
            if (controller == null) {
                controller = new ExternalController();
                controller.setId(id);
                controller.setName("Default Physical Controller");
            }
            controllers.add(controller);
        }
        controllersLoaded = true;
        return controller;
    }

    public void removeController(ExternalController controller) {
        if (!controllersLoaded) loadControllers();
        controllers.remove(controller);
    }

    public ExternalController getController(String id) {
        if (!controllersLoaded) loadControllers();
        for (ExternalController controller : controllers) if (controller.getId().equals(id)) return controller;
        return null;
    }

    public ExternalController getController(int deviceId) {
        if (!controllersLoaded) loadControllers();
        for (ExternalController controller : controllers) if (controller.getDeviceId() == deviceId) return controller;
        return null;
    }

    public ArrayList<ExternalController> getControllers() {
        if (!controllersLoaded) loadControllers();
        return controllers;
    }

    public ExternalController getOrCreateController(String id, String name) {
        ExternalController controller = getController(id);
        if (controller == null) {
            controller = new ExternalController();
            controller.setId(id);
            controller.setName(name);
            controllers.add(controller);
            controllersLoaded = true;
        }
        return controller;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(ControlsProfile o) {
        return Integer.compare(id, o.id);
    }

    public boolean isElementsLoaded() {
        return elementsLoaded;
    }

    /**
     * Force reload elements from disk, discarding any in-memory changes.
     * Use this when switching profiles or when you need to discard edits.
     */
    public void reloadElements(InputControlsView inputControlsView) {
        elementsLoaded = false;  // Reset flag to allow reload
        loadElements(inputControlsView);
    }

    public synchronized void save() {
        File file = getProfileFile(context, id);

        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", name);
            data.put("cursorSpeed", Float.valueOf(cursorSpeed));

            // Save sensitivity settings
            data.put("physicalStickDeadZone", Float.valueOf(physicalStickDeadZone));
            data.put("physicalStickSensitivity", Float.valueOf(physicalStickSensitivity));
            data.put("physicalDpadDeadZone", Float.valueOf(physicalDpadDeadZone));
            data.put("virtualStickDeadZone", Float.valueOf(virtualStickDeadZone));
            data.put("virtualStickSensitivity", Float.valueOf(virtualStickSensitivity));
            data.put("virtualDpadDeadZone", Float.valueOf(virtualDpadDeadZone));

            // Save touchpad gesture settings
            data.put("enableTapToClick", Boolean.valueOf(enableTapToClick));
            data.put("enableTwoFingerRightClick", Boolean.valueOf(enableTwoFingerRightClick));

            // Save mouse and touch behavior settings
            data.put("disableTouchpadMouse", Boolean.valueOf(disableTouchpadMouse));
            data.put("touchscreenMode", Boolean.valueOf(touchscreenMode));

            // Save game locking
            if (lockedToContainer != null) {
                data.put("lockedToContainer", lockedToContainer);
            }

            JSONArray elementsJSONArray = new JSONArray();
            if (!elementsLoaded && file.isFile()) {
                JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
                elementsJSONArray = profileJSONObject.getJSONArray("elements");
            }
            else {
                for (ControlElement element : elements) elementsJSONArray.put(element.toJSONObject());
            }
            data.put("elements", elementsJSONArray);

            JSONArray controllersJSONArray = new JSONArray();
            if (!controllersLoaded && file.isFile()) {
                JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
                if (profileJSONObject.has("controllers")) controllersJSONArray = profileJSONObject.getJSONArray("controllers");
            }
            else {
                for (ExternalController controller : controllers) {
                    JSONObject controllerJSONObject = controller.toJSONObject();
                    if (controllerJSONObject != null) {
                        controllersJSONArray.put(controllerJSONObject);
                    }
                }
            }
            if (controllersJSONArray.length() > 0) data.put("controllers", controllersJSONArray);

            FileUtils.writeString(file, data.toString());
        }
        catch (JSONException e) {
            Log.e("ControlsProfile", "Error saving profile: " + e.getMessage(), e);
        }
    }

    public static File getProfileFile(Context context, int id) {
        return new File(InputControlsManager.getProfilesDir(context), "controls-"+id+".icp");
    }

    public void addElement(ControlElement element) {
        elements.add(element);
        elementsLoaded = true;
    }

    public void removeElement(ControlElement element) {
        elements.remove(element);
        elementsLoaded = true;
    }

    public List<ControlElement> getElements() {
        return immutableElements;
    }

    public boolean isTemplate() {
        return name.toLowerCase(Locale.ENGLISH).contains("template");
    }

    public ArrayList<ExternalController> loadControllers() {
        controllers.clear();
        controllersLoaded = false;

        File file = getProfileFile(context, id);

        if (!file.isFile()) {
            return controllers;
        }

        try {
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            if (!profileJSONObject.has("controllers")) {
                return controllers;
            }
            JSONArray controllersJSONArray = profileJSONObject.getJSONArray("controllers");

            for (int i = 0; i < controllersJSONArray.length(); i++) {
                JSONObject controllerJSONObject = controllersJSONArray.getJSONObject(i);
                String id = controllerJSONObject.getString("id");
                ExternalController controller = new ExternalController();
                controller.setId(id);
                controller.setName(controllerJSONObject.getString("name"));

                JSONArray controllerBindingsJSONArray = controllerJSONObject.getJSONArray("controllerBindings");

                for (int j = 0; j < controllerBindingsJSONArray.length(); j++) {
                    JSONObject controllerBindingJSONObject = controllerBindingsJSONArray.getJSONObject(j);
                    ExternalControllerBinding controllerBinding = new ExternalControllerBinding();
                    int keyCode = controllerBindingJSONObject.getInt("keyCode");
                    String bindingName = controllerBindingJSONObject.getString("binding");
                    controllerBinding.setKeyCode(keyCode);
                    controllerBinding.setBinding(Binding.fromString(bindingName));
                    controller.addControllerBinding(controllerBinding);
                }
                controllers.add(controller);
            }
            controllersLoaded = true;
        }
        catch (JSONException e) {
            Log.e("ControlsProfile", "Error loading controllers: " + e.getMessage(), e);
            e.printStackTrace();
        }
        return controllers;
    }

    public synchronized void loadElements(InputControlsView inputControlsView) {
        // Prevent reloading if already loaded (to preserve in-memory edits)
        if (elementsLoaded) {
            return;
        }

        File file = getProfileFile(context, id);

        if (!file.isFile()) {
            elementsLoaded = true;  // Mark as loaded to prevent repeated load attempts
            return;
        }

        // Only clear and reset after confirming file exists
        elements.clear();
        virtualGamepad = false;

        try {
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            JSONArray elementsJSONArray = profileJSONObject.getJSONArray("elements");

            for (int i = 0; i < elementsJSONArray.length(); i++) {
                JSONObject elementJSONObject = elementsJSONArray.getJSONObject(i);
                ControlElement element = new ControlElement(inputControlsView);
                element.setType(ControlElement.Type.valueOf(elementJSONObject.getString("type")));
                element.setShape(ControlElement.Shape.valueOf(elementJSONObject.getString("shape")));
                element.setToggleSwitch(elementJSONObject.getBoolean("toggleSwitch"));
                element.setX((int)(elementJSONObject.getDouble("x") * inputControlsView.getMaxWidth()));
                element.setY((int)(elementJSONObject.getDouble("y") * inputControlsView.getMaxHeight()));
                element.setScale((float)elementJSONObject.getDouble("scale"));
                element.setText(elementJSONObject.getString("text"));
                element.setIconId(elementJSONObject.getInt("iconId"));
                if (elementJSONObject.has("range")) element.setRange(ControlElement.Range.valueOf(elementJSONObject.getString("range")));
                if (elementJSONObject.has("orientation")) element.setOrientation((byte)elementJSONObject.getInt("orientation"));

                boolean hasGamepadBinding = true;
                JSONArray bindingsJSONArray = elementJSONObject.getJSONArray("bindings");
                for (int j = 0; j < bindingsJSONArray.length(); j++) {
                    Binding binding = Binding.fromString(bindingsJSONArray.getString(j));
                    element.setBindingAt(j, Binding.fromString(bindingsJSONArray.getString(j)));
                    if (!binding.isGamepad()) hasGamepadBinding = false;
                }

                if (!virtualGamepad && hasGamepadBinding) virtualGamepad = true;
                elements.add(element);
            }
            elementsLoaded = true;
        }
        catch (JSONException e) {
            Log.e("ControlsProfile", "Error loading elements: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
}

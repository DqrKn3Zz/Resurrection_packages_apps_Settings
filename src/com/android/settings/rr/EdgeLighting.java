/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at
          http://www.apache.org/licenses/LICENSE-2.0
     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.os.UserHandle;
import android.provider.Settings;
import android.net.Uri;

import com.android.settings.Utils;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;

import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.preview.AmbientLightSettingsPreview;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class EdgeLighting extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String PULSE_AMBIENT_LIGHT_CUSTOM_COLOR = "ambient_light_custom_color";
    private static final String PULSE_AMBIENT_LIGHT_BLEND_COLOR = "ambient_light_blend_color";
    private static final String PULSE_AMBIENT_LIGHT_COLOR = "ambient_light_color";
    private static final String PULSE_AOD = "ambient_notification_light_hide_aod";
    private static final String PULSE_ALL = "ambient_light_pulse_for_all";

    private SystemSettingColorPickerPreference mEdgeLightColorPreference;
    private SystemSettingColorPickerPreference mEdgeLightColorBlendPreference;
    private ListPreference mColorType;
    private SystemSettingSwitchPreference mAodPulse;
    private SystemSettingSwitchPreference mPulseAll;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edge_lighting);
        ContentResolver resolver = getActivity().getContentResolver();

        mEdgeLightColorPreference = (SystemSettingColorPickerPreference) findPreference(PULSE_AMBIENT_LIGHT_CUSTOM_COLOR);
        mEdgeLightColorBlendPreference = (SystemSettingColorPickerPreference) findPreference(PULSE_AMBIENT_LIGHT_BLEND_COLOR);
        mEdgeLightColorBlendPreference.setAlphaSliderEnabled(false);
        int edgeblendColor = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_BLEND_COLOR, 0xFF3980FF);
        String sum = convertToRGB(edgeblendColor);
        mEdgeLightColorBlendPreference.setSummary(sum);
        mEdgeLightColorBlendPreference.setOnPreferenceChangeListener(this);

        mPulseAll = (SystemSettingSwitchPreference) findPreference(PULSE_ALL);
        mAodPulse = (SystemSettingSwitchPreference) findPreference(PULSE_AOD);
        boolean show = Settings.System.getInt(resolver,
                Settings.System.AMBIENT_NOTIFICATION_LIGHT_HIDE_AOD, 0) == 1;
        mAodPulse.setOnPreferenceChangeListener(this);
        updatePulse(show);

        mColorType = (ListPreference) findPreference(PULSE_AMBIENT_LIGHT_COLOR);
        int type = Settings.System.getInt(resolver,
            Settings.System.AMBIENT_LIGHT_COLOR, 0);
        mColorType.setValue(String.valueOf(type));
        mColorType.setSummary(mColorType.getEntry());
        updateprefs(type);
        boolean isAccent = type == 2;
        if (type == 4) {
            int color = mixColors(getCustomColor(), edgeblendColor);
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(color);
        } else {
            updateEdgeLightColorPreferences(isAccent);
        }
        mColorType.setOnPreferenceChangeListener(this);

        int edgeLightColor = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_CUSTOM_COLOR, 0xFF3980FF);
        mEdgeLightColorPreference.setNewPreviewColor(edgeLightColor);
        mEdgeLightColorPreference.setAlphaSliderEnabled(false);
        String edgeLightColorHex = convertToRGB(edgeLightColor);
        mEdgeLightColorPreference.setSummary(edgeLightColorHex);
        mEdgeLightColorPreference.setOnPreferenceChangeListener(this);
    }

    private void updatePulse(boolean enabled) {
       if (enabled) {
           mPulseAll.setEnabled(false);
       } else {
           mPulseAll.setEnabled(true);
       }
    }

   public int getCustomColor() {
     return Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_CUSTOM_COLOR, 0xFF3980FF);
   }

   public int getBlendColor() {
     return Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_BLEND_COLOR, 0xFF3980FF);
   }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mColorType) {
            int val = Integer.parseInt((String) newValue);
            int index = mColorType.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_LIGHT_COLOR, val);
            mColorType.setSummary(mColorType.getEntries()[index]);
            updateprefs(val);
            boolean isOn = index == 2;
            if (index == 4) {
               int color = mixColors(getCustomColor(), getBlendColor());
               AmbientLightSettingsPreview.setAmbientLightPreviewColor(color);
            } else {
               updateEdgeLightColorPreferences(isOn);
            }     
            return true;
        }  else if (preference == mEdgeLightColorPreference) {
            String hex = convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(Integer.valueOf(String.valueOf(newValue)));
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.AMBIENT_LIGHT_CUSTOM_COLOR, intHex);
            return true;
        } else if (preference == mEdgeLightColorBlendPreference) {
            String hex = convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int color = mixColors(getCustomColor(), getBlendColor());
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(color);
            return true;
        } else if (preference == mAodPulse) {
            boolean value = (Boolean) newValue;
            updatePulse(value);
            return true;
        } 
       return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public void updateprefs(int type) {
         if (type == 3) {
             mEdgeLightColorPreference.setEnabled(true);
             mEdgeLightColorBlendPreference.setEnabled(false);
         } else if (type == 4) {
             mEdgeLightColorPreference.setEnabled(true);
             mEdgeLightColorBlendPreference.setEnabled(true);
         } else  {
             mEdgeLightColorPreference.setEnabled(false);
             mEdgeLightColorBlendPreference.setEnabled(false);
         }
    }

    public static String convertToRGB(int color) {
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + red + green + blue;
    }

    private int mixColors(int color1, int color2) {
        int[] rgb1 = colorToRgb(color1);
        int[] rgb2 = colorToRgb(color2);

        rgb1[0] = mixedValue(rgb1[0], rgb2[0]);
        rgb1[1] = mixedValue(rgb1[1], rgb2[1]);
        rgb1[2] = mixedValue(rgb1[2], rgb2[2]);
        rgb1[3] = mixedValue(rgb1[3], rgb2[3]);

        return rgbToColor(rgb1);
    }

    private int[] colorToRgb(int color) {
        int[] rgb = {(color & 0xFF000000) >> 24, (color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, (color & 0xFF)};
        return rgb;
    }

    private int rgbToColor(int[] rgb) {
        return (rgb[0] << 24) + (rgb[1] << 16) + (rgb[2] << 8) + rgb[3];
    }

    private int mixedValue(int val1, int val2) {
        return (int)Math.min((val1 + val2), 255f);
    }

    private void updateEdgeLightColorPreferences(boolean useAccentColor) {
        if (useAccentColor) {
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(Utils.getColorAccentDefaultColor(getContext()));
        } else {
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(getCustomColor());
        }
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.edge_lighting;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}

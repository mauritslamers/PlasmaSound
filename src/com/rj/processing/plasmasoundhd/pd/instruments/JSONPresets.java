package com.rj.processing.plasmasoundhd.pd.instruments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.EditText;

import com.rj.processing.plasmasound.PlasmaSound;
import com.rj.processing.plasmasoundhd.PlasmaActivity;

public class JSONPresets {
	public static String PRESETS = "PRESETS";
	public static String JSON_FILENAME = "presets.json";
	public static interface PresetListener {
		public void presetChanged(JSONObject preset);
	}
	
	
	
	private static JSONPresets singleton;
	public static JSONPresets getPresets() {
		if (singleton == null) {
			singleton = new JSONPresets();
		}
		return singleton;
	}

	private ArrayList<PresetListener> listeners;
	
	
	private JSONObject currentsetting;
	
	
	
	public JSONPresets() {
		listeners = new ArrayList<PresetListener>();
	}
	public void addListener(PresetListener listen) {
		this.listeners.add(listen);
	}
	public void removeListener(PresetListener listen) {
		this.listeners.remove(listen);
	}
	public void notifyListeners(JSONObject preset) {
		Log.d("Presets", "Notifying all "+listeners.size()+" listeners");
		for (PresetListener  l : listeners) l.presetChanged(preset);
	}
	
	public JSONObject getCurrent() {
		if (currentsetting == null) {
			JSONObject newsetting = new JSONObject();
			try {
				newsetting.put("name", "Default");
			} catch (JSONException j) {
				j.printStackTrace();
			}
			currentsetting = newsetting;
		}
		return currentsetting;
	}
	
	/**
	 * This is like getCurrent, but will fill in the default with the sharedPreferences if there is no
	 * @param c
	 * @return
	 */
	public JSONObject getCurrent(Context c) {
		if (currentsetting == null) {
			JSONObject newsetting = new JSONObject();
			newsetting = readFromPreferences(c);
			currentsetting = newsetting;
		}
		return currentsetting;
	}

	
	
	
	public JSONObject readFromPreferences(Context c ) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("name", "From Preferences");
			String[] defaults = { "vibspeed", "midimax", "quantize_note_list",
					"delayfeedback", "sustain", "tremolospeed", "tremolodepth",
					"midimin", "waveform", "vibdepth", "filt", "revebrtime",
					"reverbfeedback", "delaytime", "volume", "attack",
					"release", "amp", "filter", "decay" };
			final SharedPreferences mPrefs = c.getSharedPreferences(
					PlasmaSound.SHARED_PREFERENCES_AUDIO, 0);
			for (String s : defaults) {
				if (mPrefs.contains(s)) {
					obj.put(s, mPrefs.getInt(s, 0));
				}
				if (mPrefs.contains(s+"_y")) {
					obj.put(s+"_y", mPrefs.getBoolean(s+"_y", false));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	
	
	

	public void showLoadMenu(final Context c, final PlasmaActivity p) {
		try {
			final String[] items = getPresetNames(c);
			int selection = -1;
			int i=0;
			for (String item : items) {
				if (currentsetting != null && item.equals(currentsetting.get("name"))) {
					selection = i;
				}
				i++;
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(c);
			builder.setTitle("Pick a saved instance");
			AlertDialog alert;
			builder.setSingleChoiceItems(items, selection, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        loadPreset(c,p.getInst(), items[item]);
			        dialog.dismiss();
			    }
			});
			alert = builder.create();
			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void showSaveMenu(final Context c, final PlasmaActivity p) {
		try {
			final String[] items = getPresetNames(c);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(c);
			builder.setTitle("Pick a saved instance");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        savePreset(c,p.getInst(), items[item]);
			    }
			});
			builder.setPositiveButton("New", new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int whichButton) {  
					showSaveAsMenu(c, p);
				}
				}); 
			builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int whichButton) {  
					showDeleteMenu(c, p);
				}
				}); 

			AlertDialog alert = builder.create();
			alert.show();
		} catch (Exception e) {
			
		}
	}
	
	public void showDeleteMenu(final Context c, final PlasmaActivity p) {
		try {
			final String[] items = getPresetNames(c);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(c);
			builder.setTitle("Pick a preset to delete.  WARNING: IT'S FINAL");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        deletePreset(c,p.getInst(), items[item]);
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		} catch (Exception e) {
			
		}
	}

	
	public void showSaveAsMenu(final Context c, final PlasmaActivity p ) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle("Name?");
		builder.setMessage("Pick a name for the preset");
		final EditText text = new EditText(c);
		builder.setView(text);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
			  String value = text.getText().toString();  
			  savePreset(c, p.getInst(), value);
			}  
			}); 
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	
	public void loadDefault(Context c, Instrument e) {
		JSONObject jpreset = getDefaultPreset(c);
		currentsetting = jpreset;
		final SharedPreferences mPrefs = c.getSharedPreferences(
				PlasmaSound.SHARED_PREFERENCES_AUDIO, 0);
		e.updateSettingsFromJSON(jpreset, true, mPrefs);
		this.notifyListeners(jpreset);
	}
	
	public void loadPreset(Context c, Instrument e, String preset) {
		JSONObject jpreset = getPresetFromName(preset, c);
		currentsetting = jpreset;
		final SharedPreferences mPrefs = c.getSharedPreferences(
				PlasmaSound.SHARED_PREFERENCES_AUDIO, 0);
		e.updateSettingsFromJSON(jpreset, true, mPrefs);
		this.notifyListeners(jpreset);
	}
	
	public void savePreset(Context c, Instrument e, String preset) {
		try {
			JSONObject presetsobj = getPresets(c);
			if (presetsobj == null) {
				presetsobj = new JSONObject();
				JSONObject presets = new JSONObject();
				presetsobj.put("presets", presets);
			}
			if (currentsetting != null) {
				presetsobj.put("default", currentsetting.getString("name"));
			}
			JSONObject presets = presetsobj.getJSONObject("presets");
			JSONObject presetobj = new JSONObject();
			presetobj.put("name", preset);
			presetobj = e.saveSettingsToJSON(presetobj);
			presets.put(preset, presetobj);
			writePresets(presetsobj, c);
		} catch (Exception j ) {
			j.printStackTrace();
		}
	}
	

	public void deletePreset(Context c, Instrument e, String preset) {
		try {
			JSONObject presetsobj = getPresets(c);
			JSONObject presets = presetsobj.getJSONObject("presets");
			presets.remove(preset);
			writePresets(presetsobj, c);
		} catch (Exception j ) {
			j.printStackTrace();
		}
	}
	
	public void updateDefault(Context c) {
		try {
			JSONObject presetsobj = getPresets(c);
			if (currentsetting != null) {
				presetsobj.put("default", currentsetting.getString("name"));
				writePresets(presetsobj, c);
			}
		} catch (Exception j ) {
			j.printStackTrace();
		}
	}

	
	
	
	public JSONObject getPresets(Context context) {
		try {
			File jsonFile = new File(context.getFilesDir(), JSON_FILENAME);
			if (!jsonFile.exists()) return null;
		    byte[] buffer = new byte[(int) jsonFile.length()];
		    BufferedInputStream f = new BufferedInputStream(new FileInputStream(jsonFile));
		    f.read(buffer);		
			String jsonString = new String(buffer);
			Log.d("Presets", "Presets:\n" +jsonString);
			JSONObject object = new JSONObject(jsonString);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void writePresets(JSONObject json, Context context) {
		try {
			File jsonFile = new File(context.getFilesDir(), JSON_FILENAME);
			
			String out = json.toString(4);
			Log.d("Presets", "Presets:"+out);
		    BufferedOutputStream f = new BufferedOutputStream(new FileOutputStream(jsonFile));
		    f.write(out.getBytes());
		    f.flush();
		    f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public JSONObject getDefaultPreset(Context context) {
		try {
			JSONObject presetobj = getPresets(context);
			if (presetobj == null) return null;
			if (presetobj.has("default")) {
				String defaultname = presetobj.getString("default");
				return getPresetFromName(defaultname, context);
			}		
		} catch (JSONException j ) {
			j.printStackTrace();
		}
		 return null;
	}

	
	public JSONObject getPresetFromName(String name, Context context) {
		try {
			JSONObject presetobj = getPresets(context);
			if (presetobj == null) return null;
			JSONObject presets = presetobj.getJSONObject("presets");
			return presets.getJSONObject(name);
		} catch (JSONException j ) {
			j.printStackTrace();
		}
		 return null;
	}
	
	public String[] getPresetNames(Context context) {
		try {
			JSONObject presetobj = getPresets(context);
			if (presetobj == null) return null;
			JSONObject presets = presetobj.getJSONObject("presets");
			String[] presetnames = new String[presets.length()];
			int i = 0;
			Iterator iter = presets.keys();
			while (iter.hasNext()) {
				String key = (String)iter.next();
				JSONObject preset = presets.getJSONObject(key);
				presetnames[i] = preset.getString("name");
				i++;
			}
		return presetnames;
		} catch (JSONException j) {
			j.printStackTrace();
		}
		return null;
	}
}

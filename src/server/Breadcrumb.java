package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import server.data.NoWarningJSONArray;
import server.data.NoWarningJSONObject;

public class Breadcrumb {
	public String href;
	public String name;
	public boolean active;

	public Breadcrumb() {			
	}
	
	public Breadcrumb(String href, String name, boolean active) {
		this.href = href;
		this.name = name;
		this.active = active;
	}
	
	public String toString() {
		return this.toJSON().toJSONString();
	}
	
	public JSONObject toJSON() {
		NoWarningJSONObject result = new NoWarningJSONObject();
		result.put("href", this.href);
		result.put("name", this.name);
		result.put("active", this.active);
		return result;
	}

	// Transforms the full path to a folder into an array of breadcrumb objects to be used with Bootstrap
	// assumes that fullPath starts with '/', e.g. "/a/b/c" or "/"
	public static Breadcrumb[] getBreadcrumbs(String fullPath) {
		String sep = "/";
		String[] parts = fullPath.split(sep);
		String HOME = "HOME";
		if (parts.length == 0) {
			return new Breadcrumb[] { new Breadcrumb(sep , HOME, true) };
		}
		StringBuffer hrefBuffer = new StringBuffer();
		int partsLength = parts.length;
		Breadcrumb[] results = new Breadcrumb[partsLength];
		for (int i = 0; i < partsLength; i++) {
			results[i] = new Breadcrumb();
			if (parts[i].length() == 0) { //The 0th element is empty because the path starts with '/'
				results[i].href = sep;
				results[i].name = HOME;
				results[i].active = false;
			}
			else {
				hrefBuffer.append(parts[i]);
				results[i].href = hrefBuffer.toString();
				results[i].name = parts[i];
				results[i].active = false;
			}
			if (i != partsLength - 1) {
				hrefBuffer.append(sep);
			}
			else {
				results[i].active = true;
			}
		}
		return results;
	}
	
	public static JSONArray getJSONArray(Breadcrumb[] input) {
		NoWarningJSONArray array = new NoWarningJSONArray();
		for (int i = 0; i < input.length; i++) {
			array.add(input[i]);
		}
		return array;
	}
	
	public static JSONArray getBreadcrumbsAsJSON(String fullPath) {
		return getJSONArray(getBreadcrumbs(fullPath));
	}
	
}


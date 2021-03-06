package server.data;

import org.json.simple.JSONArray;
/**
 * Subclass of {@link JSONArray} that suppresses the warnings
 * @author soleksiy
 *
 */
public class NoWarningJSONArray extends JSONArray {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 3156531521970830316L;
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean add(Object e) {
		return super.add(e);
	}
		
}

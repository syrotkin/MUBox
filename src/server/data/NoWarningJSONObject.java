package server.data;

import org.json.simple.JSONObject;
/**
 * Subclass of {@link JSONObject} that suppresses the warnings
 * @author soleksiy
 *
 */
public class NoWarningJSONObject extends JSONObject {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 5420314117919964728L;
	
	@Override
	@SuppressWarnings("unchecked")
	public Object put(Object key, Object value) {
		return super.put(key, value);
	}

}

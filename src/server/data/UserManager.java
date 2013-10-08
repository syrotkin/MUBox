package server.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;

import server.model.User;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
/**
 * Class for managing users, works with the users collection.
 * @author soleksiy
 *
 */
public class UserManager {
	DatabaseManager dbManager;
	
	private static final String USERS = "users";
	private static final int DEFAULT_LIMIT = 100;
	
	private static final String UID = "uid";
	private static final String DISPLAY_NAME = "display_name";
	private static final String IMG = "img";
	
	public UserManager(DatabaseManager dbm) {
		this.dbManager = dbm;
	}
	/**
	 * Get display name for users
	 * @param uid User UID
	 * @param users Map from user UIDs to display names.
	 * @return User's display name.
	 */
	public String getDisplayName(String uid, Map<String, String> users) {
		String userName = users.get(uid);
		if (userName == null) {
			userName = getDisplayName(uid);
			users.put(uid, userName);
		}
		return userName;
	}
	
	private String getDisplayName(String uid) {
		DBCollection users = dbManager.getCollection(USERS);
		BasicDBObject query = new BasicDBObject(UID, uid);
		String displayName = null;
		try(DBCursor dbCursor = users.find(query).limit(DEFAULT_LIMIT)) {
			while (dbCursor.hasNext()) {
				DBObject current = dbCursor.next();
				displayName = (String)current.get(DISPLAY_NAME);
				break;
			}
		}
		return displayName;
	}
	/**
	 * Lists cloud storage providers based on what providers are available in the users collection.
	 * @return JSON array of providers
	 */
	public JSONArray listProviders() {
		NoWarningJSONArray result = new NoWarningJSONArray();
		DBCollection users = dbManager.getCollection(USERS);
		List<?> providers = users.distinct("provider");
		for (Object provider : providers) {
			if (provider instanceof String) {
				result.add(provider);
			}
		}
		return result;
	}
	/**
	 * Lists users for a specific cloud storage provider
	 * @param provider Cloud storage provider (e.g. "dropbox", "google"
	 * @return array of providers
	 */
	public JSONArray listUsers(String provider) { // for now: [{uid: <uid>, display_name: <display_name>}, ...]
		NoWarningJSONArray list = new NoWarningJSONArray();		
		DBCollection users = dbManager.getCollection(USERS);
		BasicDBObject query;
		if (provider == null) {
			query = new BasicDBObject(); // vacuous
		}
		else {
			query = new BasicDBObject("provider", provider);
		}
		try (DBCursor dbCursor = users.find(query).limit(DEFAULT_LIMIT)) {
			while (dbCursor.hasNext()) {
				DBObject currentDBObject = dbCursor.next();
				String uid = (String)currentDBObject.get(UID);
				String displayName = (String)currentDBObject.get(DISPLAY_NAME);
				NoWarningJSONObject currentJSONObject = new NoWarningJSONObject();
				currentJSONObject.put(UID, uid);
				currentJSONObject.put(DISPLAY_NAME, displayName);
				list.add(currentJSONObject);
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());;
			e.printStackTrace();
		}
		return list;
	}
	
	private List<User> listUsers(BasicDBList userUidList, String ownerUid, boolean excludeOwner) {
		List<User> results = new ArrayList<User>();
		BasicDBObject uidCriteria = new BasicDBObject("$in", userUidList); 
		if (excludeOwner) {
			uidCriteria.append("$ne", ownerUid); // excluding yourself, the owner
		}
		BasicDBObject query = new BasicDBObject(UID, uidCriteria);
		DBCollection usersCollection = dbManager.getCollection(USERS);
		try (DBCursor dbCursor = usersCollection.find(query)) {
			while (dbCursor.hasNext()) {
				DBObject next = dbCursor.next();
				String uid = (String)next.get(UID);
				String userName = (String)next.get(DISPLAY_NAME);
				String img = (String)next.get(IMG);
				User user = new User();
				user.setUid(uid);
				user.setDisplayName(userName);
				if (img != null) {
					user.setImg(img);
				}
				results.add(user);
			}
		}
		return results;
	}
	/**
	 * Retrieves User objects based on the list of user UIDs.
	 * @param userUidInputList List of user UIDs.
	 * @param ownerUid The UID of the folder owner.
	 * @param excludeOwner True if the returned list should exclude the owner.
	 * @return List of users.
	 */
	public List<User> listUsers(List<String> userUidInputList, String ownerUid, boolean excludeOwner) {
		BasicDBList userUids = new BasicDBList();
		for (String userUid : userUidInputList) {
			userUids.add(userUid);
		}
		return listUsers(userUids, ownerUid, excludeOwner);
	}
	/**
	 * Retrieves the user from the database based on the user UID.
	 * @param uid User UID
	 * @return User
	 */
	public User getUser(String uid) {
		DBCollection userCollection = dbManager.getCollection(USERS, User.class);
		BasicDBObject query = new BasicDBObject(UID, uid);
		User user = (User)userCollection.findOne(query);
		return user;
	}
	/**
	 * Saves a user in the database.
	 * @param user User to save
	 */
	public void saveUser(User user) {
		DBCollection userCollection = dbManager.getCollection(USERS, User.class);
		BasicDBObject query = new BasicDBObject(UID, user.getUid());
		User matchingUser = (User)userCollection.findOne(query);
		if (matchingUser == null) {
			userCollection.save(user);
		}
		else {
			matchingUser.setAccessKey(user.getAccessKey());
			matchingUser.setAccessSecret(user.getAccessSecret());
			matchingUser.setRefreshToken(user.getRefreshToken());
			matchingUser.setEmail(user.getEmail());
			matchingUser.setProvider(user.getProvider());
			matchingUser.setImg(user.getImg());

			userCollection.save(matchingUser);
		}
	}

} // end class UserManager

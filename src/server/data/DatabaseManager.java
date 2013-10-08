package server.data;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
/**
 * Class for interacting with the database
 * @author soleksiy
 *
 */
public class DatabaseManager {
	private final static String SERVER = "localhost";
	private final static int PORT = 27017;
	private final static String DATABASE = "mydb";
	
	private static DatabaseManager instance = null;
	private DB db = null;
	private MongoClient client = null;
	private static Object lock = new Object();
			
	private DatabaseManager() {
		try {
			this.client = new MongoClient(SERVER, PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot connect to the database.");
		}
		this.db = client.getDB(DATABASE);
	}
	/**
	 * Gets an instance of DatabaseManager. As per MongoDB documentation, there should be one {@link MongoClient} per JVM.
	 * @return An instance of DatabaseManager
	 */
	public static DatabaseManager getInstance() {
		if (instance != null) {
			return instance;
		}
		synchronized (lock) {
			if (instance == null) {
				instance = new DatabaseManager();
			}
		}
		return instance;
	}
	/**
	 * Gets a database collection by name.
	 * @param name Name of the collection
	 * @return Database collection
	 */
	public DBCollection getCollection(String name) {
		return this.db.getCollection(name);
	}
	/**
	 * Gets a collection by name and sets the type of the entities retrieved from the collection to a specified type for type-safe retrieval.
	 * @param name Collection name
	 * @param type Type of the entities
	 * @return Database collection
	 */
	public <T> DBCollection getCollection(String name, Class<T> type) {
		DBCollection collection = getCollection(name);
		collection.setObjectClass(type);		
		return collection;
	}

} // class

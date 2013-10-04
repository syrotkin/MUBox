package server.data;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

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
		
	public DBCollection getCollection(String name) {
		return this.db.getCollection(name);
	}
	
	public <T> DBCollection getCollection(String name, Class<T> type) {
		DBCollection collection = getCollection(name);
		collection.setObjectClass(type);		
		return collection;
	}

} // class

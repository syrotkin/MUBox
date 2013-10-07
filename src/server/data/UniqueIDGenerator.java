package server.data;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * Generates unique IDs for shared folders and voting. This class retrieved the previously stored unique ID, increments it by 1, and stores the result in the database.
 *
 */ 
public class UniqueIDGenerator {
	
	private static final String UNIQUEIDS = "uniqueids";
	private static final String UNIQUEID = "uniqueid";
	
	private DatabaseManager dbManager;
	
	public UniqueIDGenerator(DatabaseManager dbm) {
		this.dbManager = dbm;
	}
	
	public long getUniqueID(String collection) {
		DBCollection folderseq = dbManager.getCollection(UNIQUEIDS);
		BasicDBObject query = new BasicDBObject("_id", collection);
		BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject(UNIQUEID, 1L));
		DBObject result = folderseq.findAndModify(query, null, null, false, update, false, false); // only update, and return the old value, like i++
		long newID = (long)result.get(UNIQUEID);
		return newID;
	}
	
}

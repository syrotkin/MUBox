package server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;


import server.data.DatabaseManager;
import server.data.UserManager;
import server.model.FileActivity;
import server.model.FileEntry;
import server.model.User;

/**
 * Manages the data displayed in the activity view 
 */
public class ActivityManager {
	private static final String FILEDATA = "filedata";
	
	DatabaseManager dbManager;
	UserManager userManager;
	
	public ActivityManager(DatabaseManager dbManager, UserManager userManager) {
		this.dbManager = dbManager;
		this.userManager = userManager;
	}
			
	public List<FileActivity> listActivities(String path, String userUid) {
		int topN = 100;
		String pathPart = "/".equals(path) ? "/" : path + "/";
		String escapedPath = Pattern.quote(pathPart);
		//String regex = "^" +pathPart + "[^/]+$"; // only children
		String regex = "^" +escapedPath + ".+$"; // all descendants
		List<FileEntry> deletionList = new ArrayList<FileEntry>();
		List<FileEntry> creationList = new ArrayList<FileEntry>();
		Map<String, String> userMap = new HashMap<String, String>();

		findLatestActivity(regex, "deletionDate", topN, deletionList, userUid, userMap);
		System.out.println("deletionList size: " + deletionList.size());
		printList(deletionList);
		findLatestActivity(regex, "creationDate", topN, creationList, userUid, userMap);
		System.out.println("creationList size: " + creationList.size());
		printList(creationList);
		List<FileActivity> result = mergeResults(deletionList, creationList, path);
		System.out.println("merged list size: " + result.size());
		return result;
	}
	
	private void printList(List<FileEntry> list) {
		for (FileEntry fe : list) {
			System.out.println("\t" + fe.getID() + ": " + fe.getFilename() + ", " + fe.getPath() +  
					", creation user: " + fe.getCreationUid() + ", creation: " + fe.getCreationDate() + 
					", deletion user: " +fe.getDeletionUid() + ", deletion: " + fe.getDeletionDate());
		}
	}

	private List<FileActivity> mergeResults(List<FileEntry> deletionList, List<FileEntry> creationList, String parentPath) {
		List<FileActivity> result = new ArrayList<FileActivity>();
		int i = 0; 
		int j = 0;
		FileActivity activity = null;
		
		while (i < deletionList.size() || j < creationList.size()) {
			Date deletion = null; 
			Date creation = null; 
			if (i < deletionList.size()) {
				deletion = deletionList.get(i).getDeletionDate();
			}
			if (j < creationList.size()) {
				creation = creationList.get(j).getCreationDate();
			}
			if (deletion != null && creation != null) {
				if (deletion.after(creation) || deletion.equals(creation)) {
					String deletionAction = deletionList.get(i).getDeletionAction();
					if (!"rename".equals(deletionAction) && !"move".equals(deletionAction)) { // do not record "rename" twice, only record it as creation. NOTE: Added "move" here, so that it is not recorded twice either
						// Not recording move twice makes sense if we include subfolders
						activity = createActivity(deletionList.get(i), true, parentPath); 
						result.add(activity);
					}
					i++;
				}
				else {
					activity = createActivity(creationList.get(j), false, parentPath);
					result.add(activity);
					j++;
				}
			}
			else if (deletion == null && creation != null) {
				activity = createActivity(creationList.get(j), false, parentPath);
				result.add(activity);
				j++;
			}
			else if (deletion != null && creation == null) {
				String deletionAction = deletionList.get(i).getDeletionAction();
				if (!"rename".equals(deletionAction) && !"move".equals(deletionAction)) { // do not record "rename" twice, only record it as creation. NOTE: Added "move" here, so it is only recorded as creation
					// Not recording move twice makes sense if we include subfolders.
					activity = createActivity(deletionList.get(i), true, parentPath);
					result.add(activity);
				}
				i++;
			}
			else if (deletion == null && creation == null) {
				i++;
				j++;
			}
		}
		return result;
	}
	
	private FileActivity createActivity(FileEntry fileEntry, boolean isDeletion, String parentPath) {
		FileActivity activity = new FileActivity();
		activity.setFilename(fileEntry.getFilename());
		
		activity.setPath(fileEntry.getPath().replaceFirst(Pattern.quote(parentPath), ""));
		if (isDeletion) {
			activity.setUserName(fileEntry.getDeletionUserName());
			activity.setImg(fileEntry.getDeletionUserImg());
			activity.setAction(fileEntry.getDeletionAction());
			activity.setDate(fileEntry.getDeletionDate());
		}
		else {
			activity.setUserName(fileEntry.getCreationUserName());
			activity.setImg(fileEntry.getCreationUserImg());
			activity.setAction(fileEntry.getCreationAction());
			activity.setDate(fileEntry.getCreationDate());
		}
		activity.setDetails(getActivityDetails(fileEntry, isDeletion, parentPath));
		return activity;
	}
	
	private String getActivityDetails(FileEntry fileEntry, boolean deletion, String parentPath) {
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy h:mm:ss a");
		StringBuffer buffer = new StringBuffer();
		buffer.append(fileEntry.getFilename()).append(": ");
		
		if (deletion) {
			String deletionAction = fileEntry.getDeletionAction();
			if (deletionAction == null) {
				deletionAction = "Deleted in cloud provider.";
			}
			String deletionDate = dateFormat.format(fileEntry.getDeletionDate());
			String deletionUserInfo = fileEntry.getDeletionUserName() !=  null ? fileEntry.getDeletionUserName() + ", " : "";
			if ("delete".equals(deletionAction)) {
				buffer.append("Deleted: ");
			}
			else if ("rename".equals(deletionAction)) {
				if (fileEntry.getNewFileName() == null) {
					buffer.append("Renamed:");
				}
				else {
					buffer.append("Renamed to ").append(fileEntry.getNewFileName()).append(": ");
				}
			}
			else if ("move".equals(deletionAction)) {
				if (fileEntry.getNewParent() == null) {
					buffer.append("Moved:");
				}
				else {
					String newParent= fileEntry.getNewParent().replaceFirst(Pattern.quote(parentPath), "");
					if ("".equals(newParent)) {
						newParent = "/";
					}
					buffer.append("Moved to ").append(newParent).append(": ");
				}
			}
			else {
				buffer.append(deletionAction).append(": ");
			}
			buffer.append(deletionUserInfo).append(deletionDate);
		}
		else {
			String creationAction = fileEntry.getCreationAction();
			if (creationAction == null) {
				creationAction = "Created in Dropbox ";
			}
			//String creationDate = DateFormatter.formatDate(fileEntry.getCreationDate());
			String creationDate = dateFormat.format(fileEntry.getCreationDate());
			String creationUserInfo = fileEntry.getCreationUserName() != null ? fileEntry.getCreationUserName() + ", " : "";
			if ("upload".equals(creationAction)) {
				buffer.append("Added: ");
			} 
			else if ("edit".equals(creationAction)) {
				buffer.append("Modified: ");
			}
			else if ("newfolder".equals(creationAction)) {
				buffer.append("Created: ");
			}
			else if ("copy".equals(creationAction)) {
				if (fileEntry.getOldParent() == null) {
					buffer.append("Copied:");
				}
				else {
					String oldParent = fileEntry.getOldParent().replaceFirst(Pattern.quote(parentPath), "");
					if ("".equals(oldParent)) {
						oldParent = "/";
					}
					buffer.append("Copied from ").append(oldParent).append(": ");
				}
			}
			else if ("move".equals(creationAction)) {
				if (fileEntry.getOldParent() == null) {
					buffer.append("Moved:");
				}
				else {
					String oldParent = fileEntry.getOldParent().replaceFirst(Pattern.quote(parentPath), "");
					if ("".equals(oldParent)) {
						oldParent = "/";
					}
					buffer.append("Moved from ").append(oldParent).append(": ");
				}
			}
			else if ("rename".equals(creationAction)) {
				if (fileEntry.getOldFileName() == null) {
					buffer.append("Renamed:");
				}
				else {
					buffer.append("Renamed from ").append(fileEntry.getOldFileName()).append(": ");
				}
			}
			else {
				buffer.append(creationAction).append(": ");
			}
			buffer.append(creationUserInfo).append(creationDate);
		}
		return buffer.toString();
	}

	/**
	 * Actually queries the DB for latest activity. 
	 */
	private void findLatestActivity(String regex, String sortByDesc, int topN, List<FileEntry> list, String userUid, Map<String, String> userMap) {
		DBCollection collection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject query = new BasicDBObject("uid", userUid).append("path", Pattern.compile(regex)).append(sortByDesc, new BasicDBObject("$ne", null));
		System.out.println("findLatestActivity, query: " + query.toString());
		System.out.println("\tsortByDesc: " + sortByDesc);
		try {
			try (DBCursor dbCursor = collection.find(query).sort(new BasicDBObject(sortByDesc, -1)).limit(topN)) {
				while (dbCursor.hasNext()) {
					FileEntry current = (FileEntry)dbCursor.next();
					boolean isDeleted = current.isDeleted();
					User user = null;
					String userName = null;
					String img = null;
					if (isDeleted) {
						String deletionUid = current.getDeletionUid(); 
						user = userManager.getUser(deletionUid);
						userName = user.getDisplayName();
						img = user.getImg();
						current.setDeletionUserName(userName);
						if (img != null) {
							current.setDeletionUserImg(img);
						}
					}
					// set creation info for all files. They were all created at some point
					String creationUid = current.getCreationUid();
					user = userManager.getUser(creationUid);
					userName = user.getDisplayName();
					img = user.getImg();
					current.setCreationUserName(userName);
					if (img != null) {
						current.setCreationUserImg(img);
					}

					list.add(current);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

} // end class

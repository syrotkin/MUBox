package server;

public class Constants {
	//public static final String USER_UID_SESSION_KEY = "USER_UID";
	static final String DISPLAY_NAME_SESSION_KEY = "DISPLAY_NAME";
	//static final String ACCESS_KEY_SESSION_KEY = "ACCESS_KEY";
	//static final String ACCESS_SECRET_SESSION_KEY = "ACCESS_SECRET";
	static final String WEB_AUTH_INFO_SESSION_KEY = "WEB_AUTH_INFO";
	static final String WEB_AUTH_SESSION_SESSION_KEY = "WEB_AUTH_SESSION";
	static final String GOOGLE_CREDENTIAL_SESSION_KEY = "GOOGLE_CREDENTIAL";
	public static final String USER_SESSION_KEY = "USER";
	public static final String PROVIDER_SESSION_KEY = "PROVIDER";
	public static final String DRIVE_SESSION_KEY = "DRIVE";
	
	public static final String DOWNLOADED_FILES = "downloadedfiles";
	
	
	public static class Provider {
		public static final String DROPBOX = "dropbox";
		public static final String GOOGLE = "google";
	}
		
	public static class DB {
		public static class Collection {
			public static final String DELTACURSORS = "deltacursors";
			public static final String FILEDATA = "filedata";
			public static final String FOLDERSEQ = "folderseq";
			public static final String SHAREDFOLDERS = "sharedfolders";
			public static final String USERS = "users";
		}
		
		public static class FileData {
			public static String UID = "uid";
			public static String PATH = "path";
			public static String FILENAME = "filename";
			public static String LOWERCASE_PATH = "lowercasePath";
			public static String IS_DIR = "isDir";
			public static String IS_DELETED = "isDeleted";
			public static String IS_SHARED = "isShared";
			public static String CREATION_DATE = "creationDate";
			public static String CREATION_UID = "creationUid";
			public static String CREATION_ACTION = "creationAction";
			public static String DELETION_DATE = "deletionDate";
			public static String DELETION_UID = "deletionUid";
			public static String DELETION_ACTION = "deletionAction";
			public static String NEW_PARENT = "newParent";
			public static String NEW_FILE_NAME = "newFileName";
			public static String OLD_PARENT = "oldParent";
			public static String OLD_FILE_NAME = "oldFileName";
			public static String REV  = "rev";
		}
		
	}
}

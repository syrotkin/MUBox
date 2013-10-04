package server.utils;

public class FilePath {

	public static String combine(String pathToFolder, String fileName) {
		if (fileName.length() == 0) {
			return pathToFolder;
		}
		if (pathToFolder.endsWith("/")) { // path/
			if (fileName.startsWith("/")) { //  /fileName
				return pathToFolder + fileName.substring(1);
			}
			else { // fileName
				return pathToFolder + fileName;
			}
		}
		else { // path
			if (fileName.startsWith("/")) { // /fileName
				return pathToFolder + fileName;
			}
			else { // fileName
				return pathToFolder + "/" + fileName;
			}
		}
	}

	
	public static String getParentPath(String path) {
		String parentPath = null;
		if (path.indexOf('/') == -1 || "/".equals(path)) {
			parentPath = "/";
		}
		else {
			int firstIndex = path.indexOf('/');
			int lastIndex = path.lastIndexOf('/');
			if (firstIndex == lastIndex) {
				parentPath = "/";
			}
			else {
				parentPath = path.substring(0, lastIndex);
			}
		}
		return parentPath;
	}
	
	/**
     * Returns the file name if this is a file (the part after the last
     * slash in the path).
     */
    public static String getFileName(String path) {
        int ind = path.lastIndexOf('/');
        return path.substring(ind + 1, path.length());
    }
	
	
	/**
	 * OLD: Given a path to a file, returns a path to its parent folder.
	 */
//	public static String getParentPath_old(String path) {
//		String parentPath = null;
//		if (path.indexOf('/') == -1) {
//			parentPath = "";
//		}
//		else {
//			int lastIndex = path.lastIndexOf('/');
//			parentPath = path.substring(0, lastIndex);
//		}
//		return parentPath;
//	}
//	
//	/**
//     * Used by dropbox. Returns the path of the parent directory if this is a file.
//     */
//    public static String parentPath(String path) {
//        if (path.equals("/")) {
//            return "";
//        } else {
//            int ind = path.lastIndexOf('/');
//            return path.substring(0, ind + 1);
//        }
//    }
    
	
	
    
	
}

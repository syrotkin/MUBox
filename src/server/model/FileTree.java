package server.model;
/**
 * Represents a tree of {@link FileNode}s. Used for resyncing with Google Drive.
 * @author soleksiy
 *
 */
public class FileTree {
	private FileNode root;
	public FileNode getRoot() {
		return root;
	}
	public void setRoot(FileNode root) {
		this.root = root;
	}
}
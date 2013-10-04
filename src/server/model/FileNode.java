package server.model;



import java.util.List;

import com.google.api.services.drive.model.File;

public class FileNode {
	private String fileId; // in case of root, fileId = "root", file = null;
	private File file;
	private List<FileNode> childNodes;
	public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public List<FileNode> getChildNodes() {
		return childNodes; // REMEMBER LEAKING!
	}
	public void setChildNodes(List<FileNode> childNodes) {
		this.childNodes = childNodes;
	}
}
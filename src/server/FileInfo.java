package server;

import java.io.InputStream;
/**
 * Represents information about a downloaded file.
 * @author soleksiy
 *
 */
public class FileInfo {
	private String name;
	private String mimeType;
	private InputStream inputStream;
	
	public FileInfo(String name, String mimeType) {
		this.name = name;
		this.mimeType = mimeType;
	}
	public String getName() {
		return name;
	}
	public String getMimeType() {
		return mimeType;
	}
	
	public InputStream getInputStream() {
		return inputStream;
	}
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
}
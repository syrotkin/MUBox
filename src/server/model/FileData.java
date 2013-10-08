package server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * FileData is some collection of files (perhaps all of them) pertaining to one user.
 * */
public class FileData {
	private List<FileEntry> entries;
	// user id
	private String uid;
	
	public FileData(String uid) {
		this.uid = uid;
		entries = new ArrayList<FileEntry>();
	}
	
	public String getUid() {
		return uid;
	}
	
	/**
	 * Gets a copy of the list of <code>FileEntry</code>s.
	 * @return List of <code>FileEntry</code>s.
	 */
	public List<FileEntry> getEntries() {
		if (entries == null) {
			return null;
		}
		int size = entries.size();
		List<FileEntry> result = new ArrayList<FileEntry>(size);
		for (int i = 0; i < size; i++) {
			result.add(entries.get(i));
		}
		return result;
	}
	/**
	 * Adds an entry to the list.
	 * @param entry Entry to add.
	 */
	public void addEntry(FileEntry entry) {
		entries.add(entry);
	}
	
}
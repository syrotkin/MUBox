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
	
	// NOTE: prevention of object leaking. Long live COOP.
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
	
	public void addEntry(FileEntry entry) {
		entries.add(entry);
	}

	public void sortEntries() {
		Collections.sort(entries, new Comparator<FileEntry>() {
			@Override
			public int compare(FileEntry o1, FileEntry o2) {
				return o1.getFilename().compareTo(o2.getFilename());
			}
		});
	}

	public void printEntries() {
		for (FileEntry fileEntry : getEntries()) {
			System.out.println(fileEntry.getPath() + "; " + (fileEntry.isDeleted() ? "deleted" : ""));
		}
		
	}
	
}
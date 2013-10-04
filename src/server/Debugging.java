package server;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.DefaultButtonModel;

import org.bson.types.ObjectId;
import org.eclipse.jetty.util.Loader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import server.data.DatabaseManager;
import server.data.FileManager;
import server.data.SharedFolderManager;
import server.data.UserManager;
import server.enums.MoveAction;
import server.model.FileData;
import server.model.FileEntry;
import server.model.SharedFolder;
import server.model.User;
import server.model.Voting;
import server.utils.FilePath;

import com.dropbox.client2.jsonextract.JsonThing;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

class Address {
	String description;
	public Address(String d) {
		this.description = d;
	}
}

class Person {
	private String name;
	public Person(String n) {
		this.name= n;
	}
	private Address address = new Address("Blah");

	public String getName() {
		return name;
	}
	public Address getAddress() {
		return address;
	}
}


public class Debugging<T> {	
	static DatabaseManager dbManager = DatabaseManager.getInstance();
	private static final String FILEDATA = "filedata";
	
	public static void main(String[] args) throws IOException {
		test11();
	}
	
	private static void test11() {
		//Debugging.Person p = new Debugging.Person("bob");
		Method m = null;
		try {
			m = Person.class.getMethod("getAddress");
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Address a = p.getAddress();
		
		
		Class<?> c = m.getDeclaringClass();
		if (c == null) {
			System.out.println("declaring class is null");
		}
		else {
			System.out.println(c.getName());
		}
	}
	
	private static void testFirstCharacter() {
		String[] inputs = {"Aasdf", "Zasdf", "somerehig", "", "HASD", "QWER", "qert", "1123" };	
		for (String input : inputs) {
			System.out.println(input + ": " + startsWithAorZ(input));
		}
	}
	
	private static boolean startsWithAorZ(String input) {
		if (input == null || input.length() == 0) {
			return false;
		}
		return input.charAt(0) <='Z' && input.charAt(0) >= 'A';
	}
	
	private static void testLongs() {
		long long1 = 50;
		int int1 = (int)long1;
		System.out.println(int1);
	}
	
	private static void testMap() {
		List<Voting> votings = new ArrayList<Voting>();
		Map<Long, Voting> votingMap = new HashMap<Long, Voting>();
		int i;
		for (i = 0; i < 3; i++) {
			Voting v = new Voting();
			v.setID(i);
			v.setInitiatorName("Alice");
			votings.add(v);
		}
		for (i = 0; i < votings.size(); i++) {
			votingMap.put(votings.get(i).getID(), votings.get(i));
		}
		
		i = 0;
		for (Long id : votingMap.keySet()) {
			Voting v = votingMap.get(id);
			v.setAction("rename " + id);
		}
		
		for (Voting voting : votings) {
			System.out.println(voting.getID() + ": " + voting.getAction());
		}
	}
	
	private static void testReverse() {
		String[] array = {"I", "am", "a", "student", "at", "a", "university", "in", "Switzerland"};
		for (String word : array) {
			System.out.println(reverse(word));
		}
	}
	
	private static String reverse(String input) {
		if (input == null || input.length() == 0) {
			return input;
		}
		char[] array = input.toCharArray();
		for (int i= 0; i<array.length/2; ++i ) {
			char temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
		return new String(array);
	}
	
	
	private static void testJSON() {
		JSONObject o = new JSONObject();
		String jsonString = "{ \"_id\" : { \"$oid\" : \"51af538b5f27d514d7e7509c\"} , creationAction : \"newfolder\" , creationDate : { $date : \"2013-06-05T15:04:43.251Z\"} , creationUid : \"179961868\" , filename : \"A\" , isDeleted : false , isDir : true , isShared : true , lowercasePath : \"/testsharedfiledata2/a\" , path : \"/TestSharedFileData2/A\" , rev : \"1111a55bc\" , uid : \"176162325\" , creationUserName : \"Dean User\"}";
		DBObject dbo = (DBObject)JSON.parse(jsonString);
		System.out.println(dbo.get("_id").getClass() + ", " + dbo.get("filename") + ", " + dbo.get("path"));
	}
		
	public static void JSONObject_vs_BasicDBObject() {
		Date now = new Date();
		
		BasicDBList users = new BasicDBList();
		users.add(new BasicDBObject("uid", 123123L));
		users.add(new BasicDBObject("uid", 456456L));
		BasicDBObject d = new BasicDBObject("users", users);
		d.append("date", now.getTime());
		System.out.println(d);
	
		JSONObject jo = new JSONObject();
		JSONArray ja = new JSONArray();
		JSONObject user1 = new JSONObject();
		user1.put("uid", 123123L);
		ja.add(user1);
		user1.put("uid", 456456L);
		ja.add(user1);
		jo.put("users", ja);
		jo.put("date", now.getTime());
		System.out.println(jo);
	}
	
	/*
	private static void testID() {
		UserManager um = new UserManager(dbManager);
		SharedFolderManager shfm = new SharedFolderManager(dbManager); 
		FileManager fm = new FileManager(dbManager, um, shfm);
		FileData result = fm.listChildrenForPath("176162325", "/TestSharedFileData2", false);
		for (FileEntry entry : result.getEntries()) {
			System.out.println(entry.getPath() + "; _id: " + entry.get("_id") + ", type: " + entry.get("_id").getClass());
			System.out.println(entry);
		}
	}
	*/
	
		
	private static void testListUsers() {
		String ownerUid = "x";//"172128704";
		DatabaseManager dbm = DatabaseManager.getInstance();
		
		//List<User> users = dbm.listUsersForSharedFolder(22, ownerUid);
		List<User> users = null;
		
		for (User user : users) {
			System.out.println(user.getUid() + ", " + user.getDisplayName());
		}
	}

	private static void testNullLong() {
		Object o = null;
		Long l = (Long)o;
		if (l == null) {
			System.out.println(-1);
		}
		else {
			System.out.println(l.longValue());
		}
	}


	private static void testJSONObject()  {
		JSONObject o = new JSONObject();
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "a");
		map.put("b", "b");
		o.put("map", map);
		System.out.println(o.toJSONString());
	}
	
	private static void createLowercasePath() {
		DatabaseManager dm = DatabaseManager.getInstance();
		DBCollection fileData = dm.getCollection("filedata");
		int count = 0;
		try (DBCursor dbCursor = fileData.find().limit(1000)) {
			while (dbCursor.hasNext()) {
				++count;
				DBObject next = dbCursor.next();
				String path = (String)next.get("path");
				next.put("lowercasePath", path.toLowerCase());
				fileData.save(next);
			}
		}
		System.out.println("processed " + count + " documents.");
	}
	
	// Switch with Strings works in Java 7!
	private static void testSwitch(String input) {
		switch (input) {
		case "girl":
			System.out.println("Miss");
			break;
		case "woman":
			System.out.println("Mrs.");
			break;
		case "man":
			System.out.println("Mr.");
			break;
		default:
			System.out.println("also Mr.");
		}
	}

	private static void testPaths() {
		String[] array = {"/sdf", "/path/to/foo", "/foo/file1.txt", "/file1.txt"};
		/*
		System.out.println("Using getParentPath_old:");
		for (String path : array) {
			String parentPath = FilePath.getParentPath_old(path);
			System.out.println("\t" + path+"\t\t" +  parentPath);
		}
		*/
		System.out.println("Using getParentPath:");
		for (String path : array) {
			String parentPath = FilePath.getParentPath(path);
			System.out.println("\t" + path+"\t\t" +  parentPath);
		}
		/*
		System.out.println("Using parentPath:");
		for (String path : array) {
			String parentPath = FilePath.parentPath(path);
			System.out.println("\t" + path+"\t\t" +  parentPath);
		}
		*/
		System.out.println("Using fileName:");
		for (String path : array) {
			String filename = FilePath.getFileName(path);
			System.out.println("\t" + path+"\t\t" +  filename);
		}
	}
	
	private static void testInsert() {
		DatabaseManager dbm = DatabaseManager.getInstance();
		List<SharedFolder> users = new ArrayList<SharedFolder>();
		//users.add(new BasicDBObject("uid", "123").append("path", "/path/to/foo"));
		//users.add(new BasicDBObject("uid", "456").append("path", "/foo"));
		//dbm.insertSharedFolder(users);
	}
	
	private static void testIncrement() {
		DatabaseManager dbm = DatabaseManager.getInstance();
		for (int i = 0; i < 5; i++) {
			//long number = dbm.getNextFolderSeq();
			//System.out.println(number);
		}
		
	}
	
		
	private static void testMakeNavList() {
		String a = "/";
		String b = "/blah";
		String c = "/blah/insideBlah";
		
		String[] testStrings = {a, b, c};
				
		Printer<Breadcrumb> printer = new Printer<Breadcrumb>();
		for (String input : testStrings) {
			Breadcrumb[] path = Breadcrumb.getBreadcrumbs(input);
			printer.printArray(path);
		}
	}
	
	
	public static class Printer<T> {
	
		public void printArray(T[] input) {
			if (input == null || input.length == 0) {
				System.out.println();
			}
			else {
				for (int i = 0; i < input.length; i++) {
					System.out.println(input[i]);				
				}
			}
		}
		
	}
	
	
	private static void test2() {
		// getting parent path from path:
		String fullPath = "/delete/" + "aaa/blah.txt";
		String path = fullPath.substring("/delete/".length());
		System.out.println(path);
		int index = path.indexOf('/');
		System.out.println(index);
		System.out.println(fullPath);
		
		int lastIndex = path.lastIndexOf('/');
		String test = path.substring(0, lastIndex);
		System.out.println(test);
		
	}
	
	/*
	private static void test1() {
		String currentDir = System.getProperty("user.dir");
		System.out.println(currentDir);
		Resource a = newClassPathResource("..\\bin\\insideBin", true, false);
		if (a == null) {
			System.out.println("resource is null");
		}
		else {
			System.out.println(a.getName()  + ", class: " + a.getClass().toString());
		}
	}
	
	 public static Resource newClassPathResource(String name,boolean useCaches,boolean checkParents)
     {
         URL url=Resource.class.getResource(name);
         
         if (url==null) {
        	 System.out.println("get here 0");
             url=Loader.getResource(Resource.class,name,checkParents);
         }
         if (url==null) {
             return null;
         }
         System.out.println("get here 1");
         System.out.println(url);
         return Resource.newResource(url,useCaches);
     }
	 */
	
	private static void someStringPathTest() {
		String path1 = "adsfa/blah/blaht.txt";
		String path2 = "/a/b/c/text";
		String path3 = "sdsdf/";
		String[] paths = {path1, path2, path3 };
		for (String path : paths) {
			String filename = path.substring(path.lastIndexOf('/') + 1);
			System.out.println(filename);
			System.out.println(filename.length());
		}
	}
	
	private static void read() throws IOException {
		String fileName = "C:\\dev\\test\\scalatra-hello-world\\sbt";
		Path path = Paths.get(fileName);
		List<String> list = Files.readAllLines(path, StandardCharsets.UTF_8);
		System.out.println("file length: " + list.size());
		for (int i=0; i< list.size(); ++i) {
			String newLine =list.get(i).replace("\r","");
			if (newLine.length() != list.get(i).length()) {
				System.out.println("new: " + newLine.length() + "; old: " + list.get(i).length());
			} 
			
			list.set(i, newLine); 
		}
		
		String outFile = "C:\\dev\\test\\scalatra-hello-world\\sbt_out";
		Path outPath = Paths.get(outFile);
		Files.write(outPath, list, StandardCharsets.UTF_8);
		System.out.println("file length: " + list.size());
	}
	
	private static void formatFile() {
		String fileName = "C:\\dev\\test\\scalatra-hello-world\\sbt";
		Scanner s = new Scanner(fileName);
		
		FileReader fileReader;
		try {
			fileReader = new FileReader(fileName);
			try {
				while(fileReader.read() != -1) {
							
				}
			}
			finally {
				fileReader.close();
			}
		} catch (IOException e) {
			//throw die(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static String formatParentPath(String parentPath) {
		//System.out.println("inside method");
		if ("/".equals(parentPath)) {
			return parentPath;
		}
		String newPath;
		if (parentPath.startsWith("/")) {
			newPath = parentPath.substring(1);
		}
		else {
			newPath = parentPath;
		}
		int index1 = newPath.indexOf('/');
		newPath = newPath.replace('/', '|');
		//System.out.println(newPath);
		newPath = newPath.substring(0, index1) + '/' + newPath.substring(index1+1);
		
		return newPath;
	}
	
	// TODO: this is a hack. Makes paths from /blah/blah1/blah2 to blah/blah1|blah2; from "/" to ""
		private  String formatParentPath2(String parentPath) {
			String newPath;
			if ("/".equals(parentPath)) {
				return "";
			}

			if (parentPath.startsWith("/")) {
				newPath = parentPath.substring(1);
			}
			else {
				newPath = parentPath;
			}
			newPath = newPath.replace('/', '|');
			return newPath;
		}

}

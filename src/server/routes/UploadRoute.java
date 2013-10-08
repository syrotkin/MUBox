package server.routes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;

import server.ChangeManager;
import server.CloudFactory;
import server.CloudStorage;
import server.Constants;
import server.data.FileManager;
import server.data.NoWarningJSONObject;
import server.model.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
/**
 * An implementation of a Spark route as a separate class. Moved to this separate class because the code for this route became long.
 * @author soleksiy
 *
 */
public class UploadRoute extends Route {

	private CloudFactory cloudFactory;
	private ChangeManager changeManager;
	private FileManager fileManager;
	
	public UploadRoute(String path, ChangeManager changeManager, FileManager fileManager, CloudFactory cloudFactory) {
		super(path);
		this.changeManager = changeManager;
		this.fileManager = fileManager;
		this.cloudFactory = cloudFactory;
	}
	
	//from: http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
	@Override
	public Object handle(Request request, Response response) {
		System.out.println("Entering SparkStart.upload");
		CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());		
		NoWarningJSONObject result = new NoWarningJSONObject();
		result.put("message", "Upload has not occurred");
		String parentPath = null;
		try {				
			// HACK -- because the servlet needs a MultiPartConfig annotation
			MultipartConfigElement element = new MultipartConfigElement(Constants.DOWNLOADED_FILES, 1024*1024*5, 1024*1024*5, 1024*1024*25);
			request.raw().setAttribute("org.eclipse.multipartConfig", element);

			final String path = Constants.DOWNLOADED_FILES;
			final Part filePart = request.raw().getPart("fileselect"); //used to be "fileselect"s
			final String fileName = getFileName(filePart);
			final String fullFilePath = path + File.separator + fileName;

			final Part parentPathPart = request.raw().getPart("parentpath");
			parentPath = getValueFromPart(parentPathPart);

			try (OutputStream out = new FileOutputStream(new File(fullFilePath)); 
					InputStream filecontent = filePart.getInputStream();) {
				int read = 0;
				final byte[] bytes = new byte[1024];
				while ((read = filecontent.read(bytes)) != -1) {
					out.write(bytes, 0, read); // writing to the file
				}
				System.out.println();
				result = cloudStorage.upload(fullFilePath, parentPath, fileName, fileManager);
				boolean success = (boolean)result.get("success");
				if (success) {
					User user = request.session().attribute(Constants.USER_SESSION_KEY);
					changeManager.recordUpload(parentPath, (String)result.get("filename"),  (String)result.get("rev"), (String)result.get("fileId"), user.getUid());
				}
			}
			catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		User user = getUserFromSession(request.session());
		JSONObject fileListResult = fileManager.listFiles(parentPath, false, user, cloudStorage);
		result.put("fileList", fileListResult.get("fileList"));
		System.out.println("returning " + result.toJSONString());
		return result.toJSONString();
	}

	private String getValueFromPart(final Part parentPathPart) {
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(parentPathPart.getInputStream(), writer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "/";
		}
		String result = writer.toString();
		System.out.println("parentPath undecoded: " + result);
		/*
		try {
			result = URLDecoder.decode(result, "UTF-8");
			System.out.println("parentPath decoded: " + result);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		return result;
	}

	// from http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
	private String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		for (String content : partHeader.split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}
	
	private User getUserFromSession(Session session) {
		if (session == null) {
			return null;
		}
		return session.attribute(Constants.USER_SESSION_KEY);
	}

}

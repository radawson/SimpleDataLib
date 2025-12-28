package regalowl.simpledatalib.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;











import regalowl.simpledatalib.SimpleDataLib;
import regalowl.simpledatalib.events.LogEvent;
import regalowl.simpledatalib.events.LogLevel;
import regalowl.simpledatalib.sql.QueryResult;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;




public class FileTools {
	
	private SimpleDataLib sdl;
	
	public FileTools(SimpleDataLib sdl) {
		this.sdl = sdl;
	}
	
	public ArrayList<String> getFolderContents(String folderpath) {
		ArrayList<String> libContents = new ArrayList<>();
		try {
			Path dir = Paths.get(folderpath);
			if (Files.exists(dir) && Files.isDirectory(dir)) {
				try (Stream<Path> paths = Files.list(dir)) {
					paths.forEach(path -> libContents.add(path.getFileName().toString()));
				}
			}
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to read folder contents: " + folderpath);
		}
		return libContents;
	}
	

	public String getJarPath() {
		URL url = sdl.getClass().getProtectionDomain().getCodeSource().getLocation();
		File f = null;
		try {
			f = new File(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		String path = "";
		if (!f.isDirectory()) {
			path = f.getParent();
		} else {
			path = f.getAbsolutePath();
		}
		return path;
	}

	public void copyZippedFileFromJar(String resource, String destinationFolderPath) {
		copyFileFromJar(resource, destinationFolderPath + File.separator + "temporaryZippedFile4564536.zip");
		unZipFile(destinationFolderPath + File.separator + "temporaryZippedFile4564536.zip", destinationFolderPath);
		deleteFile(destinationFolderPath + File.separator + "temporaryZippedFile4564536.zip");
	}
	
	public void copyFileFromJar(String resource, String destination) {
		InputStream resStreamIn = this.getClass().getClassLoader().getResourceAsStream(resource);
		if (resStreamIn == null) {
			sdl.getEventPublisher().fireEvent(new LogEvent("[SimpleDataLib["+sdl.getName()+"]]Failed to copy file. [" + resource + "]", null, LogLevel.SEVERE));
			return;
		}
		try {
			Path destPath = Paths.get(destination);
			Files.createDirectories(destPath.getParent());
			Files.copy(resStreamIn, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to copy file from jar: " + resource);
		} finally {
			try {
				resStreamIn.close();
			} catch (IOException e) {
				// Ignore close errors
			}
		}
	}

	public void makeFolder(String path) {
		try {
			Path folderPath = Paths.get(path);
			Files.createDirectories(folderPath);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to create folder: " + path);
		}
	}

	public void deleteFile(String path) {
		try {
			Path filePath = Paths.get(path);
			if (Files.exists(filePath)) {
				Files.delete(filePath);
			}
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to delete file: " + path);
		}
	}
	
	public void deleteDirectory(String path) {
		try {
			Path dirPath = Paths.get(path);
			if (Files.exists(dirPath)) {
				try (Stream<Path> paths = Files.walk(dirPath)) {
					paths.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
						.forEach(p -> {
							try {
								Files.delete(p);
							} catch (IOException e) {
								sdl.getErrorWriter().writeError(e, "Failed to delete: " + p);
							}
						});
				}
			}
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to delete directory: " + path);
		}
	}
	
	/**
	 * @deprecated Use deleteDirectory instead
	 */
	@Deprecated
	public void wipeDirectory(File dir) {
		deleteDirectory(dir.getPath());
	}
	public void unZipFile(String zipFile, String outputFolder) {
		try {
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(outputFolder + File.separator + fileName);
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				byte[] buffer = new byte[1024];
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				ze = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void copyFile(String sourcePath, String destPath) {
		try {
			Path source = Paths.get(sourcePath);
			Path destination = Paths.get(destPath);
			Files.createDirectories(destination.getParent());
			Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to copy file from " + sourcePath + " to " + destPath);
		}
	}

	public void moveFile(String sourcePath, String destPath) {
		try {
			Path source = Paths.get(sourcePath);
			Path destination = Paths.get(destPath);
			Files.createDirectories(destination.getParent());
			Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to move file from " + sourcePath + " to " + destPath);
		}
	}
	
	
	
	public boolean fileExists(String path) {
		return Files.exists(Paths.get(path));
	}
	
	public void makeFile(String path) {
		try {
			Path filePath = Paths.get(path);
			Files.createDirectories(filePath.getParent());
			if (!Files.exists(filePath)) {
				Files.createFile(filePath);
			}
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to create file: " + path);
		}
	}
	
	
	public void writeStringToFile(String text, String path) {
		try {
			Path filePath = Paths.get(path);
			Files.createDirectories(filePath.getParent());
			Files.writeString(filePath, text, StandardCharsets.UTF_8, 
				java.nio.file.StandardOpenOption.CREATE, 
				java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to write string to file: " + path);
		}
	}
	
	public String getStringFromFile(String path) {
		try {
			Path filePath = Paths.get(path);
			if (!Files.exists(filePath)) {
				return "error";
			}
			return Files.readString(filePath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to read string from file: " + path);
			return "error";
		}
	}
	
	public ArrayList<String> getStringArrayFromFile(String path) {
		ArrayList<String> text = new ArrayList<>();
		try {
			Path filePath = Paths.get(path);
			if (!Files.exists(filePath)) {
				text.add("error");
				return text;
			}
			text.addAll(Files.readAllLines(filePath, StandardCharsets.UTF_8));
			return text;
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to read string array from file: " + path);
			text.add("error");
			return text;
		}
	}


	
	public String getTimeStamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}
	
	public QueryResult readCSV(String filePath) {
		try {
			QueryResult qr = new QueryResult();
			if (!fileExists(filePath)) {
				return null;
			}
			Path path = Paths.get(filePath);
			try (CSVReader reader = new CSVReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
				List<String[]> rows = reader.readAll();
				boolean header = true;
				for (String[] row : rows) {
					if (header) {
						for (int i = 0; i < row.length; i++) {
							qr.addColumnName(row[i]);
						}
						header = false;
					} else {
						for (int i = 0; i < row.length; i++) {
							String cData = row[i];
							if (cData != null && cData.equals("{{NULL}}")) {
								cData = null;
							}
							qr.addData(i + 1, cData);
						}
					}
				}
			}
			return qr;
		} catch (IOException | CsvException e) {
			sdl.getErrorWriter().writeError(e, "Failed to read CSV file: " + filePath);
			return null;
		}
	}
	
	public void writeCSV(QueryResult data, String filePath) {
		try {
			if (fileExists(filePath)) {
				deleteFile(filePath);
			}
			Path path = Paths.get(filePath);
			Files.createDirectories(path.getParent());
			
			try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
				int colCount = data.getColumnCount();
				String[] columnNames = new String[colCount];
				ArrayList<String> namesArray = data.getColumnNames();
				for (int i = 0; i < colCount; i++) {
					columnNames[i] = namesArray.get(i);
				}
				writer.writeNext(columnNames);
				while (data.next()) {
					String[] row = new String[colCount];
					for (int i = 0; i < colCount; i++) {
						String cData = data.getString(i + 1);
						if (cData == null) {
							cData = "{{NULL}}";
						}
						row[i] = cData;
					}
					writer.writeNext(row);
				}
			}
		} catch (IOException e) {
			sdl.getErrorWriter().writeError(e, "Failed to write CSV file: " + filePath);
		}
	}
}

package com.tonbei.worldupdater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * A sample application that runs multiple requests against the Drive API. The requests this sample
 * makes are:
 * <ul>
 * <li>Does a resumable media upload</li>
 * <li>Updates the uploaded file by renaming it</li>
 * <li>Does a resumable media download</li>
 * <li>Does a direct media upload</li>
 * <li>Does a direct media download</li>
 * </ul>
 *
 * @author rmistry@google.com (Ravi Mistry)
 */
public class WorldUpdater {

	/**
	 * Be sure to specify the name of your application. If the application name is {@code null} or
	 * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "WorldUpdater";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd' 'HH':'mm':'ss");

	private static String folderId, jar, bat, map, fileDate;

	//TODO
	//private static final String UPLOAD_FILE_PATH = "C:\\Users\\とんかつ\\Desktop\\fgftgf.txt";
	//private static final String DIR_FOR_DOWNLOADS = "C:\\Users\\とんかつ\\Desktop\\test";
	//private static final java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);

	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR =
		new java.io.File(System.getProperty("user.home"), ".store/drive_sample");

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
	 * globally shared instance across your application.
	 */
	private static FileDataStoreFactory dataStoreFactory;

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global Drive API client. */
	private static Drive drive;

	/** Authorizes the installed application to access user's protected data. */
	private static Credential authorize() throws Exception {
		// load client secrets
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(WorldUpdater.class.getResourceAsStream("/client_secrets.json")));
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
			System.out.println(
					"Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
							+ "into drive-cmdline-sample/src/main/resources/client_secrets.json");
			System.exit(1);
		}
		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets,
				Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(dataStoreFactory)
				.build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public static void main(String[] args) {
		//TODO
//		Preconditions.checkArgument(
//			!UPLOAD_FILE_PATH.startsWith("Enter ") && !DIR_FOR_DOWNLOADS.startsWith("Enter "),
//			"Please enter the upload file path and download directory in %s", WorldUpdater.class);

		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			// authorization
			Credential credential = authorize();
			// set up the global Drive instance
			drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

		// run commands
			Path path = getApplicationPath(WorldUpdater.class);
			java.io.File config = new java.io.File(path.getParent().toFile(), "WorldUpdater.cfg");
			boolean firstsetup = false;

			if(!config.exists()) {
				firstsetup = true;
				View.header1("Initial setting");
				System.out.println("データを保存するフォルダのURLを入力してください。");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				folderId = br.readLine().substring(br.readLine().lastIndexOf("/"));
				System.out.println("Serverファイル(jarファイル)を選択してください。");
				jar = FileSelectDialog.OpenJarSelectDialog(path.getParent().toFile()).getAbsolutePath();
				System.out.println("Server起動ファイル(batファイル)を選択してください。");
				bat = FileSelectDialog.OpenBatSelectDialog(path.getParent().toFile()).getAbsolutePath();
				System.out.println("保存するMapフォルダを選択してください。");
				map = FileSelectDialog.OpenMapSelectDialog(path.getParent().toFile()).getAbsolutePath();

				fileDate = sdf.format(new Date());

				BufferedWriter writer = new BufferedWriter(new FileWriter(config));
				writer.write(folderId);
				writer.newLine();
				writer.write(jar);
				writer.newLine();
				writer.write(bat);
				writer.newLine();
				writer.write(map);
				writer.newLine();
				writer.write(fileDate);
				writer.close();
			}else {
				BufferedReader reader = new BufferedReader(new FileReader(config));
				List<String> data = new ArrayList<>();
				while (reader.readLine() != null) data.add(reader.readLine());
				folderId = data.get(0);
				jar = data.get(1);
				bat = data.get(2);
				map = data.get(3);
				fileDate = data.get(4);
				reader.close();
			}

			View.header1("Checking for the latest updates");//TODO

			List<File> zipfile = new ArrayList<>();
			String pageToken = null;
			do {
				FileList result = drive.files().list()
						.setQ("'" + folderId + "' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
						.setSpaces("drive")
						.setFields("nextPageToken, files(id, name)")
						.setPageToken(pageToken)
						.execute();
				for (File file : result.getFiles()) {
					System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
					if(file.getName().endsWith(".zip")) zipfile.add(file);
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);

			File latest = null;
			if(!zipfile.isEmpty()) {
				Date date = sdf.parse(zipfile.get(0).getName().replace(".zip", ""));
				for(File file : zipfile) {
					if(date.before(sdf.parse(file.getName().replace(".zip", "")))) {
						latest = file;
						date = sdf.parse(file.getName().replace(".zip", ""));
					}
				}
			}

			if(firstsetup) {
				if(latest != null) {
					System.out.println("Drive上にすでにデータがアップロードされています。");
					System.out.println("現在のマップデータを最新版としてアップロードしますか？");
					System.out.println("最新版をアップロード→y : アップロードせずに最新版をダウンロード→n");
					start : if(true) {
						BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
						if(br.readLine().equals("y")) {
							//TODO アップロード
						}else if(br.readLine().equals("n")) {
							//TODO ダウンロード
						}else {
							break start;
						}
					}
				}
			}else {
				if(latest != null) {
					Date fileDateObj = sdf.parse(fileDate);
					switch(fileDateObj.compareTo(sdf.parse(latest.getName().replace(".zip", "")))) {
					case 0://何もしない

						break;
					case 1://アップロード + driveからファイル削除
						//TODO
						break;
					case -1://ダウンロード
						//TODO
						break;
					}
				}
			}

			if(latest == null) {
				System.out.println("Drive上にデータがアップロードされていないため、現在のマップデータを最新版としてアップロードします。");
				//TODO アップロード
			}

			View.header1("Starting Server");
			System.out.println("サーバー起動中はこのウィンドウを閉じないでください。");


			View.header1("Starting Upload");
			//TODO アップロード + fileDate更新

/*
			View.header1("Starting Resumable Media Upload");
			File uploadedFile = uploadFile(false);

			View.header1("Updating Uploaded File Name");
			File updatedFile = updateFileWithTestSuffix(uploadedFile.getId());

			View.header1("Starting Resumable Media Download");
			downloadFile(false, updatedFile);

			View.header1("Starting Simple Media Upload");
			uploadedFile = uploadFile(true);

			View.header1("Starting Simple Media Download");
			downloadFile(true, uploadedFile);
*/
			View.header1("Success!");
			return;
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}

		System.exit(1);
	}

		/** Uploads a file using either resumable or direct media upload. */
	private static File uploadFile(boolean useDirectUpload, java.io.File uploadFile) throws IOException {
		File fileMetadata = new File();
		fileMetadata.setName(uploadFile.getName());

		FileContent mediaContent = new FileContent("application/zip", uploadFile);

		Create insert = drive.files().create(fileMetadata, mediaContent);
		MediaHttpUploader uploader = insert.getMediaHttpUploader();
		uploader.setDirectUploadEnabled(useDirectUpload);
		uploader.setProgressListener(new FileUploadProgressListener());
		return insert.execute();
	}

	/** Updates the name of the uploaded file to have a "drivetest-" prefix. */
	private static File updateFileWithTestSuffix(String fileId, String newFileName) throws IOException {
		File fileMetadata = new File();
		fileMetadata.setName(newFileName);

		Drive.Files.Update update = drive.files().update(fileId, fileMetadata);
		return update.execute();
	}

	/** Downloads a file using either resumable or direct media download. */
	private static java.io.File downloadFile(boolean useDirectDownload, File dlFile, String downloadDirPath) throws IOException {
		// create parent directory (if necessary)
		java.io.File parentDir = new java.io.File(downloadDirPath);
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Unable to create parent directory");
		}
		OutputStream out = new FileOutputStream(new java.io.File(parentDir, dlFile.getName()));

		MediaHttpDownloader downloader =
				new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
		downloader.setDirectDownloadEnabled(useDirectDownload);
		downloader.setProgressListener(new FileDownloadProgressListener());
		downloader.download(drive.files().get(dlFile.getId()).buildHttpRequestUrl(), out);

		return new java.io.File(parentDir, dlFile.getName());
	}

	public static Path getApplicationPath(Class<?> cls) throws URISyntaxException {
			ProtectionDomain pd = cls.getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			Path path = Paths.get(uri);
			return path;
	}

	public static void deleteDir(java.io.File TestFile) {
		if (TestFile.exists()) {
		//ファイル存在チェック
			if (TestFile.isFile()) {
				//存在したら削除する
				TestFile.delete();
				//対象がディレクトリの場合
			} else if(TestFile.isDirectory()) {
				//ディレクトリ内の一覧を取得
				java.io.File[] files = TestFile.listFiles();
				//存在するファイル数分ループして再帰的に削除
				for(int i=0; i<files.length; i++) deleteDir(files[i]);
				//ディレクトリを削除する
				TestFile.delete();
			}
		}
	}

	public static File upload() throws URISyntaxException, IOException {
		java.io.File parentDir = new java.io.File(getApplicationPath(WorldUpdater.class).getParent().toFile(), "temp");
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Unable to create parent directory");
		}

		java.io.File zip = new java.io.File(parentDir, sdf.format(new Date()) + ".zip");
		ZipCompressUtils.compressDirectory(zip.getAbsolutePath(), map);
		File upFile = uploadFile(false, zip);
		zip.delete();
		return upFile;
	}

	public static void download(File dlFile) throws URISyntaxException, IOException {
		java.io.File parentDir = new java.io.File(getApplicationPath(WorldUpdater.class).getParent().toFile(), "temp");
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Unable to create parent directory");
		}

		java.io.File dlf = downloadFile(false, dlFile, parentDir.getAbsolutePath());
		java.io.File mapDir = new java.io.File(map);
		deleteDir(mapDir);
		ZipUnCompressUtils.unzip(dlf.getAbsolutePath(), mapDir.getParent(), mapDir.getName());
		dlf.delete();
	}
}
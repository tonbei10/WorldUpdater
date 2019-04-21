package com.tonbei.worldupdater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.ParseException;
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
 * Based code : https://github.com/google/google-api-java-client-samples/blob/master/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/DriveSample.java
 */
public class WorldUpdater {

	private static final String APPLICATION_NAME = "WorldUpdater";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'-'HH'-'mm'-'ss");

	private static String folderId, jar, bat, map, fileDate;
	private static List<File> zipFiles = new ArrayList<>();

	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR =
		new java.io.File(System.getProperty("user.home"), ".store/" + APPLICATION_NAME);

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

		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets,
				Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(dataStoreFactory)
				.build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public static void main(String[] args) {

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

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("データを保存するフォルダのURLを入力してください。");
				String url = br.readLine();
				folderId = url.substring(url.lastIndexOf("/") + 1);
				System.out.println("Serverファイル(jarファイル)を選択してください。");
				jar = FileSelectDialog.OpenJarSelectDialog(path.getParent().toFile()).getAbsolutePath();
				System.out.println("Server起動ファイル(batファイル)を選択してください。");
				bat = FileSelectDialog.OpenBatSelectDialog(path.getParent().toFile()).getAbsolutePath();
				System.out.println("保存するMapフォルダを選択してください。");
				map = FileSelectDialog.OpenMapSelectDialog(path.getParent().toFile()).getAbsolutePath();

				fileDate = sdf.format(new Date());

				BufferedWriter writer = new BufferedWriter(new FileWriter(config));
				writer.write(folderId + ":/:" + jar + ":/:" + bat + ":/:" + map + ":/:" + fileDate);
				writer.close();
			}else {
				View.header1("Config loading");
				BufferedReader reader = new BufferedReader(new FileReader(config));
				String[] data = reader.readLine().split(":/:");
				folderId = data[0];
				jar = data[1];
				bat = data[2];
				map = data[3];
				fileDate = data[4];
				reader.close();
			}

			View.header1("Checking for the latest updates");

			String pageToken = null;
			do {
				FileList result = drive.files().list()
						.setQ("'" + folderId + "' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
						.setSpaces("drive")
						.setFields("nextPageToken, files(id, name)")
						.setPageToken(pageToken)
						.execute();
				for (File file : result.getFiles()) {
					if(file.getName().endsWith(".zip")) {
						System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
						zipFiles.add(file);
					}
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);

			File latest = null;
			if(!zipFiles.isEmpty()) {
				latest = zipFiles.get(0);
				Date date = sdf.parse(zipFiles.get(0).getName().replace(".zip", ""));
				for(File file : zipFiles) {
					if(date.before(sdf.parse(file.getName().replace(".zip", "")))) {
						latest = file;
						date = sdf.parse(file.getName().replace(".zip", ""));
					}
				}
			}

			if(latest != null) {
				if(firstsetup) {
					System.out.println("Drive上にすでにデータがアップロードされています。");
					System.out.println("現在のマップデータを最新版としてアップロードしますか？");
					System.out.println("最新版をアップロード→y : アップロードせずに最新版をダウンロード→n");
					while(true) {
						BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
						String ans = br.readLine();
						if(ans.equals("y")) { //アップロード
							upload();
							break;
						}else if(ans.equals("n")) { //ダウンロード
							download(latest);
							break;
						}
					}
				}else { //TODO 要チェック
					Date fileDateObj = sdf.parse(fileDate);
					int compare = fileDateObj.compareTo(sdf.parse(latest.getName().replace(".zip", "")));
					if(compare == 0) { //何もしない
						System.out.println("現在のマップデータは最新版です。");
					}else if(compare > 0) { //アップロード
						upload();
					}else if(compare < 0) { //ダウンロード
						download(latest);
					}
				}
			}else {
				System.out.println("Drive上にデータがアップロードされていないため、現在のマップデータを最新版としてアップロードします。");
				upload();
			}

			View.header1("Starting Server");
			System.out.println("サーバー起動中はこのウィンドウを閉じないでください。");

			ProcessBuilder pb = new ProcessBuilder(bat);
			pb.redirectErrorStream(true);  // 標準エラー出力の内容を標準出力にマージする

			Process process;
			try {
				process = pb.start();
			} catch (IOException e) {
				throw e;
			}

			int exitCode;
			try {
				// 標準出力をすべて読み込む
				new Thread(() -> {
					try (InputStream is = process.getInputStream()) {
						while (is.read() >= 0);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).start();

				process.waitFor();
				exitCode = process.exitValue();

			} catch (InterruptedException e) {
				throw e;
			} finally {
				if (process.isAlive()) {
					process.destroy(); // プロセスを強制終了
				}
			}

			if(exitCode != 0) {
				System.out.println("異常終了しました。");
				System.out.println("Driveへのアップロードをスキップします。");
				throw new RuntimeException("Abend detection.");
			}

			View.header1("Starting Upload");
			upload();

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
		fileMetadata.setParents(Collections.singletonList(folderId));

		FileContent mediaContent = new FileContent("application/zip", uploadFile);

		Create insert = drive.files().create(fileMetadata, mediaContent).setFields("id, parents, name");
		MediaHttpUploader uploader = insert.getMediaHttpUploader();
		uploader.setDirectUploadEnabled(useDirectUpload);
		uploader.setProgressListener(new FileUploadProgressListener());
		return insert.execute();
	}

	/** Updates the name of the uploaded file to have a "drivetest-" prefix. */
	@SuppressWarnings("unused")
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

	public static File upload() throws URISyntaxException, IOException, ParseException {
		java.io.File parentDir = new java.io.File(getApplicationPath(WorldUpdater.class).getParent().toFile(), "tempWorldUpdater");
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Unable to create parent directory");
		}

		fileDate = sdf.format(new Date());

		File oldest = null;
		if(!zipFiles.isEmpty()) {
			oldest = zipFiles.get(0);
			Date date = sdf.parse(zipFiles.get(0).getName().replace(".zip", ""));
			for(File file : zipFiles) {
				if(date.after(sdf.parse(file.getName().replace(".zip", "")))) {
					oldest = file;
					date = sdf.parse(file.getName().replace(".zip", ""));
				}
			}
		}

		if(oldest != null && zipFiles.size() >= 5) {
			drive.files().delete(oldest.getId()).execute();
			zipFiles.remove(oldest);
		}

		java.io.File zip = new java.io.File(parentDir, fileDate + ".zip");
		ZipCompressUtils.compressDirectory(zip.getAbsolutePath(), map);
		File upFile = uploadFile(false, zip);
		zipFiles.add(upFile);
		zip.delete();

		java.io.File config = new java.io.File(getApplicationPath(WorldUpdater.class).getParent().toFile(), "WorldUpdater.cfg");
		BufferedWriter writer = new BufferedWriter(new FileWriter(config));
		writer.write(folderId + ":/:" + jar + ":/:" + bat + ":/:" + map + ":/:" + fileDate);
		writer.close();

		deleteDir(parentDir);

		return upFile;
	}

	public static void download(File dlFile) throws URISyntaxException, IOException {
		java.io.File parentDir = new java.io.File(getApplicationPath(WorldUpdater.class).getParent().toFile(), "tempWorldUpdater");
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Unable to create parent directory");
		}

		java.io.File dlf = downloadFile(false, dlFile, parentDir.getAbsolutePath());
		java.io.File mapDir = new java.io.File(map);
		deleteDir(mapDir);
		ZipUnCompressUtils.unzip(dlf.getAbsolutePath(), mapDir.getParent(), mapDir.getName());
		dlf.delete();

		deleteDir(parentDir);
	}
}
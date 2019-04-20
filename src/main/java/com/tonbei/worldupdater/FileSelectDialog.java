package com.tonbei.worldupdater;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

@SuppressWarnings("restriction")
public class FileSelectDialog {

	private static File currentFile;

	@Nullable
	public static File OpenJarSelectDialog(File dir) {
		return OpenSelectDialog("jarファイルを選択",
				dir,
				new FileChooser.ExtensionFilter("jarファイル", "*.jar"));
	}

	@Nullable
	public static File OpenBatSelectDialog(File dir) {
		return OpenSelectDialog("batファイルを選択",
				dir,
				new FileChooser.ExtensionFilter("batファイル", "*.bat"));
	}

	@Nullable
	public static File OpenMapSelectDialog(File dir) {
		// Initializes JavaFX environment
		@SuppressWarnings("unused")
		JFXPanel jfxPanel = new JFXPanel();
		currentFile = null;

		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Mapフォルダを選択");
		dc.setInitialDirectory(dir);

		final CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				currentFile = dc.showDialog(null);
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return null;
		}

		return currentFile;
	}

	@Nullable
	public static File OpenSelectDialog(String title, File dir, FileChooser.ExtensionFilter... filters) {
		// Initializes JavaFX environment
		@SuppressWarnings("unused")
		JFXPanel jfxPanel = new JFXPanel();
		currentFile = null;

		FileChooser fc = new FileChooser();
		fc.setTitle(title);
		fc.getExtensionFilters().addAll(filters);
		fc.setInitialDirectory(dir);

		final CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				currentFile = fc.showOpenDialog(null);
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return null;
		}

		return currentFile;
	}
}
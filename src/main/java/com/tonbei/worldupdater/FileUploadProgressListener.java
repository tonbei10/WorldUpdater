package com.tonbei.worldupdater;

import java.io.IOException;
import java.text.NumberFormat;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

/**
 * Based code : https://github.com/google/google-api-java-client-samples/blob/master/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/FileUploadProgressListener.java
 */
public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {

	@SuppressWarnings("incomplete-switch")
	@Override
	public void progressChanged(MediaHttpUploader uploader) throws IOException {
		switch (uploader.getUploadState()) {
		case INITIATION_STARTED:
			View.header2("Upload Initiation has started.");
			break;
		case INITIATION_COMPLETE:
			View.header2("Upload Initiation is Complete.");
			break;
		case MEDIA_IN_PROGRESS:
			View.header2("Upload is In Progress: "
					+ NumberFormat.getPercentInstance().format(uploader.getProgress()));
			break;
		case MEDIA_COMPLETE:
			View.header2("Upload is Complete!");
			break;
		}
	}
}
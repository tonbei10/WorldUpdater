package com.tonbei.worldupdater;

/**
 * Based code : https://github.com/google/google-api-java-client-samples/blob/master/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/View.java
 */
public class View {

	static void header1(String name) {
		System.out.println();
		System.out.println("================== " + name + " ==================");
		System.out.println();
	}

	static void header2(String name) {
		System.out.println();
		System.out.println("~~~~~~~~~~~~~~~~~~ " + name + " ~~~~~~~~~~~~~~~~~~");
		System.out.println();
	}
}
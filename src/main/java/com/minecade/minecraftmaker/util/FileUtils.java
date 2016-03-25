package com.minecade.minecraftmaker.util;

import java.io.File;
import java.io.IOException;

import com.minecade.minecraftmaker.schematic.exception.FilenameException;
import com.minecade.minecraftmaker.schematic.exception.FilenameResolutionException;
import com.minecade.minecraftmaker.schematic.exception.InvalidFilenameException;

public class FileUtils {

	/**
	 * Gets the path to a file. This method will check to see if the filename
	 * has valid characters and has an extension. It also prevents directory
	 * traversal exploits by checking the root directory and the file directory.
	 * On success, a {@code java.io.File} object will be returned.
	 *
	 * @param dir
	 *            sub-directory to look in
	 * @param filename
	 *            filename (user-submitted)
	 * @param defaultExt
	 *            append an extension if missing one, null to not use
	 * @param extensions
	 *            list of extensions, null for any
	 * @return a file
	 * @throws FilenameException
	 *             thrown if the filename is invalid
	 */
	public static File getSafeSaveFile(File dir, String filename, String defaultExt, String... extensions) throws FilenameException {
		return getSafeFile(dir, filename, defaultExt, extensions, true);
	}

	/**
	 * Gets the path to a file. This method will check to see if the filename
	 * has valid characters and has an extension. It also prevents directory
	 * traversal exploits by checking the root directory and the file directory.
	 * On success, a {@code java.io.File} object will be returned.
	 *
	 * @param dir
	 *            sub-directory to look in
	 * @param filename
	 *            filename (user-submitted)
	 * @param defaultExt
	 *            append an extension if missing one, null to not use
	 * @param extensions
	 *            list of extensions, null for any
	 * @return a file
	 * @throws FilenameException
	 *             thrown if the filename is invalid
	 */
	public static File getSafeOpenFile(File dir, String filename, String defaultExt, String... extensions) throws FilenameException {
		return getSafeFile(dir, filename, defaultExt, extensions, false);
	}

	/**
	 * Get a safe path to a file.
	 *
	 * @param dir
	 *            sub-directory to look in
	 * @param filename
	 *            filename (user-submitted)
	 * @param defaultExt
	 *            append an extension if missing one, null to not use
	 * @param extensions
	 *            list of extensions, null for any
	 * @param isSave
	 *            true if the purpose is for saving
	 * @return a file
	 * @throws FilenameException
	 *             thrown if the filename is invalid
	 */
	private static File getSafeFile(File dir, String filename, String defaultExt, String[] extensions, boolean isSave) throws FilenameException {
		if (extensions != null && (extensions.length == 1 && extensions[0] == null))
			extensions = null;

		File f;

		if (defaultExt != null && filename.lastIndexOf('.') == -1) {
			filename += "." + defaultExt;
		}

		if (!filename.matches("^[A-Za-z0-9_\\- \\./\\\\'\\$@~!%\\^\\*\\(\\)\\[\\]\\+\\{\\},\\?]+\\.[A-Za-z0-9]+$")) {
			throw new InvalidFilenameException(filename, "Invalid characters or extension missing");
		}

		f = new File(dir, filename);

		try {
			String filePath = f.getCanonicalPath();
			String dirPath = dir.getCanonicalPath();

			if (!filePath.substring(0, dirPath.length()).equals(dirPath)) {
				throw new FilenameResolutionException(filename, "Path is outside allowable root");
			}

			return f;
		} catch (IOException e) {
			throw new FilenameResolutionException(filename, "Failed to resolve path");
		}
	}

	private FileUtils() {
		super();
	}

}

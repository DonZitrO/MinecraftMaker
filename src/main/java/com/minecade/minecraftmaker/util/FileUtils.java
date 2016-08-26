package com.minecade.minecraftmaker.util;

import java.io.File;
import java.io.IOException;

import com.minecade.mcore.schematic.exception.FilenameException;
import com.minecade.mcore.schematic.exception.FilenameResolutionException;
import com.minecade.mcore.schematic.exception.InvalidFilenameException;

public class FileUtils {

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
	 * @return a file
	 * @throws FilenameException
	 *             thrown if the filename is invalid
	 */
	public static File getSafeFile(File dir, String filename, String defaultExt, String... extensions) throws FilenameException {
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

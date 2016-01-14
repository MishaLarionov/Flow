package util;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import database.SQLDatabase;

public class DataModification {

	/**
	 * Returns the path to a directory, including the project folder.
	 * 
	 * @param directoryId
	 *            the UUID, in String form, of the directory which to generate
	 *            the path of.
	 * @return the path to the specified directory.
	 * @throws DatabaseException
	 *             if the directory doesn't exist or there is an error accessing
	 *             the database.
	 */
	public static String getDirectoryPath(String directoryId)
			throws DatabaseException {
		String parentDirectoryId = directoryId;

		// TODO optimize for efficiency
		StringBuilder path = new StringBuilder();
		do {
			directoryId = parentDirectoryId;
			if (path.length() > 0)
				path.insert(0, File.separator);
			path.insert(0, directoryId);
			try {
				parentDirectoryId = Results.toStringArray("ParentDirectoryID",
						SQLDatabase.getInstance()
								.getDirectoryInfo(directoryId))[0];
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new DatabaseException(e.getMessage());
			}
		} while (!parentDirectoryId.equals(directoryId));

		return path.toString();
	}

	/**
	 * Returns the path to a file, including the project folder.
	 * 
	 * @param fileId
	 *            the UUID, in String form, of the file which to generate the
	 *            path of.
	 * @return the path to the specified file.
	 * @throws DatabaseException
	 *             if the file doesn't exist or there is an error accessing the
	 *             database.
	 */
	public static String getFilePath(String fileId) throws DatabaseException {
		ResultSet fileData = SQLDatabase.getInstance().getFileInfo(fileId);
		String parentDirectoryId = null, directoryId = null;
		try {
			parentDirectoryId = fileData.getString("ParentDirectoryID");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO optimize for efficiency
		StringBuilder path = new StringBuilder();
		do {
			directoryId = parentDirectoryId;
			if (path.length() > 0)
				path.insert(0, File.separator);
			path.insert(0, directoryId);
			try {
				parentDirectoryId = Results.toStringArray("ParentDirectoryID",
						SQLDatabase.getInstance()
								.getDirectoryInfo(directoryId))[0];
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new DatabaseException(e.getMessage());
			}
		} while (!parentDirectoryId.equals(directoryId));

		return path.toString();
	}

	// TODO Decide if needed, if so, FIXME
	public static void fileVisualizer(String directoryId)
			throws DatabaseException {
		String parentDirectoryId = directoryId;
		do {
			directoryId = parentDirectoryId;
			System.out.println(directoryId + ": ");
			// String[] data = SQLDatabase.getInstance().
			try {
				parentDirectoryId = Results.toStringArray("ParentDirectoryID",
						SQLDatabase.getInstance()
								.getDirectoryInfo(directoryId))[0];

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new DatabaseException(e.getMessage());
			}
		} while (!directoryId.equals(parentDirectoryId));
	}

	/**
	 * Converts the given Strings containing the string representations of UUIDs
	 * to an array of UUID objects.
	 * 
	 * @param uuids
	 *            Strings consisting of string representations of UUIDs.
	 * @return
	 * @throws IllegalArgumentException
	 *             If an on the Strings in the array do not conform to the
	 *             string representation as described in {@link UUID#toString()}
	 */
	public static UUID[] getUUIDsFromArray(String... uuids) {
		UUID[] array = new UUID[uuids.length];
		for (int i = 0; i < uuids.length; i++) {
			array[i] = UUID.fromString(uuids[i]);
		}
		return array;
	}
}

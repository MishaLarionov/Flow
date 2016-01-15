package database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;

import server.FlowServer;
import util.DatabaseException;
import util.Results;

public class SQLDatabase {

	/**
	 * Location where the SQLite JDBC drivers are stored
	 */
	public static final String DRIVER = "org.sqlite.JDBC";

	// TODO Properly implement the timeout
	/**
	 * Number of seconds to allow for searching before timeout
	 */
	public static final int TIMEOUT = 5;

	public static final int NONE = 0, VIEW = 1, EDIT = 2, OWNER = 3;

	public static final String ARBITRARY_DOCUMENT = "ARBITRARY_DOCUMENT",
			TEXT_DOCUMENT = "TEXT_DOCUMENT";

	/**
	 * Connection to the database.
	 */
	private Connection connection;

	/**
	 * Latest instance of the SQLDatabase
	 */
	public static SQLDatabase instance;

	public SQLDatabase(String databaseName) {
		try {
			DriverManager.registerDriver(
					(Driver) Class.forName(DRIVER).newInstance());
		} catch (Exception e) {
			System.out
					.println("Error loading database driver: " + e.toString());
			return;
		}

		try {
			this.connection = DriverManager
					.getConnection("jdbc:sqlite:" + databaseName);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(
					"Error connecting to database located at: " + databaseName);
		}
		instance = this;
	}

	/**
	 * Returns the latest instance of the SQLDatabase.
	 *
	 * @return the latest instance of the SQLDatabase or NULL if not yet
	 *         initialized.
	 */
	public static SQLDatabase getInstance() {
		return instance;
	}

	/**
	 * Getter for all projects associated with the specified username,
	 * completely ignoring whether the user is the owner, or has only edit or
	 * view access.
	 *
	 * @param username
	 *            the ID of the user.
	 * @return all projects associated with the specified username.
	 * @throws DatabaseException
	 *             if there is an error accessing the database.
	 */
	public ResultSet getProjects(String username) throws DatabaseException {
		try {
			// Note: deleted the checking code, since a user can have no
			// projects, which would throw an error, checking code moved to
			// external method
			return this.query(String.format(
					"SELECT * FROM access WHERE Username = '%s';", username));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Specifies the access level to a specific project for a user.
	 *
	 * @param accessLevel
	 *            the level of access provided to the user, either {@link OWNER}
	 *            , {@link EDIT}, or {@link VIEW}.
	 * @param projectId
	 *            the project which to provide the user access to.
	 * @param username
	 *            the username which to provide access to.
	 * @return whether or not the access was successfully granted.
	 */
	public String updateAccess(int accessLevel, String projectId,
			String username) {
		// TODO Implement check for if username exists
		try {
			if (accessLevel == EDIT || accessLevel == VIEW) {
				this.update(String.format(
						"INSERT INTO access values('%s', '%s', '%s');",
						projectId, username, accessLevel));
			} else if (accessLevel == OWNER) {
				// Changes the owner of the project in the projects table
				this.update(String.format(
						"UPDATE projects SET OwnerUsername = '%s' WHERE ProjectID = '%s';",
						username, projectId));

				this.update(String.format(
						"DELETE FROM access WHERE Username = '%s' AND ProjectID = '%s';",
						username, projectId));

				// Changes the permissions of the user to be an owner
				this.update("INSERT INTO access values('" + projectId + "', '"
						+ username + "', '" + OWNER + "');");
			} else if (accessLevel == NONE) {
				this.update(String.format(
						"DELETE FROM access WHERE Username = '%s' AND ProjectID = '%s';",
						username, projectId));
			} else {
				return "ACCESS_LEVEL_INVALID";
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Getter for all files associated with the specified project.
	 *
	 * @param projectId
	 *            the ID of the project.
	 * @return all associated files.
	 */
	public ResultSet getFilesInProject(String projectId)
			throws DatabaseException {
		synchronized (this) {
			try {
				// TODO Add check if for project is exists
				return this.query(String.format(
						"SELECT * FROM documents WHERE ProjectID = '%s';",
						projectId));
			} catch (SQLException e) {
				e.printStackTrace();
				// TODO: this won't be called for the above reason, move this
				// exception to the check (Above)
				throw new DatabaseException("PROJECT_DOES_NOT_EXIST");
			}
		}
		// return null;
	}

	/**
	 * Getter for all files inside the specified directory.
	 *
	 * @param directoryUUID
	 *            the UUID of the directory in String form.
	 * @return all associated files.
	 */
	public ResultSet getFilesInDirectory(String directoryUUID)
			throws DatabaseException {
		try {
			// TODO Add check if for project is exists
			return this.query(String.format(
					"SELECT * FROM documents WHERE ParentDirectoryID = '%s';",
					directoryUUID));
		} catch (SQLException e) {
			e.printStackTrace();
			// TODO: this won't be called for the above reason, move this
			// exception to the check (Above)
			throw new DatabaseException("PROJECT_DOES_NOT_EXIST");
		}
		// return null;
	}

	/**
	 * Getter for all directories inside the specified directory.
	 *
	 * @param directoryUUID
	 *            the UUID of the directory in String form.
	 * @return all associated files.
	 */
	public ResultSet getDirectoriesInDirectory(String directoryUUID)
			throws DatabaseException {
		try {
			// TODO Add check if for project is exists
			return this.query(String.format(
					"SELECT * FROM directories WHERE ParentDirectoryID = '%s';",
					directoryUUID));
		} catch (SQLException e) {
			e.printStackTrace();
			// TODO: this won't be called for the above reason, move this
			// exception to the check (Above)
			throw new DatabaseException("PROJECT_DOES_NOT_EXIST");
		}
		// return null;
	}

	/**
	 * Creates a new project with the specified name and owner
	 *
	 * @param projectName
	 *            name of the project
	 * @param ownerId
	 *            ID of the user who creates the project
	 */
	public String newProject(String projectId, String projectName,
			String ownerId) {
		try {
			// TODO Add check to make sure user doesn't have two projects with
			// same name
			this.update(String.format(
					"INSERT INTO projects VALUES('%s', '%s', '%s');", projectId,
					projectName, ownerId));
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return this.newDirectory(projectName, projectId, projectId, projectId);
	}

	/**
	 * Creates a new file within the specified project.
	 *
	 * @param fileUUID
	 *            the ID of the file ({@link UUID#toString() string
	 *            representation} of the UUID associated with file).
	 * @param fileName
	 *            the name of the file (including the extension).
	 * @param projectUUID
	 *            the ID of the project which to place the file inside
	 * @param directoryUUID
	 *            the ID of the directory which to place the file inside
	 */
	public String newFile(String fileUUID, String fileName, String projectUUID,
			String directoryUUID, String fileType) {
		try {
			if (fileType.equals(ARBITRARY_DOCUMENT)
					|| fileType.equals(TEXT_DOCUMENT)) {
				this.update(String.format(
						"INSERT INTO documents VALUES('%s', '%s', '%s', '%s', '%s');",
						fileUUID, projectUUID, fileName, directoryUUID,
						fileType));
			} else {
				// This cannot be the client's error as the type is determined
				// by the request type (new_arbitrarydocument and
				// new_textdocument), rather than a passed value.
				return FlowServer.ERROR;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Creates a new directory within the specified project.
	 *
	 * @param directoryName
	 *            the name of the directory.
	 * @param directoryId
	 *            the ID of the directory ({@link UUID#toString() string
	 *            representation} of the UUID associated with the directory).
	 * @param projectId
	 *            the ID of the project which to place the directory inside.
	 * @param parentDirectoryId
	 *            the ID of the directory which to place the directory inside.
	 */
	public String newDirectory(String directoryName, String directoryId,
			String projectId, String parentDirectoryId) {
		try {
			// TODO Add checks
			// TODO Change this to not require parentDirectoryId
			this.update(String
					.format("INSERT INTO directories VALUES('%s', '%s', '%s', '%s');",
							directoryId,
							parentDirectoryId.equals(directoryId) ? "null"
									: parentDirectoryId,
							directoryName, projectId));
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Getter for all of the usernames in the database.
	 *
	 * @return all of the usernames in the database.
	 * @throws DatabaseException
	 */
	public ResultSet getUserNames(String projectId) throws DatabaseException {
		try {
			if (!this.query(String.format(
					"SELECT * from projects WHERE ProjectID = '%s';",
					projectId)).next()) {
				throw new DatabaseException("PROJECT_DOES_NOT_EXIST");
			}
			return this.query(String.format(
					"SELECT Username FROM access WHERE ProjectID = '%s';",
					projectId));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		throw new DatabaseException(FlowServer.ERROR);
	}

	/**
	 * Authenticates a user by verifying if the specified username and password
	 * pair exists in the database.
	 *
	 * @param username
	 *            the user's username.
	 * @param password
	 *            the user's encrypted password.
	 * @return whether or not the username and password exists in the database.
	 */
	public boolean authenticate(String username, String password) {
		try {
			ResultSet pair = this.query(String.format(
					"SELECT Password FROM users WHERE Username = '%s';",
					username));
			if (pair.next()) {
				// TODO Verify
				return password.equals(pair.getString("password"));
			}
		} catch (SQLException e) {
			// TODO Remove this debugging message
			System.err.println("Error authenticating user: " + username
					+ " with password: " + password);
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates a new session for the specified username and serial number.
	 *
	 * @param username
	 *            the UUID of the user.
	 * @param serialNumber
	 *            the serial number of the user's hard drive.
	 * @param sessionId
	 *            the session ID which to associated with the user.
	 * @return whether or not the session was successfully associated with the
	 *         user and serial number.
	 */
	public boolean newSession(String username, String sessionId) {
		try {
			this.update(
					String.format("INSERT INTO sessions VALUES ('%s', '%s');",
							username, sessionId));
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Removes the specified session from the database
	 *
	 * @param sessionId
	 *            the string representation of the UUID of the session
	 * @return whether or not the session was successfully removed
	 */
	public boolean removeSession(String sessionId) {
		try {
			// TODO Verify if the session actually exists prior to removal
			this.update(String.format(
					"DELETE FROM sessions WHERE SessionID = '%s';", sessionId));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Getter for the username and serial number associated with the specified
	 * session ID.
	 *
	 * @param sessionId
	 *            the id associated with the desired session
	 * @return the username and serial number associated with the specified
	 *         session ID
	 * @throws DatabaseException
	 *             if the sessionId is invalid ("INVALID_SESSION_ID") or there
	 *             is an error accessing the database ({@link FlowServer#ERROR}
	 *             ).
	 */
	public ResultSet getSessionInfo(String sessionId) throws DatabaseException {
		try {
			ResultSet temp = this.query(String.format(
					"SELECT * FROM sessions WHERE SessionID = '%s';",
					sessionId));
			if (temp.next()) {
				// Duplicate query because SQLite doesn't support moving back in
				// data (i.e. creating non 'TYPE_FORWARD_ONLY' ResultSets)
				return this.query(String.format(
						"SELECT * FROM sessions WHERE SessionID = '%s';",
						sessionId));
			}
			throw new DatabaseException("INVALID_SESSION_ID");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new DatabaseException(FlowServer.ERROR);
	}

	/**
	 * Add users to the database with the specified username and password.
	 *
	 * @param username
	 *            the desired username
	 * @param password
	 *            the <b>encrypted</b> password <br>
	 *            Please don't enter passwords in plain text.
	 * @return whether or not the insertion into the database was successful. It
	 *         could have been unsuccesful (returned false) because:<br>
	 *         <ul>
	 *         <li>The selected username already exists in the database.</li>
	 *         <li>An error was thrown when searching the database for all
	 *         current users.</li>
	 *         <li>An error was thrown when inserting user into the database.
	 *         </li>
	 *         </ul>
	 */
	public String addUser(String username, String password) {
		try {
			// Checks if a user with the specified username already exsists
			if (this.query(String.format(
					"SELECT username FROM users WHERE Username = '%s';",
					username)).next()) {
				return "USERNAME_TAKEN";
			}
		} catch (SQLException e) {
			System.err.println("Error querying database from all users");
			e.printStackTrace();
			return FlowServer.ERROR;
		}

		try {
			this.update(String.format("INSERT INTO users VALUES ('%s', '%s');",
					username, password));
		} catch (SQLException e) {
			System.err.println("Error inserting user into database");
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Retrieves all associated data with the specified file.
	 *
	 * @param fileId
	 *            the UUID of the file to retrieve.
	 * @return all associated data from the 'documents' SQL table.
	 * @throws DatabaseException
	 *             if the file doesn't exists in the database.
	 */
	public ResultSet getFileInfo(String fileId) throws DatabaseException {
		try {
			ResultSet temp = this.query(String.format(
					"SELECT * from documents WHERE DocumentID = '%s';",
					fileId));
			if (temp.next()) {
				return temp;
			} else {
				// Throw an exception in this case because the server expects to
				// use the found file, this prevents a '!=null' check
				throw new DatabaseException("INVALID_FILE_UUID");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch blocks
			e.printStackTrace();
		}
		// Throw an exception in this case because the server expects to use
		// the found file, this prevents a '!=null' check
		throw new DatabaseException(FlowServer.ERROR);
	}

	/**
	 * Renames the specified project. This only changes the <b>given</b> project
	 * name, <b>not the UUID</b>, hence not the internal server directory
	 * structure either.
	 *
	 * @param projectId
	 *            the UUID of the project to rename.
	 * @param newName
	 *            the name which to assign to the project.
	 * @throws DatabaseException
	 *             if the specified project UUID doesn't exists in the database
	 *             or the new name contains invalid characters.
	 */
	public String renameProject(String projectId, String newName) {
		// TODO Check if name is valid
		try {
			if (!this.query(String.format(
					"SELECT * from projects WHERE ProjectID = '%s';",
					projectId)).next()) {
				return "INVALID_PROJECT_UUID";
			}
			this.update(String.format(
					"UPDATE projects SET ProjectName = '%s' WHERE ProjectID = '%s';",
					newName, projectId));
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	// TODO COMMENT
	public String renameDirectory(String directoryUUID, String newName) {
		try {
			if (!this.query(String.format(
					"SELECT * from directories WHERE DirectoryID = '%s';",
					directoryUUID)).next()) {
				return "INVALID_DIRECTORY_UUID";
			}
			this.update(String.format(
					"UPDATE projects SET DirectoryName = '%s' WHERE DirectoryID = '%s';",
					newName, directoryUUID));
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Deletes the specified project from the database. This will
	 * <b>permanently</b> delete the project and access to all contained files
	 * for <b>all users</b>. <br>
	 * <br>
	 * This method should only be called after verifying that the current
	 * session belongs to the user which is the <b>owner</b> of the specified
	 * project.
	 *
	 * @param projectId
	 *            the UUID of the project to delete.
	 * @throws DatabaseException
	 *             if the project doesn't exist.
	 */
	public String deleteProject(String projectId) {
		try {
			if (!this.query(String.format(
					"SELECT * from projects WHERE ProjectID = '%s';",
					projectId)).next()) {
				return "INVALID_PROJECT_UUID";
			}
			this.update(String.format(
					"DELETE FROM projects WHERE ProjectID = '%s';", projectId));
			this.update(String.format(
					"DELETE FROM access WHERE ProjectID = '%s';", projectId));
			this.update(String.format(
					"DELETE FROM documents WHERE ProjectID = '%s';",
					projectId));
			this.update(String.format(
					"DELETE FROM directories WHERE ProjectID = '%s';",
					projectId));
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	// TODO COMMENT
	public String deleteDirectory(String directoryUUID) {
		try {
			if (!this.query(String.format(
					"SELECT * from Directories WHERE DirectoryID = '%s';",
					directoryUUID)).next()) {
				return "INVALID_DIRECTORY_UUID";
			}
			this.update(String.format(
					"DELETE FROM documents WHERE ParentDirectoryID = '%s';",
					directoryUUID));
			// TOOD Cycle through all child directories and delete files
		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Allows a user to remove their account and <b>all projects which they are
	 * the owner</b> from the database. This means these projects will <b>no
	 * longer be accessible</b>. Please change the owner of any projects that
	 * other users wish to continue developing.
	 *
	 * @param username
	 *            the username associated with the account to close.
	 * @throws DatabaseException
	 *             if the username does not exist in the database.
	 */
	public String closeAccount(String username) {
		try {
			if (!this.query(String.format(
					"SELECT * from users WHERE Username = '%s';", username))
					.next()) {
				return "USERNAME_DOES_NOT_EXIST";
			}

			// TODO Delete all documents using the deleted project IDs
			this.update(String.format(
					"DELETE FROM users WHERE Username = '%s';", username));
			this.update(String.format(
					"DELETE FROM projects WHERE OwnerUsername = '%s';",
					username));
			this.update(String.format(
					"DELETE FROM sessions WHERE Username = '%s';", username));
			this.update(String.format(
					"DELETE FROM access WHERE Username = '%s';", username));

			// TODO Delete all the actual file data from disk

		} catch (SQLException e) {
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Allows users to change their password.
	 *
	 * @param username
	 *            the username of the user (unique)
	 * @param newPassword
	 *            the new password which the user wants to associate with their
	 *            username
	 * @throws DatabaseException
	 *             if the username does not exist in the system, or the entered
	 *             password is invalid
	 */
	public String changePassword(String username, String newPassword) {
		try {
			if (!this.query(String.format(
					"SELECT * from users WHERE Username = '%s';", username))
					.next()) {
				return "USERNAME_DOES_NOT_EXIST";
			}
			this.update(String.format(
					"UPDATE users SET Password = '%s' WHERE Username = '%s';",
					newPassword, username));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}

	/**
	 * Internal method which calls the {@link Statement#ExecuteQuery} method
	 * with the specified query.
	 *
	 * @param query
	 *            the SQL statement to search the database with.
	 * @return the results returned from the server.
	 */
	ResultSet query(String query) throws SQLException {
		Statement statement = this.connection.createStatement();
		statement.setQueryTimeout(TIMEOUT);
		return statement.executeQuery(query);
	}

	/**
	 * Internal method which calls the {@link Statement#ExecuteUpdate} method
	 * with the specified query.
	 *
	 * @param query
	 *            the SQL statement to update the database with.
	 */
	void update(String query) throws SQLException {
		Statement statement = this.connection.createStatement();
		statement.setQueryTimeout(TIMEOUT);
		statement.executeUpdate(query);
	}

	/**
	 * Checks if the specified username exists in the database.
	 *
	 * @param username
	 *            the user's username (unique)
	 * @return whether or not the username exists in the database.
	 * @throws DatabaseException
	 *             if there is an error accessing the database.
	 */
	public boolean userExists(String username) throws DatabaseException {
		try {
			return this.query(
					"SELECT * FROM users WHERE Username = '" + username + "';")
					.next();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Retrieves all associated info with the specified ProjectID.
	 *
	 * @param projectId
	 *            the {@link UUID#toString toString} of the UUID of the project.
	 * @return all associated information from the projects table.
	 * @throws DatabaseException
	 *             if there is an error accessing the database.
	 */
	public ResultSet getProjectInfo(String projectId) throws DatabaseException {
		try {
			ResultSet info = this.query(String.format(
					"SELECT * FROM projects WHERE ProjectID = '%s';",
					projectId));
			if (info.next())
				return info;
			throw new DatabaseException("PROJECT_NOT_FOUND");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Retrieves all associated info with the specified ProjectID.
	 *
	 * @param directoryId
	 *            the {@link UUID#toString toString} of the UUID of the
	 *            directory.
	 * @return all associated information from the directories table.
	 * @throws DatabaseException
	 *             if there is an error accessing the database.
	 */
	public ResultSet getDirectoryInfo(String directoryId)
			throws DatabaseException {
		try {
			ResultSet temp = this.query(String.format(
					"SELECT * FROM directories WHERE DirectoryID = '%s';",
					directoryId));
			if (temp.next())
				return temp;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		throw new DatabaseException(FlowServer.ERROR);
	}

	/**
	 * Retrieves the username associated with the specified session ID.
	 *
	 * @param sessionID
	 *            the UUID, in string form, of the session to search for.
	 * @return the username associated with the specified session ID.
	 * @throws DatabaseException
	 *             if there is an error accessing the database or the session
	 *             doesn't exist.
	 */
	public String getUsername(String sessionID) throws DatabaseException {
		try {
			ResultSet data = this.getSessionInfo(sessionID);
			if (!data.next())
				throw new DatabaseException("INVALID_SESSION_ID");
			return data.getString("Username");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DatabaseException(e.getMessage());
		}
	}

	/**
	 * Verifies if the user associated with the specified session ID has at
	 * least VIEW access to the specified project.
	 *
	 * @param sessionID
	 *            the UUID of the session, in String form.
	 * @param projectUUID
	 *            the UUID of the project, in String form.
	 * @return Whether or not the user has at least VIEW access to the specified
	 *         project.
	 * @throws DatabaseException
	 */
	public boolean verifyPermissions(String sessionID, String projectUUID)
			throws DatabaseException {
		try {
			if (!this.query(String.format(
					"SELECT * FROM access WHERE Username = '%s' AND ProjectID = '%s';",
					this.getUsername(sessionID), projectUUID)).next())
				return false;
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Get all users with the specified access level to the specified project.
	 * 
	 * @param projectUUID
	 *            the UUID, in String form, of the project.
	 * @param accessLevel
	 *            the access level, defined in the access constants (
	 *            {@link OWNER}, {@link VIEW}, {@link EDITOR}).
	 * @return the usernames of the users which meet the specified criteria.
	 * @throws DatabaseException
	 */
	public String[] getUsers(String projectUUID, int accessLevel)
			throws DatabaseException {
		// TODO Auto-generated method stub
		try {
			return Results.toStringArray("Username",
					this.query(String.format(
							"SELECT Username FROM Access WHERE ProjectID = '%s' AND AccessLevel = '%d';",
							projectUUID, accessLevel)));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Get all available versions of the specified file.
	 * 
	 * @param fileUUID
	 *            the String representation of the UUID of the file.
	 * @return array containing the string representations of the UUIDs of the
	 *         versions.
	 * @throws DatabaseException
	 *             if there is an error accessing the database.
	 */
	public String[] getFileVersions(String fileUUID) throws DatabaseException {
		try {
			ResultSet response = this.query(String.format(
					"SELECT VersionID FROM Versions WHERE FileID = '%s'",
					fileUUID));
			return Results.toStringArray("VersionID", response);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Retrieve all associated information with the specified version UUID.
	 * 
	 * @param versionUUID
	 *            the String representation of the UUID of the version.
	 * @return All associated information from columns: 'VersionID', 'Date', and
	 *         'DocumentID'
	 * @throws DatabaseException
	 *             if there is an error accessing the database or the version
	 *             doesn't exist.
	 */
	public ResultSet getVersionInfo(String versionUUID)
			throws DatabaseException {
		try {
			ResultSet response = this.query(String.format(
					"SELECT * FROM Versions WHERE VersionID = '%s';",
					versionUUID));
			if (response.next())
				return response;
			throw new DatabaseException("INVALID_VERSION_UUID");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Retrieve save date of the specified version.
	 * 
	 * @param versionUUID
	 *            the String representation of the UUID of the version.
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT,
	 *         for when the version was created.
	 * @throws DatabaseException
	 *             if there is an error accessing the database or the version
	 *             doesn't exist.
	 */
	public long getDate(String versionUUID) throws DatabaseException {
		ResultSet response = this.getVersionInfo(versionUUID);
		try {
			return response.getLong("Date");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Easy retrieval of file type given the UUID.
	 * 
	 * @param fileUUID
	 *            the String representation of the UUID of the file.
	 * @return the type of the file, either {@link ARBITRARY_DOCUMENT} or
	 *         {@link TEXT_DOCUMENT}.
	 * @throws DatabaseException
	 *             if there is an error accessing the database or the file
	 *             doesn't exist.
	 */
	public String getFileType(String fileUUID) throws DatabaseException {
		ResultSet response = this.getFileInfo(fileUUID);
		try {
			return response.getString("FileType");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Retrieves UUID of the latest version of the specified document.
	 * 
	 * @param fileUUID
	 *            the string representation of the UUID of the file.
	 * @return the string representation of the UUID of the latest version of
	 *         the specified file.
	 * @throws DatabaseException
	 *             if there is an error accessing the database or the file
	 *             doesn't exist in the database.
	 */
	public String getLatestVersionUUID(String fileUUID)
			throws DatabaseException {
		ResultSet response;
		try {
			response = this.query(String.format(
					"SELECT VersionID from Versions WHERE Date IN (SELECT MAX(Date) FROM Versions WHERE DocumentID = '%s');",
					fileUUID));
			if (response.next())
				return response.getString("FileID");
			throw new DatabaseException("INVALID_FILE_UUID");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DatabaseException(FlowServer.ERROR);
		}
	}

	/**
	 * Creates a new version of the specified file with the specified version
	 * UUID.
	 * 
	 * @param fileUUID
	 *            the string representation of the UUID of the file.
	 * @param versionUUID
	 *            the string representation of the UUID of the version.
	 * @return the status of the request, either 'OK' or
	 *         {@link FlowServer#ERROR}
	 */
	public String newVersion(String fileUUID, String versionUUID) {
		try {
			this.update(String.format(
					"INSERT INTO Versions VALUES('%s', '%d', '%s');",
					versionUUID, new Date().getTime(), fileUUID));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return FlowServer.ERROR;
		}
		return "OK";
	}
}

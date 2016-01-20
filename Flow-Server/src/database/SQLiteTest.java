package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Testing class for the SQLite database. This allows a command-line-like
 * interface to perform changes onto the database.
 * 
 * Usage:
 * <ul>
 * <li>Decide what type of command you want to perform</li>
 * <li>Type 'a' for any commands which don't return a value (i.e. DELETE,
 * UPDATE, DROP, CREATE, etc.)</li>
 * <li>Type 'b' for any commands which do return values (typically a 'SELECT'
 * statement)</li>
 * <li>Type your statement and press enter</li>
 * <li>If applicable, view results of query in console.</li>
 * </ul>
 * 
 * @version December 17th, 2015
 * @author Bimesh De Silva
 *
 */
public class SQLiteTest {

	public static void main(String[] args) throws SQLException, IOException {
		SQLDatabase database = new SQLDatabase();

		BufferedReader in = new BufferedReader(
				new InputStreamReader(System.in));

		while (true) {
			switch (in.readLine().toLowerCase()) {
			case "a":
				try {
					database.update(in.readLine());
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case "b":
				try {
					// Loop through and display all returned data
					ResultSet results = database.query(in.readLine());
					ResultSetMetaData meta = results.getMetaData();
					int cols = meta.getColumnCount();
					while (results.next()) {
						for (int i = 1; i <= cols; i++) {
							System.out.print(meta.getColumnName(i) + " : "
									+ results.getString(i));
							if (i < cols)
								System.out.print(", ");
						}
						System.out.println();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				System.out.println(
						"Enter 'a' for update (i.e. insert, drop, delete, create, etc. Or select 'b' for a query.");
				break;
			}
		}
	}
}
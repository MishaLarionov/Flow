
package editing;

import gui.FlowClient;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.tree.TreePath;

import login.CreateAccountPane;
import message.Data;
import shared.Communicator;
import shared.FileTree.ProjectNode;

/**
 * Toolbar with Search/Import/Export/Project options buttons
 * 
 * @author Vince Ou
 *
 */
@SuppressWarnings("serial")
public class EditorToolbar extends JToolBar {

	// Keeps track of the buttons
	private JPopupMenu	popup;
	private JMenuItem	createProjectButton;
	private JMenuItem	renameProjectButton;

	/**
	 * Creates a new EditorToolbar
	 * 
	 * @param pane
	 *        the parent EditPane
	 */
	public EditorToolbar(EditPane pane) {
		// Swing setup
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setBorder(FlowClient.EMPTY_BORDER);

		// Creates the project options dialog
		popup = new JPopupMenu("Project Management");
		// Creates a button to create a new project button
		createProjectButton = new JMenuItem();
		createProjectButton.setText("New project");
		createProjectButton.addActionListener(new ActionListener() {

			/**
			 * Asks user for new project name, then sends request to server
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				// Asks user for new name
				String projectName = JOptionPane.showInputDialog(null, "Please enter a name for your new Project\nNo characters such as: \\ / ? % * : | " + "\" < > . # & { } $ @ = ` + ", "New Project", JOptionPane.QUESTION_MESSAGE);
				if (projectName == null) {
					return;
				}
				projectName = projectName.trim();
				while (CreateAccountPane.stringContains(projectName, CreateAccountPane.INVALID_CHARS) || projectName.length() < 1) {
					projectName = JOptionPane.showInputDialog(null, "That name is invalid.\nPlease enter a name for your new Project\nNo characters such as: \\ / ? % * : | " + "\" < > . # & { } $ @ = ` + ", "Invalid name", JOptionPane.QUESTION_MESSAGE);
					if (projectName == null) {
						return;
					}
					projectName = projectName.trim();
				}

				// Sends request to server
				Data createProjectRequest = new Data("new_project");
				createProjectRequest.put("project_name", projectName);
				createProjectRequest.put("session_id", Communicator.getSessionID());
				switch (Communicator.communicate(createProjectRequest).get("status", String.class)) {
				// Success case
					case "OK":
						break;

					// Other cases
					default:
						JOptionPane.showConfirmDialog(null, "Your project name is invalid. Please choose another one.\nThe most likely case is that your project name conflicts with another project name.", "Project creation failure", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
						break;
				}
				// Refreshes
				pane.getFileTree().refreshProjectList();
			}
		});

		// Rename project button
		renameProjectButton = new JMenuItem();
		renameProjectButton.setText("Rename current project");
		renameProjectButton.addActionListener(new ActionListener() {

			/**
			 * Gets the selected project, then asks to be renamed
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				// Gets the path
				TreePath path = pane.getFileTree().getSelectionPath();
				if (path == null) {
					return;
				}
				Object[] pathArray = path.getPath();
				if (pathArray == null) {
					return;
				}
				ProjectNode selectedNode = (ProjectNode) pathArray[1];

				// Asks user for new name
				String modifiedProjectName = JOptionPane.showInputDialog(null, "Please enter new name for the project " + selectedNode.getName() + "\nNo characters such as: \\ / ? % * : | " + "\" < > . # & { } $ @ = ` + ", "Rename project", JOptionPane.QUESTION_MESSAGE);
				if (modifiedProjectName == null) {
					return;
				}
				modifiedProjectName = modifiedProjectName.trim();
				while (CreateAccountPane.stringContains(modifiedProjectName, CreateAccountPane.INVALID_CHARS) || modifiedProjectName.length() < 1) {
					modifiedProjectName = JOptionPane.showInputDialog(null, "That name is invalid.\nPlease enter an appropriate new name for this project." + "\nNo characters such as: \\ / ? % * : | " + "\" < > . # & { } $ @ = ` + ", "Invalid name", JOptionPane.QUESTION_MESSAGE);
					if (modifiedProjectName == null) {
						return;
					}
					modifiedProjectName = modifiedProjectName.trim();
				}

				// Preps request to server
				Data modifyRequest = new Data("project_modify");
				modifyRequest.put("project_modify_type", "RENAME_PROJECT");
				modifyRequest.put("project_uuid", selectedNode.getProjectUUID());
				modifyRequest.put("session_id", Communicator.getSessionID());
				modifyRequest.put("new_name", modifiedProjectName);
				// Sends request to server
				switch (Communicator.communicate(modifyRequest).get("status", String.class)) {
				// Success case
					case "OK":
						((ProjectNode) pane.getFileTree().getSelectionPath().getPath()[1]).setName(modifiedProjectName);
						JOptionPane.showConfirmDialog(null, "Your project has been succesfully renamed to " + modifiedProjectName + ".", "Project renaming success", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
						break;
					// Failure cases
					case "PROJECT_NAME_INVALID":
						JOptionPane.showConfirmDialog(null, "Your project name is invalid.\nPlease choose another one.", "Project renaming failure", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
						break;
					case "PROJECT_DOES_NOT_EXIST":
						JOptionPane.showConfirmDialog(null, "The project you are trying to rename does not exist.\n" + "Try refreshing the list of projects by moving your mouse cursor into,\n" + "then out of the project list.", "Project renaming failure", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
						break;
				}
				// Forces a refresh
				pane.getFileTree().refreshProjectList();
			}
		});

		// Delete project button
		JMenuItem deleteProjectButton = new JMenuItem();
		deleteProjectButton.setText("Delete current project");
		deleteProjectButton.addActionListener(new ActionListener() {

			/**
			 * Gets the active project, makes sure that the user is ABSOLUTELY SURE they want to
			 * delete it, then proceeds
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				// Gets the project
				UUID projectUUID = ((ProjectNode) pane.getFileTree().getSelectionPath().getPath()[1]).getProjectUUID();
				if (projectUUID == null) {
					return;
				}
				// Confirmation dialog
				String confirm = JOptionPane.showInputDialog(null, "Please type the project name that you are intending\n" + "to delete EXACTLY AS IT IS in the following box.\n\n" + "Deleting a project means you will lose ALL data and\n" + "all collaborators will be removed. Back up code accordingly.", "Confirm project deletion",
						JOptionPane.WARNING_MESSAGE);
				if (confirm == null) {
					return;
				}

				// Gets project information (for project name)
				Data projectRequest = new Data("project_info");
				projectRequest.put("session_id", Communicator.getSessionID());
				projectRequest.put("project_uuid", projectUUID);
				Data project = Communicator.communicate(projectRequest);
				// Confirms that they match
				if (confirm.equals(project.get("project_name", String.class))) {
					// Secondary confirmation
					int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete " + project.get("project_name", String.class) + "?", "Confirm project deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (confirmation == JOptionPane.YES_OPTION) {
						// Creates deletion message
						Data deleteProjectRequest = new Data("project_modify");
						deleteProjectRequest.put("project_modify_type", "DELETE_PROJECT");
						deleteProjectRequest.put("project_uuid", projectUUID);
						deleteProjectRequest.put("session_id", Communicator.getSessionID());

						// Sends deletion message
						Data reply = Communicator.communicate(deleteProjectRequest);
						String status = reply.get("status", String.class);
						switch (status) {
						// Success case
							case "OK":
								project = null;
								pane.getFileTree().refreshProjectList();
								break;

							// Failure cases
							default:
								break;
						}
					} else
						return;
				} else {
					// Typo!
					JOptionPane.showConfirmDialog(null, "The project name is incorrect.\nNothing has been changed.", "Deletion failed", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

		});

		// Adds the three buttons to the popup menu
		popup.add(createProjectButton);
		popup.add(renameProjectButton);
		popup.add(deleteProjectButton);

		// Adds buttons
		add(new SearchButton());
		add(new ProjectManageButton());
		add(new ImportButton());
		add(new ExportButton());
		addSeparator();

		// Does things.
		setFloatable(false);
		setRollover(true);
	}

	/**
	 * Button to search the current document for text
	 * 
	 * @author Vince Ou
	 *
	 */
	private class SearchButton extends JButton {

		/**
		 * Creates a new SearchButton
		 */
		private SearchButton() {
			// Sets an icon
			try {
				setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResource("images/search.png")).getScaledInstance(FlowClient.BUTTON_ICON_SIZE, FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			setFocusable(false);
			setBorder(FlowClient.EMPTY_BORDER);
			addActionListener(new ActionListener() {

				/**
				 * Goes through the currently active window and searches for a string
				 */
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO pop open a search window and search for something
					System.out.println("Search button pressed");
				}
			});
		}
	}

	/**
	 * Button that pens the project management menu
	 * 
	 * @author Vince Ou
	 *
	 */
	private class ProjectManageButton extends JButton {

		/**
		 * Opens up the good ol' project management menu created up there
		 */
		private ProjectManageButton() {
			// Sets an icon
			try {
				setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResource("images/projectManage.png")).getScaledInstance(FlowClient.BUTTON_ICON_SIZE, FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			setFocusable(false);
			setBorder(FlowClient.EMPTY_BORDER);
			addActionListener(new ActionListener() {

				/**
				 * Shows the JPopup Menu
				 */
				@Override
				public void actionPerformed(ActionEvent e) {
					popup.show(EditorToolbar.this, ProjectManageButton.this.getX(), ProjectManageButton.this.getY());
				}
			});
		}
	}

	/**
	 * Button that will import a file to a FlowProject
	 * 
	 * @author Vince Ou
	 *
	 */
	private class ImportButton extends JButton {

		/**
		 * Creates a new ImportButton
		 */
		private ImportButton() {
			// Sets an icon
			try {
				setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResource("images/import.png")).getScaledInstance(FlowClient.BUTTON_ICON_SIZE, FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			setFocusable(false);
			setBorder(FlowClient.EMPTY_BORDER);
			addActionListener(new ActionListener() {

				/**
				 * Gets the selected directory, and imports a file into there
				 */
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO pop open a window for the user to first select a
					// file, then to choose the project to insert it in
					System.out.println("Import button pressed");
				}
			});
		}
	}

	/**
	 * Button to export a file out of Flow onto the user's desktop
	 * 
	 * @author Vince Ou
	 *
	 */
	private class ExportButton extends JButton {

		private ExportButton() {
			// Sets the icon
			try {
				setIcon(new ImageIcon(ImageIO.read(ClassLoader.getSystemResource("images/export.png")).getScaledInstance(FlowClient.BUTTON_ICON_SIZE, FlowClient.BUTTON_ICON_SIZE, Image.SCALE_SMOOTH)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			setFocusable(false);
			setBorder(FlowClient.EMPTY_BORDER);
			addActionListener(new ActionListener() {

				/**
				 * Confirm that is the file to be exported, and copies it into a directory of their
				 * choosing
				 */
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO pop open a window asking where the user would like
					// the file exported, then export file to that location.
					System.out.println("Export button pressed");
				}
			});
		}
	}

	/**
	 * To avoid duplicate code. Will create a new project.
	 */
	public void createProjectButtonDoClick() {
		createProjectButton.doClick();
	}

	/**
	 * To avoid duplicate code. Will rename a project.
	 */
	public void renameProjectButtonDoClick() {
		renameProjectButton.doClick();
	}
}

package shared;

import gui.FlowClient;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import message.Data;
import struct.FlowDirectory;
import struct.FlowFile;
import struct.FlowProject;

@SuppressWarnings("serial")
public abstract class DocTree extends JTree {

    private DefaultTreeModel model;
    private JScrollPane scrollView;

    private UUID[] usersProjectsUUIDs;
    private FlowProject activeProject;

    public DocTree() {
	// UIManager.put("Tree.closedIcon", icon);
	// UIManager.put("Tree.openIcon", icon);
	// UIManager.put("Tree.leafIcon", icon);
	setMinimumSize(new Dimension(100, 0));
	setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	setBorder(FlowClient.EMPTY_BORDER);
	scrollView = new JScrollPane(this);
	model = new DefaultTreeModel(new DefaultMutableTreeNode("Workspace"));
	setModel(model);
	getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

	addMouseListener(new MouseAdapter() {
	    @Override
	    public void mouseEntered(MouseEvent arg0) {
		refreshProjectList();
	    }
	});
	// refreshProjectList();
    }

    public JScrollPane getScrollable() {
	return scrollView;
    }

    public FlowProject getActiveProject() {
	return activeProject;
    }

    public void setActiveProject(FlowProject newActive) {
	activeProject = newActive;
    }

    public void refreshProjectList() {
	if (FlowClient.NETWORK) {
	    Data projectList = new Data("list_projects");
	    projectList.put("session_id", Communicator.getSessionID());
	    Data reply = Communicator.communicate(projectList);
	    System.out.println(reply);
	    usersProjectsUUIDs = reply.get("projects", UUID[].class);

	    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	    // Adds a new project
	    for (UUID uuid : usersProjectsUUIDs) {
		boolean projectExistsLocally = false;
		for (int i = root.getChildCount() - 1; !projectExistsLocally && i >= 0; i--) {
		    if (((ProjectNode) root.getChildAt(i)).getProject().getProjectUUID().equals(uuid)) {
			projectExistsLocally = true;
		    }
		}
		if (!projectExistsLocally) {
		    createProjectNode(uuid);
		}
	    }

	    // Deletes projects that don't exist
	    for (int i = root.getChildCount() - 1; i >= 0; i--) {
		boolean projectStillExists = false;
		for (int j = 0; j < usersProjectsUUIDs.length && !projectStillExists; j++) {
		    if (usersProjectsUUIDs[j].equals(((ProjectNode) root.getChildAt(i)).getProject().getProjectUUID())) {
			projectStillExists = true;
		    }
		}
		if (!projectStillExists) {
		    root.remove(i);
		}
	    }
	}
    }

    private void createProjectNode(UUID projectUUID) {
	if (FlowClient.NETWORK) {
	    Data fileListRequest = new Data("request_project");
	    fileListRequest.put("project_uuid", projectUUID);
	    fileListRequest.put("session_id", Communicator.getSessionID());
	    FlowProject project = Communicator.communicate(fileListRequest).get("project", FlowProject.class);

	    ProjectNode newProjectNode = new ProjectNode(project);
	    ((DefaultMutableTreeNode) model.getRoot()).add(newProjectNode);
	    loadProjectFiles(project, newProjectNode);
	}
    }

    private void loadProjectFiles(FlowDirectory fDir, DirectoryNode dir) {
	// adds folders
	if (!fDir.getDirectories().isEmpty()) {
	    for (FlowDirectory subDir : fDir.getDirectories()) {
		DirectoryNode subDirNode = new DirectoryNode(subDir);
		loadProjectFiles(subDir, subDirNode);
	    }
	}

	// adds files
	if (!fDir.getFiles().isEmpty()) {
	    for (FlowFile file : fDir.getFiles()) {
		dir.add(new FileNode(file));
	    }
	}
    }

    public class ProjectNode extends DirectoryNode {
	private FlowProject project;

	public ProjectNode(FlowProject project) {
	    super(project);
	    this.project = project;
	}

	public FlowProject getProject() {
	    return project;
	}

	public String toString() {
	    return project.toString();
	}
    }

    public class DirectoryNode extends DefaultMutableTreeNode {
	private FlowDirectory folder;

	public DirectoryNode(FlowDirectory dir) {
	    super(dir.toString());
	    this.folder = dir;
	}

	public FlowDirectory getDirectory() {
	    return folder;
	}

	public String toString() {
	    return folder.toString();
	}
    }

    public class FileNode extends DefaultMutableTreeNode {
	private FlowFile file;

	public FileNode(FlowFile file) {
	    this.file = file;
	}

	public FlowFile getFile() {
	    return file;
	}

	public String toString() {
	    return file.toString();
	}
    }
}
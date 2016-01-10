package shared;

import gui.FlowClient;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
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
    private DirectoryNode activeDirectoryNode;
    private FileNode activeFileNode;

    public DocTree() {
	// TODO
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
	    public void mouseClicked(MouseEvent e) {
		if (e.isAltDown())
		    refreshProjectList();
		// else if (e.isShiftDown()) {
		// DefaultMutableTreeNode root = (DefaultMutableTreeNode)
		// model.getRoot();
		// System.out.println(root);
		// root.removeAllChildren();
		// model.reload();
		// revalidate();
		// repaint();
		// }
	    }
	});
	addTreeSelectionListener(new TreeSelectionListener() {

	    @Override
	    public void valueChanged(TreeSelectionEvent e) {
		TreePath treePath = e.getPath();
		if (treePath == null)
		    return;

		DefaultMutableTreeNode selected = (DefaultMutableTreeNode) treePath.getLastPathComponent();

		if (selected instanceof ProjectNode) {
		    setActiveProject(((ProjectNode) selected).getProject());
		    setActiveDirectoryNode((ProjectNode) selected);
		} else if (selected instanceof DirectoryNode) {
		    setActiveProject(((FlowProject) ((DirectoryNode) selected).getDirectory().getRootDirectory()));
		    setActiveDirectoryNode((DirectoryNode) selected);
		} else if (selected instanceof FileNode) {
		    FileNode fileNode = (FileNode) selected;
		    setActiveProject((FlowProject) fileNode.getFile().getParentDirectory().getRootDirectory());
		    setActiveDirectoryNode((DirectoryNode) ((FileNode) selected).getParent());
		    setActiveFileNode(fileNode);
		} else {
		    if (((DefaultMutableTreeNode) selected).getChildCount() == 0) {
			refreshProjectList();
		    }

		}
	    }
	});
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

    public FileNode getActiveFileNode() {
	return activeFileNode;
    }

    public void setActiveFileNode(FileNode activeFileNode) {
	this.activeFileNode = activeFileNode;
    }

    public DirectoryNode getActiveDirectoryNode() {
	return activeDirectoryNode;
    }

    public void setActiveDirectoryNode(DirectoryNode activeDirectoryNode) {
	this.activeDirectoryNode = activeDirectoryNode;
    }

    public void refreshProjectList() {
	if (!FlowClient.NETWORK) {
	    return;
	} else {
	    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	    Data projectList = new Data("list_projects");
	    projectList.put("session_id", Communicator.getSessionID());
	    Data reply = Communicator.communicate(projectList);
	    usersProjectsUUIDs = reply.get("projects", UUID[].class);
	    if (usersProjectsUUIDs == null) {
		return;
	    }

	    // Adds a new project
	    for (UUID uuid : usersProjectsUUIDs) {
		boolean projectExistsLocally = false;
		for (int i = root.getChildCount() - 1; !projectExistsLocally && i >= 0; i--) {
		    if (((ProjectNode) root.getChildAt(i)).getProject().getProjectUUID().equals(uuid)) {
			projectExistsLocally = true;
		    }
		}
		if (!projectExistsLocally) {
		    createProjectNode(uuid).getProject();
		}
	    }

	    // Deletes projects that don't exist
	    for (int i = root.getChildCount() - 1; i >= 0; i--) {
		boolean projectExistsRemotely = false;
		for (int j = 0; j < usersProjectsUUIDs.length && !projectExistsRemotely; j++) {
		    if (usersProjectsUUIDs[j].equals(((ProjectNode) root.getChildAt(i)).getProject().getProjectUUID())) {
			projectExistsRemotely = true;
		    }
		}
		if (!projectExistsRemotely) {
		    model.removeNodeFromParent((MutableTreeNode) root.getChildAt(i));
		}
	    }
	    model.reload(root);
	    revalidate();
	    repaint();
	}
    }

    private ProjectNode createProjectNode(UUID projectUUID) {
	if (FlowClient.NETWORK) {
	    Data fileListRequest = new Data("request_project");
	    fileListRequest.put("project_uuid", projectUUID);
	    fileListRequest.put("session_id", Communicator.getSessionID());
	    FlowProject project = Communicator.communicate(fileListRequest).get("project", FlowProject.class);

	    ProjectNode newProjectNode = new ProjectNode(project);
	    ((DefaultMutableTreeNode) model.getRoot()).add(newProjectNode);
	    getProjectFiles(project, newProjectNode);
	    return newProjectNode;
	}
	return null;
    }

    private void getProjectFiles(FlowDirectory fDir, DirectoryNode dir) {
	// adds folders
	if (!fDir.getDirectories().isEmpty()) {
	    for (FlowDirectory subDir : fDir.getDirectories()) {
		DirectoryNode subDirNode = new DirectoryNode(subDir);
		getProjectFiles(subDir, subDirNode);
	    }
	}

	// adds files
	if (!fDir.getFiles().isEmpty()) {
	    for (FlowFile file : fDir.getFiles()) {
		dir.add(new FileNode(file));
	    }
	}
    }

    public void reloadProjectFiles(ProjectNode projectNode) {
	// TODO see if you can combine these two into one loop
	DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[projectNode.getChildCount()];
	for (int i = 0; i < projectNode.getChildCount(); i++) {
	    children[i] = (DefaultMutableTreeNode) projectNode.getChildAt(i);
	}
	for (DefaultMutableTreeNode child : children) {
	    model.removeNodeFromParent(child);
	}
	model.reload(projectNode);

	Data projectReload = new Data("request_project");
	projectReload.put("project_uuid", projectNode.getProject().getProjectUUID());
	FlowProject reloadedProject = Communicator.communicate(projectReload).get("project", FlowProject.class);
	if (reloadedProject == null) {
	    JOptionPane.showConfirmDialog(null, "The project couldn't be found.\nTry refreshing the project list by Alt + clicking.", "Project retrieval error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
	    return;
	}

	reloadProjectFilesRecursively(reloadedProject, projectNode);

	projectNode.setProject(reloadedProject);
    }

    private void reloadProjectFilesRecursively(FlowDirectory fDir, DirectoryNode dir) {
	ArrayList<FlowDirectory> localDirs = new ArrayList<FlowDirectory>();
	ArrayList<FlowFile> localFiles = new ArrayList<FlowFile>();

	for (int i = 0; i < dir.getChildCount(); i++) {
	    DefaultMutableTreeNode child = (DefaultMutableTreeNode) dir.getChildAt(i);
	    if (child instanceof DirectoryNode) {
		int indexInDirectory = fDir.getDirectories().indexOf(((DirectoryNode) child).getDirectory());
		if (indexInDirectory == -1) {
		    model.removeNodeFromParent(child);
		} else {
		    reloadProjectFilesRecursively(fDir.getDirectories().get(indexInDirectory), (DirectoryNode) child);
		    localDirs.add(((DirectoryNode) child).getDirectory());
		}
	    } else if (child instanceof FileNode) {
		int indexOfFile = fDir.getFiles().indexOf(((FileNode) child).getFile());
		if (indexOfFile == -1) {
		    model.removeNodeFromParent(child);
		} else {
		    localFiles.add(((FileNode) child).getFile());
		}
	    }
	}

	for (FlowDirectory remoteDir : fDir.getDirectories()) {
	    if (localDirs.indexOf(remoteDir) == -1) {
		DirectoryNode newNode = new DirectoryNode(remoteDir);
		model.insertNodeInto(newNode, dir, 0);
		reloadProjectFilesRecursively(remoteDir, newNode);
	    }
	}
	for (FlowFile remoteFile : fDir.getFiles()) {
	    if (localFiles.indexOf(remoteFile) == -1) {
		model.insertNodeInto(new FileNode(remoteFile), dir, 0);
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

	private void setProject(FlowProject newProject) {
	    project = newProject;
	}
    }

    public class DirectoryNode extends DefaultMutableTreeNode {
	private FlowDirectory folder;

	public DirectoryNode(FlowDirectory dir) {
	    super(dir);
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
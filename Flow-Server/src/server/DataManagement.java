package server;

import struct.FlowFile;
import struct.FlowProject;
import struct.TextDocument;
import struct.User;
import util.FileSerializer;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by Netdex on 1/5/2016.
 */
public class DataManagement {

    private static DataManagement instance;
    private static Logger L = Logger.getLogger("DataManagement");
    private static FileSerializer fileSerializer = new FileSerializer();
    private File dataFile;

    public static DataManagement getInstance() {
        if (instance == null)
            instance = new DataManagement();
        return instance;
    }

    private DataManagement() {

    }

    public void init(File dataFile) {
        this.dataFile = dataFile;
        L.info("loading data files from file");
        if (!dataFile.exists())
            dataFile.mkdir();
    }

    public boolean addUser(User u) {
        L.info("adding user " + u);
        File userDirectory = new File(dataFile.getAbsolutePath(), "users");
        userDirectory.mkdir();
        fileSerializer.writeToFile(new File(userDirectory.getAbsolutePath(),
                u.getUsername() + ".flow"), u);
        return true;
    }

    public boolean removeUser(String username) {
        L.info("removing user");
        File userFile = new File(new File(dataFile, "users"), username + ".flow");
        return userFile.delete();
    }

    public User getUserByUsername(String username) {
        L.info("getting user " + username + " by username");
        File userFile = new File(new File(dataFile, "users"),
                username + ".flow");
        if (!userFile.exists())
            return null;
        return fileSerializer.readFromFile(userFile, User.class);
    }

    public boolean addProjectToUser(FlowProject project) {
        L.info("adding project " + project.getDirectoryName());
        File projectDirectory = new File(new File(dataFile, "projects"),
                project.getProjectUUID().toString());
        if (projectDirectory.exists())
            return false;
        projectDirectory.mkdir();
        File projectMetadataFile = new File(projectDirectory, "project.flow");
        fileSerializer.writeToFile(projectMetadataFile, project);
        return true;
    }

    public boolean removeProject(UUID uuid) {
        L.info("removing project with uuid " + uuid);
        File projectDirectory = new File(new File(dataFile, "projects"),
                uuid.toString());
        if (!projectDirectory.exists())
            return false;
        projectDirectory.delete();
        return true;
    }

    public FlowProject getProjectFromUUID(UUID uuid) {
        L.info("getting project with uuid " + uuid);
        File projectDirectory = new File(new File(dataFile, "projects"),
                uuid.toString());
        if (!projectDirectory.exists())
            return null;
        File projectMetadataFile = new File(projectDirectory, "project.flow");
        FlowProject project = fileSerializer.readFromFile(projectMetadataFile,
                FlowProject.class);
        return project;
    }

    public boolean renameProject(UUID uuid, String newName) {
        L.info("renaming project with uuid " + uuid);
        FlowProject oldProject = this.getProjectFromUUID(uuid);
        if (oldProject == null)
            return false;
        oldProject.setDirectoryName(newName);
        File projectMetadata = new File(
                new File(new File(dataFile, "projects"), uuid.toString()),
                "project.flow");
        if (!projectMetadata.exists())
            return false;
        projectMetadata.delete();
        fileSerializer.writeToFile(projectMetadata, oldProject);
        return true;
    }

    public boolean createFolderInProject(UUID projectUUID, UUID... path) {
        String pathstr = "";
        for (UUID u : path) {
            pathstr += u + File.separator;
        }
        File directory = new File(new File(new File(dataFile, "projects"), projectUUID.toString()), pathstr);
        if (directory.exists())
            return false;
        directory.mkdirs();
        return true;
    }

    public boolean deleteFolderInProject(UUID projectUUID, UUID... path) {
        String pathstr = "";
        for (UUID u : path) {
            pathstr += u + File.separator;
        }
        File directory = new File(new File(new File(dataFile, "projects"), projectUUID.toString()), pathstr);
        if (!directory.exists())
            return false;
        directory.delete();
        return true;
    }

    public boolean renameFolderInProject(UUID projectUUID, UUID newUUID, UUID... path){
        String pathstr = "";
        for (UUID u : path) {
            pathstr += u + File.separator;
        }
        File directory = new File(new File(new File(dataFile, "projects"), projectUUID.toString()), pathstr);
        if (!directory.exists())
            return false;
        directory.renameTo(new File(newUUID.toString()));
        return true;
    }

    public boolean addTextDocumentToProject(UUID projectUUID,
                                            TextDocument textDoc) {
        File projectDirectory = new File(new File(dataFile, "projects"),
                projectUUID.toString());
        File f = new File(projectDirectory, textDoc.getParentFile()
                .getParentDirectory().getFullyQualifiedPath());
        if (f.exists())
            return false;
        f.mkdirs();
        FileSerializer fs = new FileSerializer();
        File ff = new File(
                f.getAbsolutePath() + File.separator + textDoc.getUUID());
        if (ff.exists())
            return false;
        fs.writeToFile(ff, textDoc);
        return true;
    }

    public boolean removeTextFileFromProject(String username, UUID projectUUID,
                                             TextDocument textDocument) {
        // TODO rewrite this it is wrong
        File f = new File(dataFile.getAbsolutePath() + File.separator + username
                + File.separator + projectUUID.toString() + File.separator
                + textDocument.getParentFile().getParentDirectory()
                .getFullyQualifiedPath()
                + File.separator + textDocument.getUUID().toString());
        if (!f.exists())
            return false;
        f.delete();
        return true;
    }

    public FlowFile getFileFromPath(UUID projectUUID, String path,
                                    UUID fileUUID) {

        // TODO rewrite this to use path concatenation
        File f = new File(dataFile.getAbsolutePath() + File.separator
                + "projects" + File.separator + projectUUID.toString()
                + File.separator + path + File.separator + fileUUID);
        if (!f.exists())
            return null;
        File ff = new File(f.getAbsolutePath() + File.separator + "file.flow");
        if (!ff.exists())
            return null;
        FileSerializer fs = new FileSerializer();
        FlowFile fff = fs.readFromFile(ff, FlowFile.class);
        return fff;
    }

    public TextDocument getTextDocumentFromPath(String username,
                                                UUID projectUUID, String path, UUID fileUUID, UUID versionUUID) {
        // TODO rewrite this is wrong
        File f = new File(dataFile.getAbsolutePath() + File.separator + username
                + File.separator + projectUUID.toString() + File.separator
                + path + File.separator + fileUUID.toString() + File.separator
                + versionUUID.toString());
        if (!f.exists())
            return null;
        File ff = new File(f.getAbsolutePath() + File.separator + "file.flow");
        if (!ff.exists())
            return null;
        FileSerializer fs = new FileSerializer();
        TextDocument fff = fs.readFromFile(ff, TextDocument.class);
        return fff;
    }
}

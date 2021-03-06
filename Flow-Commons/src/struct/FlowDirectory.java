package struct;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a directory in the flow file system
 * <p>
 * Created by Netdex on 12/29/2015.
 */
public class FlowDirectory implements Serializable {

    private FlowDirectory parent;
    private ArrayList<FlowDirectory> childDirectories;
    private ArrayList<FlowFile> childFiles;

    private String dirName;

    public FlowDirectory(String dirName) {
        this.childDirectories = new ArrayList<>();
        this.childFiles = new ArrayList<>();
        this.dirName = dirName;
        // TODO check directory name for invalid characters, throw exception if not
    }

    public FlowDirectory(FlowDirectory parent, String dirName) {
        this(dirName);
        this.parent = parent;
    }

    /**
     * Gets the parent of this directory, null if root directory
     *
     * @return the parent of this directory, null if root directory
     */
    public FlowDirectory getParent() {
        return parent;
    }

    /**
     * Adds a directory as a child of this one
     *
     * @param directory The directory to add as a child
     */
    public void addDirectory(FlowDirectory directory) {
        childDirectories.add(directory);
    }

    /**
     * Adds a file as a child of this directory
     *
     * @param file The file to add as a child
     */
    public void addFile(FlowFile file) throws DuplicateFileNameException {
        for (FlowFile childFile : childFiles) {
            if (childFile.getFileName().equals(file.getFileName()))
                throw new DuplicateFileNameException();
        }
        childFiles.add(file);
    }

    /**
     * Gets all child directories
     *
     * @return all child directories
     */
    public ArrayList<FlowDirectory> getDirectories() {
        return childDirectories;
    }

    /**
     * Gets all child files
     *
     * @return all child files
     */
    public ArrayList<FlowFile> getFiles() {
        return childFiles;
    }

    /**
     * @return the fully qualified path from root directory
     */
    public String getFullyQualifiedPath() {
        String path = "";
        FlowDirectory cd = this;
        while (cd.getParent() != null) {
            path += cd.getDirectoryName() + "/";
            cd = cd.getParent();
        }
        return path;
    }

    /**
     * @return the root directory
     */
    public FlowDirectory getRootDirectory() {
        FlowDirectory cd = this;
        while (cd.getParent() != null) {
            cd = cd.getParent();
        }
        return cd;
    }

    public String getDirectoryName() {
        return dirName;
    }

    @Override
    public String toString() {
        return dirName;
    }

    static class DuplicateFileNameException extends Exception {
    }

    static class InvalidFileNameException extends Exception {
    }
}

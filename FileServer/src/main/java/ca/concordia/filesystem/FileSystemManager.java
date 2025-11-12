package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;

    private final static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if(instance == null) {
            // Initialize the file system
           instance = this;

           File newFile = new File(filename);
           if (newFile.exists()) {
                newFile.createNewFile();
           }

           disk = new RandomAccessFile(newFile, "rw");      // Open file in read-write mode
           
           inodeTable = new FEntry[MAXFILES];
           freeBlockList = new boolean[MAXBLOCKS];
           Arrays.fill(freeBlockList, true);        // All blocks are free initially

           for (int i = 0; i < MAXFILES; i++) {
               inodeTable[i] = null;                // No files initially
           }

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }


    // Create a new file
    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            if (fileName.length() > 11) {
                throw new Exception("File name is longer than 11 characters.");
            }

            for (FEntry entry : inodeTable) {
                if (entry != null && entry.getFilename().equals(fileName)) {
                    throw new Exception("File already exists.");
                }
            }

            int emptySpot = -1;
            for (int i = 0: i< MAXFILES; i++) {
                if (inodeTable[i] == null) {
                    emptySpot = i;
                    break;
                }
            }

            if (emptySpot == -1) {          // First empty inode table spot
                throw new Exception("Maximum file limit reached.");
            }
            inodeTable[emptySpot] = new FEntry(fileName, (short)0, (short)-1);         // Create new file entry
        } finally {
            globalLock.unlock();
        }
    }


    // Read from a file
    public byte[] readFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry entry = findEntry(fileName);             *****
            if (entry == null) {
                throw new Exception("File not found.");
            }
            if (entry.getFirstBlock() == -1 || entry.getFilesize() == 0) {
                return new byte[0];         // Empty file
            }
        
            byte[] data = new byte[entry.getFilesize()];        // Create byte array to store file data, the size of the file
            disk.seek((long)entry.getFirstBlock() * BLOCK_SIZE);
            disk.readFully(data, 0, entry.getFilesize());       // Start at index 0 and reads filesize bytes
           
            return data;  

        } finally {
            globalLock.unlock();
        }
    }


    // Write to a file
    public void writeFile(String fileName, byte[] data, int size) throws Exception {
        globalLock.lock();
        try{
            FEntry entry = findEntry(fileName);
            if (entry == null) {
                throw new Exception("File not found.");
            }



        } finally {
            globalLock.unlock();
        }
        
    }


    // Delete a file
    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();
        try{
            FEntry entry = findEntry(fileName);
            if (entry == null) {
                throw new Exception("File not found.");
            }

            // Free allocation of blocks
            freeFileBlocks(entry);

            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] == entry){
                    inodeTable[i] = null;        // Remove file entry
                    break;
                }
            }
        } finally {
            globalLock.unlock();
        }
    }



    // List all files
    public String[] listFiles() {
        globalLock.lock();
        try {
            List<String> fileList = new ArrayList<>();
            for (FEntry entry : inodeTable) {
                if (entry != null) {
                    fileList.add(entry.getFilename());
                }
            }
            return fileList.toArray(new String[0]);
        } finally {
            globalLock.unlock();
        }
    }



// Method to find a file entry by name
private FEntry findEntry(String fileName) {
        for (FEntry entry : inodeTable) {
            if (entry != null && entry.getFilename().equals(fileName)) {
                return entry;
            }
        }
        return null;
}


private void freeFileBlocks(FEntry entry) throws IOException{

        short blockIndex = entry.getFirstBlock();   // Get the first block index
        if (blockIndex >= 0){
            freeBlockList[blockIndex] = true;                // Free the block
            disk.seek((long) blockIndex * BLOCK_SIZE);       // Move the file pointer to the start of the block
            disk.write(new byte[BLOCK_SIZE]);                // Clear block data
        }
}












}

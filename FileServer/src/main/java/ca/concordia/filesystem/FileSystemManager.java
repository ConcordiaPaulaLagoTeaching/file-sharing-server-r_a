package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;

    private final Semaphore mutex = new Semaphore(1);      // protects readerCount
    private final Semaphore writeLock = new Semaphore(1);  // blocks writers OR readers
    private final Semaphore readerBlock = new Semaphore(1); // using this to prevent starvation of a reader
    private int readerCount = 0;

//    private final static FileSystemManager instance;
    //Implement as a singleton class (so one instance but a global point of access)
    private static volatile FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws IOException{
        // Initialize the file system manager with a file
        if(instance == null) {
            // Initialize the file system
           instance = this;

           File newFile = new File(filename);
           if (!newFile.exists()) {
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
            for (int i = 0; i< MAXFILES; i++) {
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
        // globalLock.lock();
        startRead();
        try {
            FEntry entry = findEntry(fileName);
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
            // globalLock.unlock();
            endRead(); // releases lock
        }
    }


    // Write to a file
    public void writeFile(String fileName, byte[] data) throws Exception {
        // globalLock.lock();
        startWrite();
        try{
            FEntry entry = findEntry(fileName);
            if (entry == null) {
                throw new Exception("File not found.");
            }

            int size = data.length;
            int numBlocks = (int) Math.ceil((double) size / BLOCK_SIZE);

            int freeBlocks = countFreeBlocks();
            if (numBlocks > freeBlocks) {
                throw new Exception("File too large.");
            }

            // Free existing blocks
            freeFileBlocks(entry);

            // Now, we can write the new data
            int bytesWritten = 0;
            short firstBlockIndex = -1;

            for (int i = 0; i < numBlocks; i++) {
                int blockIndex = findFreeBlock();
                if (blockIndex == -1) {
                    throw new Exception("No free blocks available.");
                }

                freeBlockList[blockIndex] = false; // Mark block as used

                // Moving the file pointer to the start of the block
                disk.seek((long) blockIndex * BLOCK_SIZE);

                // Write data to the block
                disk.write(data, bytesWritten, Math.min(BLOCK_SIZE, size - bytesWritten));

                if (i == 0) {
                    firstBlockIndex = (short) blockIndex; // Store the index of the first block
                }
                bytesWritten += Math.min(BLOCK_SIZE, size - bytesWritten);
            }
            if (firstBlockIndex != -1) {
                entry.setFirstBlock(firstBlockIndex);   // update inode to point to first block
                entry.setFilesize((short) size);        // store file size (watch max size vs short limit)
                // Optionally persist inode table or metadata to disk here
            }


        } finally {
            // globalLock.unlock();
            endWrite(); // release lock
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
            return fileList.toArray(new String[0]);     // Convert List to Array and return
        } finally {
            globalLock.unlock();
        }
    }



// Finding a file entry by name
private FEntry findEntry(String fileName) {
        for (FEntry entry : inodeTable) {
            if (entry != null && entry.getFilename().equals(fileName)) {
                return entry;
            }
        }
        return null;
}


// Counting number of free blocks
private int countFreeBlocks() {
        int count = 0;
        for (boolean isFree : freeBlockList) {
            if (isFree) {
                count++;
            }
        }
        return count;
}


// Finding a free block index
private int findFreeBlock() {
        for (int i = 0; i < freeBlockList.length; i++) {
            if (freeBlockList[i]) {
                return i;
            }
        }
        return -1; // If none are free
}



// Freeing file blocks and erasing their contents
private void freeFileBlocks(FEntry entry) throws IOException{

        short blockIndex = entry.getFirstBlock();   // Get the first block index
        if (blockIndex >= 0){
            freeBlockList[blockIndex] = true;                // Free the block
            disk.seek((long) blockIndex * BLOCK_SIZE);       // Move the file pointer to the start of the block
            disk.write(new byte[BLOCK_SIZE]);                // Erase old data
        }
}

// Semaphore/Deadlock Prevention Operations
/*
 * block writing when there is a reader
 * block reading when there is a writer
 * If a writer enters the queue, block readerCount from incrementing
 * Meaning that if a writer is waiting, we dont want anymore readers, giving the writer a chance
 */

private void startRead() throws InterruptedException {
    readerBlock.acquire();
    readerBlock.release();
    mutex.acquire();               // lock counter
    readerCount++;
    if (readerCount == 1) {
        writeLock.acquire();       // first reader blocks writers
    }
    mutex.release();
}

private void endRead() throws InterruptedException {
    mutex.acquire();
    readerCount--;
    if (readerCount == 0) {
        writeLock.release();       // last reader unblocks writers
    }
    mutex.release();
}

private void startWrite() throws InterruptedException {
    readerBlock.acquire();          //block new readers
    writeLock.acquire();           // only one writer allowed, blocks all readers
}

private void endWrite() {
    writeLock.release();        
    readerBlock.release();      //allow new readers
}










}

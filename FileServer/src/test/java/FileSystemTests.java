import ca.concordia.filesystem.FileSystemManager;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTests {
    static FileSystemManager fs;

    @BeforeAll
    static void setup() throws Exception {
        fs = new FileSystemManager("testfs.dat", 10 * 128);
    }

    @Test
    void testCreateFile() throws Exception {
        fs.createFile("a.txt");
        boolean fileFound = false;
        for (String fileName : fs.listFiles()) {
            if(fileName.equals("a.txt")) {
                fileFound = true;
                break;
            }
        }
        assertTrue(fileFound);
    }

    @Test
    void testTooLongFilename() {
        Exception ex = assertThrows(Exception.class, () -> fs.createFile("verylongname.txt"));
        assertTrue(ex.getMessage().toLowerCase().contains("filename"));
        assertTrue(ex.getMessage().toLowerCase().contains("long"));
    }

    @Test
    void testWriteAndReadFile() throws Exception {
        fs.createFile("a.txt");
        fs.writeFile("a.txt", "hello".getBytes());
        assertEquals("hello", new String(fs.readFile("a.txt")));
    }

    @Test
    void testWriteAndReadLongFile() throws Exception {
        fs.createFile("c.txt");
        String longContent = "This is a long content that exceeds 128 bytes. ".repeat(5);
        fs.writeFile("c.txt", longContent.getBytes());
        assertEquals(longContent, new String(fs.readFile("c.txt")));
    }

    @Test
    void testDeleteFile() throws Exception {
        fs.createFile("b.txt");
        fs.deleteFile("b.txt");
        for(String fileName : fs.listFiles()){
            assertNotEquals("b.txt", fileName);
        }
    }
}

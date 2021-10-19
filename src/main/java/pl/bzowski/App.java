package pl.bzowski;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import  me.tongfei.progressbar.ProgressBar;
import javax.swing.filechooser.FileSystemView;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

public class App {
    private static final String ICEDRIVE_HOST = "https://webdav.icedrive.io/";
    private static long reduce;
    private static List<MyLocalFile> myLocalFiles;

    public static void main(String[] args) throws IOException {
        // Pobierz nazwę komputera
        String computerName = getComputerName();
        System.out.println(computerName);

        printDriveLetters();

        NumberFormat nf = NumberFormat.getNumberInstance();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            System.out.print(root + ": ");
            try {
                FileStore fileStore = Files.getFileStore(root);
                System.out.println("available=" + nf.format(fileStore.getUsableSpace()) + ", total="
                        + nf.format(fileStore.getTotalSpace()));
                        
            } catch (IOException e) {
                System.out.println("error querying space: " + e.toString());
            }
        }

        List<MyLocalFile> myLocalFiles = getAllFilesForBackup();

        reduce = myLocalFiles.stream().mapToLong(MyLocalFile::getSize).reduce(0L, (a, b) -> a + b);
        System.out.println("To Waży: " + reduce);
        
        // Sardine sardine = SardineFactory.begin(args[0], args[1]);

        // sardine.list(ICEDRIVE_HOST)
        // .stream()
        // .filter(davResource -> davResource.getName().equals(computerName))
        // .findFirst()
        // .ifPresent(davResource -> System.out.println(davResource.getName()));

        // if (sardine.exists(ICEDRIVE_HOST + computerName)) {
        // System.out.println("got here!");
        // } else {
        // sardine.createDirectory(ICEDRIVE_HOST + computerName);
        // }
        //
        // list(sardine, ICEDRIVE_HOST);
    }

    private static List<MyLocalFile> getAllFilesForBackup() throws IOException {
        String path = "D:\\";
        myLocalFiles = new ArrayList<>();
        long occupiedSpace = 0L;
        try {
            FileStore fileStore = Files.getFileStore(Path.of(path));
          occupiedSpace = fileStore.getTotalSpace() - fileStore.getUsableSpace();
        } catch (IOException e) {
            System.out.println("error querying space: " + e.toString());
        }
        try (ProgressBar pb = new ProgressBar("Search:", occupiedSpace)) {
            Files.walkFileTree(Path.of(path), new FileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isExcludedDir(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (Files.isDirectory(dir)) {
                        Files.list(dir).forEach(f -> {
                            FileChannel fileChannel;
                            try {
                                if (!Files.isDirectory(f)) {
                                    fileChannel = FileChannel.open(f);
                                    long fileSize = fileChannel.size();
                                    fileChannel.close();
                                    myLocalFiles.add(new MyLocalFile("puzon", f, fileSize));
                                    pb.stepBy(fileSize);
                                }
                            } catch (IOException e) {
                                System.out.println("Problem with one file");
                            }
                        });
                    }
                    return FileVisitResult.CONTINUE;
                }

                private boolean isExcludedDir(Path dir) throws IOException {
                    return dir.toString().contains("C:\\Windows") || dir.toString().contains("\\.")
                            || dir.toString().contains("AppData") || dir.toString().contains("Program Files")
                            || dir.toString().contains("$RECYCLE.BIN");

                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
            pb.stepTo(occupiedSpace);
        }
        
        return myLocalFiles;
    }

    private static String getFileSizeMegaBytes(File file) {
        return (double) file.length() / (1024 * 1024) + " mb";
    }

    private static void printDriveLetters() {
        // Pobierz liste dyskow twardych
        File[] paths;
        FileSystemView fsv = FileSystemView.getFileSystemView();

        // returns pathnames for files and directory
        paths = File.listRoots();

        // for each pathname in pathname array
        for (File path : paths) {
            // prints file and directory paths
            System.out.println("Drive Name: " + path);
            System.out.println("Description: " + fsv.getSystemTypeDescription(path));
        }
    }

    static void list(Sardine sardine, String url) throws IOException {
        for (DavResource davResource : sardine.list(url)) {
            String[] split = url.split("/");
            System.out.println(davResource.getHref());
            if (davResource.isDirectory() && !davResource.getName().equals("")
                    && !davResource.getName().equals(split[split.length - 1])) {
                list(sardine, url + davResource.getName());
            }
        }
    }

    private static String getComputerName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}

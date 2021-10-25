package pl.bzowski;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import javax.swing.filechooser.FileSystemView;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.google.common.net.UrlEscapers;

import org.apache.commons.io.FileUtils;

public class App {
    private static final String ICEDRIVE_HOST = "https://webdav.icedrive.io/";
    private static long reduce;
    private static List<MyLocalFile> myLocalFiles;
    private static List<DavResource> list;

    public static void main(String[] args) throws IOException {
        // Pobierz nazwę komputera
        String computerName = getComputerName();
        System.out.println(computerName);

        // pobierz dyski komputera
        printDriveLetters();

        // Pokaż zajętą przestrzeń na dysku
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

        // Wylistuj wszystkie pliki na dysku
        List<MyLocalFile> myLocalFiles = getAllFilesForBackup();

        reduce = myLocalFiles.stream().mapToLong(MyLocalFile::getSize).reduce(0L, (a, b) -> a + b);
        System.out.println("To Waży: " + reduce);

        Sardine sardine = SardineFactory.begin("x", "x");
        SardineLocalCache sardineLocalCache = new SardineLocalCache(sardine);

        // sprawdz czy jest katalog o nazwie komputera
        sardine.list(ICEDRIVE_HOST).stream().filter(davResource -> davResource.getName().equals(computerName))
                .findFirst().ifPresentOrElse((kot) -> System.out.println("Katalog o nazwie komputera obecny!"), () -> {
                    try {
                        sardine.createDirectory(ICEDRIVE_HOST + computerName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // sprawdz czy jest dysk d
        sardine.list(ICEDRIVE_HOST + computerName).stream().filter(davResource -> davResource.getName().equals("D"))
                .findFirst().ifPresentOrElse((dysk) -> System.out.println("Dysk D jest ustworzony na icedrive"), () -> {
                    try {
                        sardine.createDirectory(ICEDRIVE_HOST + computerName + "/D");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        //Pobierz drzewo plików z icedrive
        Set<String> iceDriveFiles = makeIceDriveFileTree(sardine, ICEDRIVE_HOST + computerName + "/D/");
        
        

        try (ProgressBar pb = new ProgressBar("Upload progress:", reduce)) {
            myLocalFiles.stream().sorted((a, b) -> {
                return Long.compare(a.getSize(), b.getSize());
            }).forEach(mlf -> {
                try {
                    String fileOnDrivePath = ICEDRIVE_HOST + mlf.getComputedPath();

                    if (mlf.getSize() > 0) {
                        if (!sardine.exists(fileOnDrivePath)) {
                            InputStream fis = new FileInputStream(new File(mlf.getFilePath()));
                            String fileDirectoryOnDrive = mlf.getFileDirectory();
                            Arrays.stream(fileDirectoryOnDrive.split("/")).reduce((a, b) -> {
                                String ret = a + "/" + b;
                                sardineLocalCache.createIfNotExists(ICEDRIVE_HOST + ret);
                                return ret;
                            });
                            sardine.put(fileOnDrivePath, fis);
                            pb.stepBy(mlf.getSize());
                        } else {
                            pb.stepBy(mlf.getSize());
                        }
                    } else {
                        System.out.println("File has 0 size! " + mlf.getFilePath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }
    }

    private static Set<String> makeIceDriveFileTree(Sardine sardine, String path) throws IOException {
        Set<String> iceDriveFiles = new HashSet<>();
        List<DavResource> resources = sardine.list(UrlEscapers.urlFragmentEscaper().escape(path));
        for(DavResource dr : resources) {
            if((ICEDRIVE_HOST.substring(0, ICEDRIVE_HOST.length() - 1) + dr.getPath()).equals(path)) {
                continue;
            }
            if(dr.isDirectory()) {
                Set<String> makeIceDriveFileTree = makeIceDriveFileTree(sardine, ICEDRIVE_HOST.substring(0, ICEDRIVE_HOST.length() - 1) + dr.getPath());
                iceDriveFiles.addAll(makeIceDriveFileTree);
            } else {
                iceDriveFiles.add(dr.getPath());
            }
        }
        return iceDriveFiles;
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

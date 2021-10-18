package pl.bzowski;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;


public class App {
    private static final String ICEDRIVE_HOST = "https://webdav.icedrive.io/";

    static Stream<Path> walkReadable(Path p) {
        if (Files.isReadable(p)) {
            if (Files.isDirectory(p)) {
                if(p.toString().contains("Application Support")) {
                    
                }
                try {
                    return Stream.concat(Stream.of(p), Files.walk(p));
                } catch (IOException | UncheckedIOException ioe) {
                    return Stream.of(p);
                }
            } else {
                return Stream.of(p);
            }
        }
        return Stream.of(p);
    }


    public static void main(String[] args) throws IOException {
        //Pobierz nazwę komputera
        String computerName = getComputerName();
        System.out.println(computerName);


        //Pobierz listę katalogów na komputerze
        Set<String> fileList = new HashSet<>();

        System.out.println("List directory: " + "/");
        walkReadable(Paths.get("/users/UP75IR"))
                .flatMap(App::walkReadable)
                .filter(Files::isExecutable)
                .forEach(System.out::println);

//        Sardine sardine = SardineFactory.begin(args[0], args[1]);

//        sardine.list(ICEDRIVE_HOST)
//                .stream()
//                .filter(davResource -> davResource.getName().equals(computerName))
//                .findFirst()
//                .ifPresent(davResource -> System.out.println(davResource.getName()));


//        if (sardine.exists(ICEDRIVE_HOST + computerName)) {
//            System.out.println("got here!");
//        } else {
//            sardine.createDirectory(ICEDRIVE_HOST + computerName);
//        }
//
//        list(sardine, ICEDRIVE_HOST);
    }

    static void list(Sardine sardine, String url) throws IOException {
        for (DavResource davResource : sardine.list(url)) {
            String[] split = url.split("/");
            System.out.println(davResource.getHref());
            if (davResource.isDirectory() && !davResource.getName().equals("") && !davResource.getName().equals(split[split.length - 1])) {
                list(sardine, url + davResource.getName());
            }
        }
    }

    private static String getComputerName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}

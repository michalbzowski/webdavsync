package pl.bzowski;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class App {
    private static final String ICEDRIVE_HOST = "https://webdav.icedrive.io/";

    public static void main(String[] args) throws IOException {
        String computerName = getComputerName();
        System.out.println(computerName);
        Sardine sardine = SardineFactory.begin(args[0], args[1]);

//        sardine.list(ICEDRIVE_HOST)
//                .stream()
//                .filter(davResource -> davResource.getName().equals(computerName))
//                .findFirst()
//                .ifPresent(davResource -> System.out.println(davResource.getName()));


        if (sardine.exists(ICEDRIVE_HOST + computerName)) {
            System.out.println("got here!");
        } else {
            sardine.createDirectory(ICEDRIVE_HOST + computerName);
        }

        list(sardine, ICEDRIVE_HOST);
    }

    static void list(Sardine sardine, String url) throws IOException {
        for (DavResource davResource : sardine.list(url)) {
            String[] split = url.split("/");
            System.out.println(davResource.getHref());
            if (davResource.isDirectory() && !davResource.getName().equals("") && !davResource.getName().equals(split[split.length-1])) {
                list(sardine, url + davResource.getName());
            }
        }
    }

    private static String getComputerName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}

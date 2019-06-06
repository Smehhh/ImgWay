import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Loader {
    public static void main(String[] args) throws URISyntaxException {
        System.out.println("Введите URL");
        Scanner scanner = new Scanner(System.in);
        String base = scanner.nextLine().trim();
        System.out.println("Введите папку назначения");
        String end = scanner.nextLine().trim();

        URI url = null;
        try {
            url = new URI(base);
        } catch (URISyntaxException e) {
            System.out.println("Невозможно распознать URL");
            return;
        }
        File endFile = new File(end);
        if (!endFile.isDirectory() && (endFile.exists() || !endFile.mkdirs())) {
            System.out.println("Невозможно создать папку назначения");
            return;
        }

        try {
            Document document = Jsoup.connect(url.toString()).get();
            List<String> imgs = getImgFrom(document);
            List<URI> uris = resolveUri(url, imgs);

            for (int i = 0; i < uris.size(); i++) {
                URI imageUrl = uris.get(i);
                if (imageUrl.getQuery() != null) {
                    System.out.println("Активный контент пропущен");
                    uris.remove(i);
                    continue;
                }
                String path = imageUrl.getPath();
                if (path.isEmpty() || path.charAt(path.length() - 1) == '/') {
                    System.out.println("Имя изображения не найдено");
                    uris.remove(i);
                    continue;
                }
                i++;
            }
            for (URI url1 : uris) {
                URL imageUrl = url1.toURL();
                String path = imageUrl.getPath();
                path = path.replace("/", File.separator);
                int port = imageUrl.getPort();
                if (port < 0) {
                    port = imageUrl.getDefaultPort();
                }
                String serv = imageUrl.getProtocol() + "_" + imageUrl.getAuthority() + "_" + port;
                File image = new File(endFile, serv + path);

                File parent = image.getParentFile();
                if (!parent.isDirectory() && (parent.exists() || !parent.mkdirs())) {
                    System.out.println("Невозможно создать папку " + parent);
                    continue;
                }
                try (InputStream inputStream = imageUrl.openStream()) {
                    Files.copy(inputStream, image.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Файл загружен " + imageUrl);
                } catch (IOException e) {
                    System.out.println("Невозможно загрузить файл " + imageUrl);
                }
            }
        } catch (IOException e) {
            System.out.println("Невозможно загрузить страницу с этим URL");
            return;
        }
    }

    private static List<URI> resolveUri(URI url, List<String> imgs) throws URISyntaxException {
        List<URI> result = new ArrayList<>();
        for (int i = 0; i < imgs.size(); i++) {
            result.add(url.resolve(imgs.get(i)));
        }
        return result;
    }

    private static List<String> getImgFrom(Document document) {
        Elements elements = document.select("img");
        List<String> imgs = new ArrayList<>();
        for (Element element : elements) {
            imgs.add(element.attr("src"));
        }
        return imgs;
    }
}

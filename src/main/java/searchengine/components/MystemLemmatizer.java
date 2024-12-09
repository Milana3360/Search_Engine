package searchengine.components;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MystemLemmatizer {

    public static String lemmatize(String text) {
        StringBuilder result = new StringBuilder();

        try {
            byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);

            Process process = new ProcessBuilder("src/main/resources/tools/mystem.exe", "-n", "-e", "utf-8")
                    .redirectErrorStream(true)
                    .start();

            process.getOutputStream().write(utf8Bytes);
            process.getOutputStream().close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
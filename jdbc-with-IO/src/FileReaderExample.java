import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileReaderExample {
    public static void main(String[] args) {
        try{
            Path path = Paths.get("src/students.txt");
            Path path2 = Paths.get("src/laptop.txt");
            List<String> lines = Files.readAllLines(path2);
            for (String line : lines){
                System.out.println(line);
            }
        } catch (RuntimeException e){
            System.out.println(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

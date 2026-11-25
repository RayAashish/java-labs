package oops.c05_polymorphism.dynamic_polymorphism;

import java.io.FileNotFoundException;
import java.io.IOException;

public class NarrowerExceptionHandling {
    public static void main(String[] args) {
        
    }
}


class Books{
    void bookType() throws IOException{
        System.out.println("Generic Book");
    }
}

class ScifiBooks extends Books{
    void bookType() throws FileNotFoundException{
        System.out.println("sci-fi books");
    }
}

class NovelBooks extends Books{
    void bookType(){
        System.out.println("Novel books");
    }
}

// class HistoryBooks extends Books{
//     void bookType() throws Exception{ //Compile time error as can't widen it, just narrow it.
//         System.out.println("Histroy books");
//     }
// }
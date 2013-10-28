package LocalTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class fileReadTest {
  public static void main(String[] args) throws IOException {
    
    String line = new String();
    BufferedReader br = new BufferedReader(new FileReader("dataset/C101.txt"));
    
    for(int i = 0; i < 5; i++)
      line = br.readLine();
    
    String[] tokens = line.split("\\s+");
    
    for(int i = 0; i < tokens.length; i++)
      System.out.print(tokens[i] + " / ");
    
    for(int i = 0; i < 4; i++)
      line = br.readLine();
    
    while((line = br.readLine())!=null) {
      tokens = line.split("\\s+");
      System.out.println("");
      for(int i = 0; i < tokens.length; i++)
        System.out.print(tokens[i] + " / ");
    }
    
    
  }
}

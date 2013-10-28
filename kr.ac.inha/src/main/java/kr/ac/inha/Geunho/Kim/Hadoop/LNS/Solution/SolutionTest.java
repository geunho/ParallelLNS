package kr.ac.inha.Geunho.Kim.Hadoop.LNS.Solution;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

import kr.ac.inha.Geunho.Kim.Hadoop.LNS.RouteWritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.SequenceFile;

import be.mschyns.www.route;

/**
 * @update 2013. 6. 13
 * @author Geunho Kim
 * 
 * program to test the result of Hadoop MapReduce job.
 */
public class SolutionTest {
  public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
    // parameters for input path
    String datasetName = "R108";
    String population = "2000";
    String iteration = "5";
    
    String fileName = datasetName + ".txt";
    System.out.println("Dataset: " + fileName);
    
    LocalTestParamsVRP status = new LocalTestParamsVRP();
    status.initParams("dataset/" + fileName);
    
    Path path = new Path("output/" + datasetName 
        + "-population" + population + "-iter" + iteration + ".dat");
    
    Configuration config = new Configuration();
    SequenceFile.Reader sfr = new SequenceFile.Reader(
        FileSystem.getLocal(config), path, config);
    
    DoubleWritable key = (DoubleWritable) sfr.getKeyClass().newInstance();
    RouteWritable value = (RouteWritable) sfr.getValueClass().newInstance();
    
    // check whole list of solutions
     /* while (sfr.next(key, value)) {
      System.out.println(key.get());
      System.out.println(value.toString());
    } // */
    
    
    // check the best solution and print the routes in graphic interface /*
    sfr.next(key, value);
    
    System.out.println("\nSolution");
    System.out.println("Total Cost: " + key.get());
    System.out.println(value.toString());
      
    ArrayList<route> alr = new ArrayList<route>();
    alr = value.getRoutes();
    
    setPath(value.getRoutes(), status);
    calcCost(value.getRoutes(), status);
    
    for(int i = 1; i <= 25; i++) {
      if(status.b[i] < status.wval[i])
        System.out.println("constraint violate: " + i);
    }
    
    // display the solution in windows
    JFrame frameMap = new JFrame("Map - AC VRP TW - M.Schyns");
    frameMap.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frameMap.setSize(1400, 800);
    displayMap panel = new displayMap(status, value.getRoutes());
    frameMap.setContentPane(panel);
    frameMap.setVisible(true);
    // */
  }
  
  
  private static void setPath(ArrayList<route> alr, LocalTestParamsVRP status) {
    double earliest;
    status.wval[0] = status.a[0];
    int i, j, prevcity, city;
    for (i = 0; i < alr.size(); i++) {
      if (alr.get(i).getQ() >= 0) {
        prevcity = 0;
        for (j = 1; j < alr.get(i).getpath().size(); j++) {
          city = alr.get(i).getpath().get(j);
          earliest = status.wval[prevcity] + status.s[prevcity]
              + status.ttime[prevcity][city];
          if (earliest < status.a[city])
            earliest = status.a[city];
          status.wval[city] = earliest;
          prevcity = city;
        }
      }
    }
  }
  
  private static void calcCost(ArrayList<route> alr, LocalTestParamsVRP status) {
    for (route r : alr) {
      double cost = 0.0;
      int prevcity = 0;
      for (int i = 1; i < r.getpath().size(); i++) {
        int city = r.getpath().get(i);
        cost += status.dist[prevcity][city];
        prevcity = city;
      }

      r.setcost(cost);
    }
  }
}

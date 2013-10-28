package kr.ac.inha.Geunho.Kim.Hadoop.LNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.ByteRangeInputStream.URLOpener;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;

import com.google.common.io.Closeables;

import be.mschyns.www.branchandbound;
import be.mschyns.www.paramsVRP;
import be.mschyns.www.route;

/**
 * @update 2013. 6. 13
 * @author Geunho Kim
 * 
 *  Parallel Large Neighborhood Search
 * Main class extends AbstractJob class from Apache Mahout 
 */
public class ParallelLNSJob extends AbstractJob {
  
  static final String NUM_ITERATIONS = ParallelLNSJob.class.getName() + ".numIterations";
  static final String NUM_POPULATION = ParallelLNSJob.class.getName() + ".numPopulation";
  static final String DESTROY_RATE = ParallelLNSJob.class.getName() + ".destroyRate";
  static final String INPUT_PATH = ParallelLNSJob.class.getName() + ".input";

  // global variables for solutions
  public static paramsVRP userParam;
  public static ArrayList<route> routes;
  
  private String input; // hdfs input path
  private int numIterations;
  private static int numPopulation;
  private static float destroyRate; // 0 < destroyRate < 1
  
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new ParallelLNSJob(), args);
  }
  
  
  public int run(String[] args) throws Exception {
    
    addOutputOption();
    addOption("input", null, "input file path", true);
    addOption("numIterations", null, "number of iterations", true);
    addOption("numPopulation", null, "number of population", true);
    addOption("destroyRate", null, "rate of destroy", String.valueOf(0.20));
    
    Map<String,List<String>> parsedArgs = parseArguments(args);
    if (parsedArgs == null)
      return -1;
    
    input = getOption("input");
    numIterations = Integer.parseInt(getOption("numIterations"));
    numPopulation = Integer.parseInt(getOption("numPopulation"));
    destroyRate = Float.parseFloat(getOption("destroyRate"));
    
    generateSolution(input);
            
    for(int i = 0; i < numIterations; i++) {
      Job mapReduce = prepareJob(outputPath(i-1), outputPath(i),
          SequenceFileInputFormat.class, LargeNeighborhoodSearchMapper.class, DoubleWritable.class, 
          RouteWritable.class, LargeNeighborhoodSearchReducer.class, DoubleWritable.class,
          RouteWritable.class, SequenceFileOutputFormat.class);
      mapReduce.setCombinerClass(LargeNeighborhoodSearchReducer.class);
      Configuration mapReduceConf = mapReduce.getConfiguration();
      mapReduceConf.setInt(NUM_ITERATIONS, numIterations);
      mapReduceConf.setInt(NUM_POPULATION, numPopulation);
      mapReduceConf.setFloat(DESTROY_RATE, destroyRate);  
      mapReduceConf.set(INPUT_PATH, input);  

      boolean succeeded = mapReduce.waitForCompletion(true);
      if (!succeeded) {
        throw new IllegalStateException("Job failed!");
      }
      
      
    }
    
      
    return 0;
  }

  // modify BPACVRPTW class from M. Schyns VRPTW system 
  private void generateSolution(String inputPath) throws IOException {
    userParam = new paramsVRP();
    userParam.initParams(inputPath);
    
    branchandbound BB = new branchandbound();
    ArrayList<route> locroutes = new ArrayList<route>();
    routes = new ArrayList<route>();
    
    if(!BB.BBnode(userParam, locroutes, null, routes, 0))
      System.out.println("Failed to generate initial solution.");
    
    // destroy duplicated nodes
    int[] checkNode = new int[26];
    for(int i = 1; i <= 25; i++)
      checkNode[i] = 1;
    
    for(int i = 0; i < routes.size(); i++) {
      route rt = routes.get(i);
      for(int j = 1; j < rt.path.size() -1; j++) {
        int nodeNum = rt.path.get(j);
        if(checkNode[nodeNum] == 1)
          checkNode[nodeNum]--;
        else {
          rt.removeCity(nodeNum);      
          j--;
        }
      }
    }
    
    
    FileSystem fs = FileSystem.get(getOutputPath("output--1").toUri(), getConf());
    SequenceFile.Writer writer = null;
    
    try {
      writer = new SequenceFile.Writer(fs, getConf(), new Path(getOutputPath("output--1"), "part-r-00000"), 
          DoubleWritable.class, RouteWritable.class);

      RouteWritable rw = new RouteWritable(routes);
      System.out.println("routes:\n" + rw.toString());
      writer.append(new DoubleWritable(Double.MAX_VALUE), rw);
    } finally {
      Closeables.closeQuietly(writer);
    }
      
  }
  
  public static class LargeNeighborhoodSearchMapper 
      extends Mapper<DoubleWritable, RouteWritable, DoubleWritable, RouteWritable> {
    
    paramsVRP status;
    
    int pop_count;
    int numPop;
    float destRate;
    String input;
    
    ArrayList<route> r_list;
    ArrayList<Integer> destroyList = new ArrayList<Integer>();
    
    // Stacks for tracking the search
    Stack<Integer> s_node = new Stack<Integer>();
    Stack<Integer> s_route = new Stack<Integer>();
    Stack<Integer> s_pos = new Stack<Integer>();
    
    @Override
    protected void setup(Mapper.Context ctx) throws IOException, InterruptedException {
      input = ctx.getConfiguration().get(INPUT_PATH);
      destRate = Float.parseFloat(ctx.getConfiguration().get(DESTROY_RATE));
      numPop = Integer.parseInt(ctx.getConfiguration().get(NUM_POPULATION));
      status = new paramsVRP();
      status.initParams(input);
      r_list = new ArrayList<route>();
      pop_count = 0;
    }
    
    @Override
    protected void map(DoubleWritable dw_cost, RouteWritable rw, Context ctx) 
        throws IOException, InterruptedException {

      while(numPop != pop_count) {
        r_list = rw.getRoutes();        
        setPath(r_list, status);
        calcCost(r_list, status);
        
        // remove violate node and add to destroy list.
        for(int i = 1; i <= 25; i++) {
          if(status.b[i] < status.wval[i]) {
            for(route rt : r_list) {
              rt.removeCity(i);
            }
            destroyList.add(i);
          }         
        }
        
        destroy(r_list, destRate);
        setPath(r_list, status);
        calcCost(r_list, status);
        
        repair(0, 0, status);
        setPath(r_list, status);
        calcCost(r_list, status);

        pop_count++;
        
        double cost = 0.0;
        for(route r : r_list)
          cost += r.cost;
        if(cost <= dw_cost.get())
          ctx.write(new DoubleWritable(cost), new RouteWritable(r_list));
      }
    }
    
    private void destroy(ArrayList<route> list, float destroyRate) {
      int totalPathSize = 0;
      for(route route : r_list)
        totalPathSize += route.path.size() - 2;
      
      int numDestroy = (int) (totalPathSize * destroyRate);
      destroyList = randomNumber(totalPathSize, numDestroy);
   
      for(int i = 0; i < list.size(); i++)
        for(int j = 0; j < destroyList.size(); j++)
          list.get(i).removeCity(destroyList.get(j));
    }
    
    private ArrayList<Integer> randomNumber(int numCustomer, int numDestroy) {
      LinkedList<Integer> list = new LinkedList<Integer>();
      for (int i = 1; i <= numCustomer; i++)
        list.addLast(i);

      Random random = new Random();
      ArrayList<Integer> d_list = new ArrayList<Integer>(numDestroy);
      
      for(int i = 0; i < numDestroy; i++) {
        int index = random.nextInt(list.size());
        d_list.add(list.get(index));
        list.remove(index);
      }
      
      return d_list;
    }
    
    /*
     * @param backtracking point: number of route list, position of the route
     * @return  
     */
    private void repair(int routeNum, int pos, paramsVRP paramStatus) {
      int r, p;
      int i_node;
      double total_dist = 0.0;
      double lowest_dist = Double.MAX_VALUE;
      boolean isSolution = true;
      paramsVRP status = paramStatus;
      
      setPath(r_list,status);
      calcCost(r_list, status);
      
      while(!destroyList.isEmpty()) {
        i_node = destroyList.get(0);
        
        for(r = routeNum; r < r_list.size(); r++) {
          route route = new route();
          route = r_list.get(r);
                   
          for(p = pos; p < route.getpath().size() - 1; p++) {
            isSolution = true;
            /*
             * forward node: f_node
             * backward node: b_node
             * insert node: i_node
             * position: [f_node]->[i_node]->[b_node]
             */
            Integer f_node = route.getpath().get(p);
            Integer b_node = route.getpath().get(p + 1);
            
            /*
             * Constraints check: 
             *   1. capacity
             *   2. time windows
             */
            double r_cost = route.cost;
            double d_fToi = status.distBase[f_node][i_node];
            double d_iTob = status.distBase[i_node][b_node];
            double s_i = status.s[i_node];
            double w_f = status.wval[f_node];
            double s_f = status.s[f_node];
            double w_i = w_f + s_f + d_fToi;
            
            if(w_f + s_f + d_fToi < status.a[i_node])
              w_i = status.a[i_node];
            
            if(((r_cost + s_i) <= status.capacity) 
                && (w_f + s_f + d_fToi <= status.b[i_node])
                && (w_i + s_i + d_iTob <= status.b[b_node])) {
              
              route.addcity(f_node, i_node);
              setPath(r_list,status);
              calcCost(r_list, status);
              
              /*
               *  check if insertion violates rules or not.
               * (because above checking is not complete)
               */
              for(int i = p; i < route.path.size(); i++) {
                int c_num = route.path.get(i);
                if(status.wval[c_num] > status.b[c_num]) {                  
                  isSolution = false;
                }
              }
              
              if(!isSolution) {
                route.removeCity(i_node);
                setPath(r_list,status);
                calcCost(r_list, status);
                continue;
              }

              for(route dist : r_list)
                total_dist += dist.getcost();
              if(total_dist < lowest_dist) {
                lowest_dist = total_dist;
                s_node.push(i_node);
                s_route.push(r);
                s_pos.push(p);
              }
              total_dist = 0;
              route.removeCity(i_node);
            }
          } // position For statement
          p = 0;
        } // route For statement
        r = 0;
        
        if(isSolution) {
          // choose the lowest path
          r_list.get(s_route.peek()).addcity(r_list.get(s_route.peek()).getpath().get(s_pos.peek()), s_node.peek());
          destroyList.remove(0);
          setPath(r_list,status);
          calcCost(r_list, status);
          lowest_dist = Double.MAX_VALUE;
          // remove empty routes
          /* for(int z = 0; z < r_list.size(); z++) {
            route rt = r_list.get(z);
            if(rt.path.size() == 2)
              r_list.remove(z);
          } */
            
              
        } else {
          r_list.get(s_route.peek()).removeCity(s_node.peek());
          destroyList.add(0, s_node.pop());
          setPath(r_list,status);
          calcCost(r_list, status);
          
          // backtracking
          repair(s_route.pop(), s_pos.pop() + 1, status);
          break;
        }
      }
    }
    
    private void setPath(ArrayList<route> alr, paramsVRP status) {
      // ////////////////////////////////////////////////
      // Tune and display the solution
      // compute the service starting time for each city
      
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
    
    private void calcCost(ArrayList<route> alr, paramsVRP status) {
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
    
  } // end of mapper
  
  public static class LargeNeighborhoodSearchReducer 
      extends Reducer<DoubleWritable, RouteWritable, DoubleWritable, RouteWritable> {

    int numPop;
    double minimum = Double.MAX_VALUE;
    ArrayList<route> bestSolution;
    
    @Override
    protected void setup(Reducer.Context ctx) throws IOException, InterruptedException {
      numPop = Integer.parseInt(ctx.getConfiguration().get(NUM_POPULATION));
      bestSolution = new ArrayList<route>();
    }
    
    protected void reduce(DoubleWritable dw_cost, RouteWritable rw, Context ctx) throws IOException, InterruptedException {
      
      if (dw_cost.get() < minimum) {
        bestSolution.clear();
        try {
          // deep copy of routes list
          for (int i = 0; i < rw.getRoutes().size(); i++) {
            route r = rw.getRoutes().get(i).clone();
            bestSolution.add(r);
          }
        } catch (CloneNotSupportedException cnse) {
        }
        minimum = dw_cost.get();
      
        ctx.write(new DoubleWritable(minimum), new RouteWritable(bestSolution));
      }

    }
  } // end of reducer
  
  private Path outputPath(int iteration) {
    return getOutputPath("output-" + iteration);
  }
    
  
}

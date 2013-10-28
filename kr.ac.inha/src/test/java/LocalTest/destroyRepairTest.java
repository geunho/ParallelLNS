package LocalTest;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import be.mschyns.www.route;

/**
 * @update 2013. 6. 13
 * @author Geunho Kim
 *
 * Test class for destroy and repair function
 *  To run this program, external libaray setting should be changed to
 * Window version of cplex.
 */
public class destroyRepairTest {
  static ArrayList<route> routes = new ArrayList<route>();
  static paramsVRP userParam = new paramsVRP();
  static ArrayList<Integer> destroyList = new ArrayList<Integer>();
  // Stacks for tracking the search
  static Stack<Integer> s_node = new Stack<Integer>();
  static Stack<Integer> s_route = new Stack<Integer>();
  static Stack<Integer> s_pos = new Stack<Integer>();
  
  public static void main(String[] args) throws IOException {
    userParam.initParams("dataset/C101.txt");

    branchandbound BB = new branchandbound();
    ArrayList<route> locroutes = new ArrayList<route>();

    BB.BBnode(userParam, locroutes, null, routes, 0);
    
    destroy(0.30);
    setPath();
    calcCost();

    System.out.println("destroyList.size() = " + destroyList.size());
    
    //_repair(30);
    backtrackSearch(0, 0);
    calcCost();
    setPath();
    
    // display the routes in the console
    DecimalFormat df = new DecimalFormat("#0.00");
    for (int i = 0; i < routes.size(); i++) {
      if (routes.get(i).getQ() > 1e-6) {
        System.out.print(df.format(routes.get(i).getQ()) + " route "
            + (i + 1) + " (" + df.format(routes.get(i).getcost()) + "): 0");
        for (int j = 1; j < routes.get(i).getpath().size(); j++)
          System.out.print("->" + routes.get(i).getpath().get(j));
        System.out.println();
      }
    }
    

    
  }
  
  static private void destroy(double destroyRate) {
    System.out.println("\n\n:::::::::::::::::::::Test destroy function:::::::::::::::::::::::");
    int totalPathSize = 0;
    for(route route : routes)
      totalPathSize += route.path.size() - 2;
    System.out.println("totalPathSize: " + totalPathSize);
    
    int numDestroy = (int) (totalPathSize * destroyRate);
    destroyList = randomNumber(totalPathSize, numDestroy);
    
    System.out.println("Destroy List");
    for(int i = 0; i < destroyList.size(); i++)
      System.out.print(destroyList.get(i) + " / ");
    
    ArrayList<route> list = routes;
    
    System.out.println("\n\nPath before destroy");
    for(route route : list)
      System.out.println("route: " + route.path.toString());
    
    for(int i = 0; i < list.size(); i++)
      for(int j = 0; j < destroyList.size(); j++)
        list.get(i).removeCity(destroyList.get(j));
    
    System.out.println("\nPath after destroy");
    for(route route : list)
      System.out.println("route: " + route.path.toString());
    
  }
  
  static private ArrayList<Integer> randomNumber(int numCustomer, int numDestroy) {
    LinkedList<Integer> list = new LinkedList<Integer>();
    for (int i = 1; i <= numCustomer; i++)
      list.addLast(i);

    Random random = new Random();
    ArrayList<Integer> d_list = new ArrayList<Integer>(numDestroy);
    for(int i = 0; i < numDestroy; i++) {
      System.out.print(">");
      int index = random.nextInt(list.size()); // 현재 list 길이 만큼의 숫자에서 랜덤 숫자 뽑아냄
      d_list.add(list.get(index)); // 랜덤 값이 나온 값을 리스트에서 뽑아냄
      list.remove(index);
    }
    
    return d_list;
  }
  
  
  /*
   * iteration is meaningless. 
   */
  static private void _repair(int iterations) {
    double cost = 0.0;
    for(route r : routes)
      cost += r.cost;
    // backtrack Searching
    for(int i = 0; i < iterations; i++)
      backtrackSearch(0, 0);
  }
  
  /*
   * @param backtracking point: number of route list, position of the route
   * @return 
   * 
   * start searching with parameters and 
   */
  static private void backtrackSearch(int routeNum, int pos) {
    int r, p;
    int d_count = 0;
    int i_node;
    double total_dist = 0.0;
    double lowest_dist = Double.MAX_VALUE;
    boolean isInserted = false;
    boolean isSolution = true;
    while(!destroyList.isEmpty()) {
      i_node = destroyList.get(0);
      for(r = routeNum; r < routes.size(); r++) {
        route route = routes.get(r);
        for(p = pos; p < route.getpath().size() - 1; p++) {
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
          double d_fToi = userParam.distBase[f_node][i_node];
          double d_iTob = userParam.distBase[i_node][b_node];
          double s_i = userParam.s[i_node];
          double w_f = userParam.wval[f_node];
          double s_f = userParam.s[f_node];
          double w_i = w_f + s_f + d_fToi;
          
          if(w_f + s_f + d_fToi < userParam.a[i_node])
            w_i = userParam.a[i_node];           
          if(((r_cost + s_i) <= userParam.capacity) 
              && (w_f + s_f + d_fToi <= userParam.b[i_node])
              && (w_i + s_i + d_iTob <= userParam.b[b_node])) {
            
            route.addcity(f_node, i_node);
            // destroyList.remove(0);
            isInserted = true;
            // store states
            // s_node.push(i_node);
            // s_route.push(r);
            // s_pos.push(p);
            System.out.println("set path: " + i_node);
            setPath();
            calcCost();
            
            for(int i = 1; i <= userParam.nbclients; i++) {
              if( !(userParam.wval[i] <= userParam.b[i]))
                isSolution = false;
            }
            
            if(!isSolution) {
              route.removeCity(i_node);
              setPath();
              calcCost();
              continue;
            }

            
            for(route dist : routes)
              total_dist += dist.getcost();
            if(total_dist < lowest_dist) {
              lowest_dist = total_dist;
              s_node.push(i_node);
              s_route.push(r);
              s_pos.push(f_node);
              // destroyList.remove(0);
              // d_count++;
            }
            total_dist = 0;
            route.removeCity(i_node);
            // break;
          } else {
            isInserted = false;
          }
          /* if(isInserted)
            break; */
        } // position For statement
        p = 0;
        /*if(isInserted)
          break;*/
      } // route For statement
      r = 0;
      if(isSolution) {
        routes.get(s_route.peek()).addcity(routes.get(s_route.peek()).getpath().get(s_pos.peek()), s_node.peek());
        destroyList.remove(0);
        lowest_dist = Double.MAX_VALUE;
      } else {
        destroyList.add(0, s_node.pop());
        backtrackSearch(s_route.pop(), s_pos.pop() + 1);
      }
      /*
      if(!isInserted) {
        // back track and search again
        System.out.println("back track: " + s_node.peek());
        destroyList.add(s_node.peek());
        d_count--;
        routes.get(s_route.peek()).removeCity(s_node.pop());
        setPath();
        calcCost();
        backtrackSearch(s_route.pop(), s_pos.pop());
      }
      */
    }           
  }
  
  // TODO: modify it, Spaghetti source
  static private void repair() {
    // tracking values
    LinkedList countList = new LinkedList();
    LinkedList customerList = new LinkedList();
    
    ArrayList<route> list = new ArrayList<route>(); 
    list.addAll(routes);
    int count = 0;
    boolean check = false;
    ArrayList<Integer> d_list = new ArrayList<Integer>();
    
    d_list.addAll(destroyList);
    int originalSize = d_list.size();
    //while(!d_list.isEmpty()) {
      if(d_list.size() != originalSize) {
        // restore the destroy list
        System.out.println("\n갱신");
        d_list.removeAll(d_list);
        d_list.addAll(destroyList);
        for(int i = 0; i < countList.size(); i++)
          routes.get((Integer)countList.get(i)).removeCity((Integer)customerList.get(i));
        
      }
      Collections.shuffle(d_list);
      int d_count = 0;
      for(int i = 0; i <= destroyList.size() - d_count +1; i++) {
        check = false;
        int customer = d_list.get(i - d_count);
        System.out.print("//customer = " + customer + "//i = " + i + "//");
        Collections.shuffle(routes);
        for(route route : routes) {
         // if(route.getpath().size() >= 2) {
            for(int j = 0; j < route.getpath().size() - 1; j++) {
              //System.out.println("route cost = " + cost);
              Integer f_node = route.getpath().get(j);
              Integer a_node = route.getpath().get(j+1);
             
              /*
               * Constraints check: 
               *   1. capacity
               *   2. time windows
               */
              if(((route.cost + userParam.distBase[f_node][customer] + userParam.distBase[customer][a_node] + userParam.s[customer]) <= userParam.capacity) 
                  // && (userParam.a[customer] <= userParam.wval[f_node] + userParam.s[f_node] + userParam.distBase[f_node][customer])
                  && (userParam.wval[f_node] + userParam.s[f_node] + userParam.distBase[f_node][customer] <= userParam.b[customer])
                  // && (userParam.a[a_node] <= userParam.wval[f_node] + userParam.s[f_node] + userParam.distBase[f_node][customer] + userParam.distBase[customer][a_node] + userParam.s[customer])
                  && (userParam.wval[f_node] + userParam.s[f_node] + userParam.distBase[f_node][customer] + userParam.distBase[customer][a_node] + userParam.s[customer] <= userParam.b[a_node])) {
                //System.out.print(d_list.get(i));
                routes.get(count).addcity(f_node, customer);
                // store states
                countList.add(count);
                customerList.add(customer);
                System.out.println("insert the node " + customer);
                d_list.remove(i - d_count);
                d_count++;
                setPath();
                
                check = true;
                break;
              }
              if(check)
                break;
            
          }
          count++;
        }
        count = 0;
      //}
    }
    
    System.out.println("\nPath after repair");
    for(route route : routes)
      System.out.println("route: " + route.path.toString());
    //System.out.print(".");
  }
  
  
  // TODO: parameterize the paramVRP and ArrayList<route>
  static private void setPath() {
    // /////////////////////////////////////////////////////////////////////////
    // Tune and display the solution
    // compute the service starting time for each city
    
    double earliest;
    userParam.wval[0] = userParam.a[0];
    int i, j, prevcity, city;
    for (i = 0; i < routes.size(); i++) {
      if (routes.get(i).getQ() > 1e-6) {
        prevcity = 0;
        for (j = 1; j < routes.get(i).getpath().size(); j++) {
          city = routes.get(i).getpath().get(j);
          earliest = userParam.wval[prevcity] + userParam.s[prevcity]
              + userParam.ttime[prevcity][city];
          if (earliest < userParam.a[city])
            earliest = userParam.a[city];
          userParam.wval[city] = earliest;
          prevcity = city;
        }
      }
    }
  }
  
  // TODO: parameterize the paramVRP and ArrayList<route>
  static private void calcCost() {
    for (route r : routes) {
      double cost = 0.0;
      int prevcity = 0;
      for (int i = 1; i < r.getpath().size(); i++) {
        int city = r.getpath().get(i);
        cost += userParam.dist[prevcity][city];
        prevcity = city;
      }

      r.setcost(cost);
    }
  }

}

package LocalTest;

import java.util.ArrayList;

import be.mschyns.www.route;

public class routeCloneableTest {
  public static void main(String[] args) throws CloneNotSupportedException {
    ArrayList<route> routes = new ArrayList<route>();
    ArrayList<route> _routes = new ArrayList<route>();
    
    route r1 = new route();
    r1.addcity(3);
    r1.addcity(7);
    r1.addcity(23);
    
    routes.add(r1);
    
    // deep copy
    for(int i = 0; i < routes.size(); i++) {
      route r = routes.get(i).clone();
      _routes.add(r);
    }
      
      
    _routes.get(0).addcity(9);
    _routes.get(0).addcity(12);

    System.out.println("routes: " + routes.get(0).path.toString());
    System.out.println("_routes: " + _routes.get(0).path.toString());
  }
}

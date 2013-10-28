package kr.ac.inha.Geunho.Kim.Hadoop.LNS;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Writable;
import org.apache.mahout.math.Varint;
import org.apache.mahout.math.VectorWritable;

import be.mschyns.www.route;

/**
 * @update 2013. 6. 10
 * @author Geunho Kim
 *
 * Writable class for route list
 */
public class RouteWritable implements Writable {
  ArrayList<route> routes;

  
  public RouteWritable() {
  }
  
  public RouteWritable(ArrayList<route> list) {
    setRoutes(list);
  }
  
  public ArrayList<route> getRoutes() {
    ArrayList<route> new_route = new ArrayList<route>();
    
    try {
      for (int i = 0; i < routes.size(); i++) {
        route r;
        r = routes.get(i).clone();
        new_route.add(r);
      }
    } catch (CloneNotSupportedException e) {}
    
    return new_route;
  }
  
  public void setRoutes(ArrayList<route> list) {
    this.routes = new ArrayList<route>();
    for(int i = 0; i < list.size(); i++) {
      route r;
      try {
        r = list.get(i).clone();
        this.routes.add(r);
      } catch (CloneNotSupportedException e) {}
    }
  }

  public void readFields(DataInput in) throws IOException {
    int size = in.readInt();
    ArrayList<route> tmp = new ArrayList<route>(size);
    
    for(int i = 0; i < size; i++) {
      int pathSize = in.readInt();
      route r = new route();
      
      for(int j = 0; j < pathSize; j++) {
        r.addcity(in.readInt());
      }
      tmp.add(r);
    }
    
    setRoutes(tmp);
  }

  public void write(DataOutput out) throws IOException {
    out.writeInt(routes.size()); // set flag as routes' size
    for(int i = 0; i < routes.size(); i++) {
      route tmp = routes.get(i);
      out.writeInt(tmp.path.size()); // set second flag as route size
      for(int j = 0; j < tmp.path.size(); j++) {
        int node = tmp.path.get(j);
        out.writeInt(node); // write node information one by one
      }
    }
  }
  
  @Override
  public String toString() {
    String list = "";
    for(route r : routes) {
      list += r.path.toString() + "\n";
    }
    
    return list;
  }

}

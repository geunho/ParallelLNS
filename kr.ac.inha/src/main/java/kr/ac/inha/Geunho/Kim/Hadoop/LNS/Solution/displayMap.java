package kr.ac.inha.Geunho.Kim.Hadoop.LNS.Solution;

/**
 * @author mschyns
 *
 */
import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import be.mschyns.www.route;


public class displayMap extends JPanel {
	LocalTestParamsVRP 	userParam;
	double 		maxx,maxy,minx,miny,ratiox,ratioy;
	Color[]		mycolors={Color.blue,Color.red,Color.green,Color.orange,Color.cyan,Color.magenta,Color.yellow};
	ArrayList<route> routes;

	public displayMap(LocalTestParamsVRP userParam,ArrayList<route> routes) {
	
		super();
		
		int i;
		this.userParam=userParam;
		maxx=userParam.posx[0];
		maxy=userParam.posy[0];
		minx=userParam.posx[0];
		miny=userParam.posy[0];
		for (i=1;i<userParam.nbclients+1;i++) {
			if (userParam.posx[i]<minx)
				minx=userParam.posx[i];
			if (userParam.posy[i]<miny)
				miny=userParam.posy[i];
			if (userParam.posx[i]>maxx)
				maxx=userParam.posx[i];
			if (userParam.posy[i]>maxy)
				maxy=userParam.posy[i];
		}
		this.routes=routes;
	}
	
	
	public void paintComponent(Graphics g1d) {
		int i,j,vehic,lowmapy;
		
		Graphics2D g=(Graphics2D) g1d;
		g.setColor(Color.white);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(Color.black);
        ratiox=(maxx>minx)?(this.getWidth()-100)/(maxx-minx):1.0;
		ratioy=(maxy>miny)?(this.getHeight()-20)/(maxy-miny):1.0;
		lowmapy=this.getHeight()-10;
		
		int cx,cy;
		for (i=0;i<=userParam.nbclients;i++) {
			cx=10+(int) ((userParam.posx[i]-minx)*ratiox);
			cy=lowmapy-(int) ((userParam.posy[i]-miny)*ratioy);
        	g.drawOval(cx-2, cy-2, 5, 5);
        	g.drawString(userParam.citieslab[i], cx+2, cy);
        	g.drawString("["+userParam.a[i]+"|"+(int) userParam.wval[i]+"|"+userParam.b[i]+"]", cx+2, cy+10);
        }
		
		g.drawString("Time Window: [Earliest | Scheduled | Latest]", this.getWidth()-290, 20);
		int lastx,lasty,newx,newy;
		vehic=0;
		for (i=0;i<routes.size();i++) {
			if (routes.get(i).getQ()>=0) {
				g.setColor(mycolors[vehic%7]);
				lastx=10+(int) ((userParam.posx[0]-minx)*ratiox);
				lasty=lowmapy-(int) ((userParam.posy[0]-miny)*ratioy);
				for (j=0;j<routes.get(i).getpath().size();j++) { 
					newx=10+(int) ((userParam.posx[routes.get(i).getpath().get(j)]-minx)*ratiox);
					newy=lowmapy-(int) ((userParam.posy[routes.get(i).getpath().get(j)]-miny)*ratioy);
					g.drawLine(lastx,lasty,newx,newy);
					lastx=newx;
					lasty=newy;
				}
				g.drawString("Vehicle "+vehic, this.getWidth()-100, 40+vehic*14);
				vehic++;
			}
		}	
		g.setColor(Color.black);
	}

}

import java.awt.BorderLayout;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.PriorityQueue;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

//import com.teamdev.jxbrowser.chromium.Browser;
//import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class Main {
	final static int CHANGE = 3;

	public static Stop algorithm(Stop origin/*, Browser browser*/) throws CloneNotSupportedException {
		PriorityQueue<Stop> routes = new PriorityQueue<Stop>();
		HashMap<Long, Stop> recordings = new HashMap<Long, Stop>();
		
		routes.add(origin);
		Stop destination = origin.getDestination();
		
		while( recordings.get(destination.getID()) == null ) {
			Stop head = routes.poll();
			
//			showPath(browser, head);
			//System.out.println(head.toString());
			
			for(Stop nextStop: head.getNexts()) {
				if(nextStop.getChange() <= CHANGE) {
					if( recordings.get(nextStop.getID()) == null ) {
						recordings.put(nextStop.getID(), nextStop);
						routes.add(nextStop);
					}
					else if( recordings.get(nextStop.getID()).getChange() > nextStop.getChange() ){
						recordings.remove(nextStop.getID());
						recordings.put(nextStop.getID(), nextStop);
						routes.add(nextStop);
					}
				}
			}
			
		}
		
		return (Stop)recordings.get(destination.getID());
	}
	
//	public static void showPath(Browser browser, Stop s) {
//		browser.executeJavaScript("removeMarkers();");
//		while(s != null) {
//			browser.executeJavaScript("mark(" + s.getLatitude() + ", " + s.getLongitude() + ", '" + s.toString() + ", " + s.getPath() + "');");
//			s = s.getPrev();
//		}
//		return;
//	}
	
	public static void main(String[] args) throws Exception {
//        Browser browser = new Browser();
//        BrowserView view = new BrowserView(browser);
//
//        JFrame frame = new JFrame("JxBrowser");
//        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        frame.add(view, BorderLayout.CENTER);
//        //frame.setSize(500, 400);
//        frame.setLocationRelativeTo(null);
//        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
//
//        browser.loadHTML("<!DOCTYPE html> <html> <head> <style> #map { height: 625px; width: 100%; } </style> </head> <body> <div id='map'></div> <script> var map; var markers = []; function initMap() { var szeged = {lat: 46.266713, lng: 20.144236}; map = new google.maps.Map(document.getElementById('map'), { zoom: 13, center: szeged }); } function mark(lat, lng, title) { var mark = {lat: lat, lng: lng}; var marker = new google.maps.Marker({ position: mark, title: title }); marker.setMap(map); markers.push(marker); } function removeMarkers() { for(var i=0; i<markers.length; i++) { markers[i].setMap(null); } markers = []; } </script> <script async defer src='https://maps.googleapis.com/maps/api/js?key=AIzaSyCOfonnufmw9Vadj2zvJys9wxZsvUssxUc&callback=initMap'> </script> </body> </html>");
        
		//Calendar now = Calendar.getInstance();
		
		/************Algorithm test*************/
		
		Stop origin = Db.getStopByStopID(1403707829l);
		Stop destination = Db.getStopByStopID(1404296015l);
		
		Calendar cal = new GregorianCalendar(2018, Calendar.MARCH, 22, 18, 35);

		origin.setDate(cal);
		origin.setDestination(destination);
		origin.setFirst(origin);
		
		Stop result = algorithm(origin/*, browser*/);
		
//		showPath(browser, result);
		while(result != null) {
			System.out.println(result);
			System.out.println(result.getPath());
			result = result.getPrev();
		}
		
		/*********End of algorithm test*********/
		
	}

}

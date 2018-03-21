import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Db {
	private static final String connectionUrl = "jdbc:sqlite:database\\timetable.db";
	private static Connection conn;
	
	private static Connection getConnection() {
		if( conn == null) {		
			try {
				conn = DriverManager.getConnection(connectionUrl);
			}
			catch(Exception e) {
				System.err.println(e);
				System.exit(1);
			}
		}
		return conn;	
	}
	
	public static void test() {
		final Connection conn = getConnection();
		
		Statement stmt = null;
		ResultSet result;
		
		try {
			stmt = conn.createStatement();
			result = stmt.executeQuery("select * from stops");
			
			while(result.next()) {
				System.out.println( result.getString("stop_name") );
			}
		}
		catch(SQLException e) {
			System.err.println("Error: query failed!");
		}
		finally {
			try {
				stmt.close();
			}
			catch(SQLException e) {
				System.err.println(e);
			}
			
			try {
				conn.close();
			}
			catch(SQLException e) {
				System.err.println(e);
			}
		}
	}
	
	public static Stop getStopByStopID(long stopID) {
		final Connection conn = getConnection();
		Statement stmt = null;
		final String query = "SELECT * FROM stops WHERE stop_id = " + stopID;
		Stop result = null;
		
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			
			resultSet.next();
			long stopId = resultSet.getLong(1);
			String stopName = resultSet.getString(2);
			double stopLat = resultSet.getDouble(3);
			double stopLon = resultSet.getDouble(4);
			
			result = new Stop(stopId, stopName, stopLat, stopLon);
		}
		catch(SQLException e) {
			System.err.println("Error: query failed!");
		}
		finally {
			try {
				stmt.close();
			}
			catch(SQLException e) {
				System.err.println("Error: unable to close statement!");
			}
		}
		
		return result;
	}
	
	public static ArrayList<Stop> getNexts(Stop s) {
		final Connection conn = getConnection();
		Statement stmt = null;
		ArrayList<Stop> result = null;
		
		final long stopID = s.getID();
		final Calendar calendar = s.getDate();
		
		final String dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
		
		final String date = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
		final String time = new SimpleDateFormat("HH:mm").format(calendar.getTime());
		
		final String query =
                "SELECT 	stop_times.stop_id, stops.stop_name, stops.stop_lat, stops.stop_lon, " +
                        "			routes.route_short_name, sub_stop_times.departure_time, stop_times.arrival_time, trips.direction_id, stop_times.stop_sequence, routes.route_type " +
                        "FROM 		stops, " +
                        "			stop_times, " +
                        "			trips, " +
                        "			routes, " +
                        "			( SELECT trip_id, stop_sequence, departure_time FROM stop_times WHERE stop_id = " + stopID + ") AS sub_stop_times, " +
                        "           ( SELECT service_id FROM calendar WHERE '" + date + "' >= date(start_date) AND '" + date + "' <= date(end_date) AND " + dayName + " = 1 ) AS sub_calendar " +
                        "WHERE 		time(stop_times.departure_time) >= '" + time + "' " +
                        "AND		stop_times.stop_sequence = sub_stop_times.stop_sequence + 1 " +
                        "AND		stop_times.trip_id = trips.trip_id " +
                        "AND		trips.route_id = routes.route_id " +
                        "AND		stop_times.trip_id = sub_stop_times.trip_id " +
                        "AND		trips.service_id = sub_calendar.service_id " +
                        "AND		stop_times.stop_id = stops.stop_id " +
                        "ORDER BY 	sub_stop_times.departure_time";
		
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			result = getStopsFromResultSet(resultSet, s);
		}
		catch(SQLException e) {
			System.err.println("Error: query failed!");
		}
		catch(ParseException e) {
			System.err.println("Error: parsing failed!");
		}
		finally {
			try {
				stmt.close();
			}
			catch(SQLException e) {
				System.err.println("Error: unable to close statement!");
			}
		}
		
		return result;
	}
	
	public static ArrayList<Stop> getAllStops() {
		final Connection conn = getConnection();
		Statement stmt = null;
		ArrayList<Stop> result = null;
		
		final String query =
				"SELECT * FROM stops";
		
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			result = getStopsFromResultSet(resultSet);
		}
		catch(SQLException e) {
			System.err.println("Error: query failed!");
		}
		finally {
			try {
				stmt.close();
			}
			catch(SQLException e) {
				System.err.println("Error: unable to close statement!");
			}
		}
		
		return result;
	}

	
	
	private static ArrayList<Stop> getStopsFromResultSet(ResultSet rs, Stop s) throws SQLException, ParseException {
		ArrayList<Stop> result = new ArrayList<Stop>();
		final Calendar calendar = s.getDate();
		final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		
		Boolean hasPathWithCurrentService = false;
		while(rs.next()) {
			Calendar departureTime = new GregorianCalendar();
			Calendar arrivalTime = new GregorianCalendar();
			
			long stopID = rs.getLong(1);
			String stopName = rs.getString(2);
			double stopLat = rs.getDouble(3);
			double stopLon = rs.getDouble(4);
			String serviceName = rs.getString(5);
			departureTime.setTime( timeFormat.parse(rs.getString(6)) );
			arrivalTime.setTime( timeFormat.parse(rs.getString(7)) );
			int directionId = rs.getInt(8);
			int stopSequence = rs.getInt(9);
			int routeType = rs.getInt(10);
			
			departureTime.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
			arrivalTime.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
			
			//Filter earliest result by service name
			boolean ok = true;
			for(Stop stop: result) {
				if( stop.getPath().getServiceName().equals(serviceName) ) ok = false; 
			}
			
			//Force staying on route even if current service is slower
			if(!hasPathWithCurrentService 
					&& s.getPath() != null 
					&& s.getPath().getServiceName().equals(serviceName)) {
				for(Stop stop: result) {
					if( stop.getID() == stopID ) {
						result.remove(stop);
						break;
					}
				}
				hasPathWithCurrentService = true;
			}
			
			if(ok) {
				Path path = new Path(serviceName, departureTime, arrivalTime, s, routeType);
				path.setDirectionId(directionId);
				Stop stop = new Stop(stopID, stopName, stopLat, stopLon, path);
				stop.setSequence(stopSequence);
				result.add(stop);
			}
		}
		
		return result;
	}
	
	private static ArrayList<Stop> getStopsFromResultSet(ResultSet rs) throws SQLException {
		ArrayList<Stop> result = new ArrayList<Stop>();
		
		while(rs.next()) {
			long stopID = rs.getLong(1);
			String stopName = rs.getString(2);
			double stopLat = rs.getDouble(3);
			double stopLon = rs.getDouble(4);

			result.add( new Stop(stopID, stopName, stopLat, stopLon) );
		}
		
		return result;
	}
	
}

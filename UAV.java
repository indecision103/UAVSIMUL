package jzombies;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.collections15.list.TreeList;
import org.geotools.referencing.GeodeticCalculator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import jzombies.Operator;
import jzombies.Util;
import jzombies.specific_mission_node;

import javax.measure.unit.SI;

class Operation {
	private specific_mission_node my_specific_mission_node;
	
	private String mode;
	private String mission;
	private String type;
	private String start_point;
	private String connection_point;
	private String destination_point;
	private double start_time;
	private double late_time;
	private double min_altitude;
	private double max_altitude;
	
	public Operation() {
		this.mode = "";
		this.mission = "";
		this.type = "";
		this.start_point = "";
		this.connection_point = "";
		this.destination_point = "";
		this.start_time = Double.MIN_VALUE;
		this.late_time = Double.MIN_VALUE;
		this.min_altitude = Double.MIN_VALUE;
		this.max_altitude = Double.MIN_VALUE;
	}
	
	public Operation(String mode) {
		if (mode == "all_random") {
			this.mode = mode;
			this.mission = "all_random";
			this.type = "all_random";
			
			this.start_point = "";
			this.connection_point = "";
			this.destination_point = "";
			
			Random r_1 = new Random();
			int r_1_min = 1;
			int r_1_max = 101;
			int r_1_start_time = r_1.nextInt(r_1_max - r_1_min) + r_1_min;
			this.start_time = r_1_start_time;
			
			Random r_2 = new Random();
			int r_2_min = 100;
			int r_2_max = 200;
			int r_2_late_time = r_2.nextInt(r_2_max - r_2_min) + r_2_min;
			this.late_time = r_2_late_time;
			
			Random r_3 = new Random();
			int r_3_min = 1;
			int r_3_max = 51;
			int r_3_min_altitude = r_3.nextInt(r_3_max - r_3_min) + r_3_min;
			this.min_altitude = r_3_min_altitude;
			
			Random r_4 = new Random();
			int r_4_min = 50;
			int r_4_max = 100;
			int r_4_max_altitude = r_4.nextInt(r_4_max - r_4_min) + r_4_min;
			this.max_altitude = r_4_max_altitude;
			
		} else if (mode == "random_start") {
			this.mode = mode;
		} else if (mode == "random_end") {
			this.mode = mode;
		} else if (mode == "probability"){
			this.mode = mode;
			this.mission = "probability";
			this.type = "probability";
		} else {
			Util my_tool = new Util();
			my_specific_mission_node = my_tool.load_specific_operation(mode);
			
			this.mode = mode;
			this.mission = my_specific_mission_node.return_mission();
			this.type = my_specific_mission_node.return_type();
			this.start_point = my_specific_mission_node.return_start_point();
			this.connection_point = my_specific_mission_node.return_connection_point();
			this.destination_point = my_specific_mission_node.return_destination_point();
			this.start_time = my_specific_mission_node.return_start_time();
			this.late_time = my_specific_mission_node.return_late_time();
			this.min_altitude = my_specific_mission_node.return_min_altitude();
			this.max_altitude = my_specific_mission_node.return_max_altitude();
		}

		
	}
	
	public String return_mode () {
		return mode;
	}
	
	public String return_mission () {
	    return mission;
	}

	public String return_type () {
	    return type;
	}

	public void set_start_point (String temp_start_point) {
		start_point = temp_start_point;
	}
	
	public String return_start_point () {
	    return start_point;
	}
	
	public void set_connection_point (String temp_connection_point) {
	    connection_point = temp_connection_point;
	}
	
	public String return_connection_point () {
	    return connection_point;
	}
	
	public void set_destination_point (String temp_destination_point) {
		destination_point = temp_destination_point;
	}
	
	public String return_destination_point () {
	    return destination_point;
	}

	public double return_start_time () {
	    return start_time;
	}

	public double return_late_time () {
	    return late_time;
	}
	
	public double return_min_altitude () {
	    return min_altitude;
	}

	public double return_max_altitude () {
	    return max_altitude;
	}

	
}

public class UAV {

	private Geography<Object> geography;
	private GeometryFactory fac;
//	private boolean moved;
	
    // UAV attributions
	private double height;
	private double speed;
	private boolean if_straight_path;
	private int start_time;
	private double tx_power;
	private double rx_threshold;
	private boolean if_nearest_target;
	private TreeList<Object> targets;
	private int internal_time_step;
	private Base_Station current_basestation;
	private int end_time;
	private ArrayList<Double> start_coordinate_pair;
	private ArrayList<ArrayList<Double>> connection_coordinate_pair;
	private ArrayList<Double> end_coordinate_pair;
	private ArrayList<Double> start_index_pair;
	private ArrayList<ArrayList<Double>> connection_index_pair;
	private ArrayList<Double> end_index_pair;
	private double x;
	private double y;
	
    // Path loss distance parameters
	private double log_distance_loss;
	
	// operation for loading specific mission
	private Operation my_operation;
	private Map<String, ArrayList<Double>> airport_list;
	private Operator my_operator;
	private int duration;
	
	double angle;
	private int current_batch;
	
    // Constructor
	public UAV(Geography<Object> geography, double height, double speed,
			   boolean if_straight_path, int start_time, double tx_power, double rx_threshold,
			   boolean if_nearest_target, String mode, Map<String, ArrayList<Double>> airport_list, Operator my_operator, int current_batch){
		this.geography = geography; 
		this.height = getMeters(height);
		this.speed = speed;
//		System.out.println(if_straight_path);
		this.if_straight_path = if_straight_path;
		this.tx_power = tx_power;
		this.rx_threshold = rx_threshold;
		this.if_nearest_target = if_nearest_target;
		this.targets = new TreeList();
		this.my_operation = new Operation(mode);
		this.start_time = start_time;//(int) this.my_operation.return_start_time();
		this.airport_list = airport_list;
		this.my_operator = my_operator;
		this.duration = (int) my_operator.find(this.my_operation.return_mission(), this.my_operation.return_type()).return_duration();
		this.internal_time_step = -1;
		this.current_basestation = null;
		this.end_time = -1;
		this.current_batch = current_batch;
		this.fac = new GeometryFactory();
		this.x = 0.0;
		this.y = 0.0;
	}
	
	// Random Constructor
	public UAV(Geography<Object> geography, double height, double speed,
			   boolean if_straight_path, int start_time, double tx_power, double rx_threshold,
			   boolean if_nearest_target, String mode, Map<String, ArrayList<Double>> airport_list, int current_batch) {
		this.geography = geography; 
		this.height = getMeters(height);
		this.speed = speed;
//		System.out.println(if_straight_path);
		this.if_straight_path = if_straight_path;
		this.tx_power = tx_power;
		this.rx_threshold = rx_threshold;
		this.if_nearest_target = if_nearest_target;
		this.targets = new TreeList();
		this.my_operation = new Operation(mode);
		this.start_time = start_time;//(int) this.my_operation.return_start_time();
		this.airport_list = airport_list;
		this.internal_time_step = -1;
		this.current_basestation = null;
		this.end_time = -1;
		this.current_batch = current_batch;
		this.fac = new GeometryFactory();
		this.x = 0.0;
		this.y = 0.0;
	}
	
	public void set_speed(double new_speed) {
		this.speed = new_speed;
	}
	
	public double return_speed() {
		return this.speed;
	}
	
	public void set_start_coordinate_pair(ArrayList<Double> start_coordinate_pair) {
		this.start_coordinate_pair = start_coordinate_pair;
	}
	
	public ArrayList<Double> return_start_coordinate_pair() {
		return this.start_coordinate_pair ;
	}
	
	public void set_start_index_pair(ArrayList<Double> start_index_pair) {
		this.start_index_pair = start_index_pair;
	}
	
	public ArrayList<Double> return_start_index_pair() {
		return this.start_index_pair ;
	}
	
	public void set_connection_coordinate_pair(ArrayList<ArrayList<Double>> connection_coordinate_pair) {
		this.connection_coordinate_pair = connection_coordinate_pair;
	}
	
	public ArrayList<ArrayList<Double>> return_connection_coordinate_pair() {
		return this.connection_coordinate_pair ;
	}
	
	public void set_connection_index_pair(ArrayList<ArrayList<Double>> connection_index_pair) {
		this.connection_index_pair = connection_index_pair;
	}
	
	public ArrayList<ArrayList<Double>> return_index_pair(){
		return this.connection_index_pair;
	}
	
	public void set_end_coordinate_pair(ArrayList<Double> end_coordinate_pair) {
		this.end_coordinate_pair = end_coordinate_pair;
	}
	
	public ArrayList<Double> return_end_coordinate_pair() {
		return this.end_coordinate_pair ;
	}
	
	public void set_end_index_pair(ArrayList<Double> end_index_pair) {
		this.end_index_pair = end_index_pair;
	}
	
	public ArrayList<Double> return_end_index_pair() {
		return this.end_index_pair ;
	}
	
	public int return_end_time() {
		return this.end_time;
	}
	
	public int return_start_time() {
		return this.start_time;
	}
	
	public int return_internal_time() {
		return this.internal_time_step;
	}
	
	public Operation return_Operation() {
		return my_operation;
	}
	
	public int set_internal_time(int time) {
		return this.internal_time_step = time;
	}
	
	// Calculate the path loss distance between UAV and base station
	// PL(d0) = Path Loss in dB at a distance d0
	// PLd>d0 = Path Loss in dB at an arbitrary distance d
	// n = Path Loss exponent
	// loss = A zero-mean Gaussian distributed random variable (in dB) with standard deviation
	private double cal_path_loss_distance(double d_0, double d, double n, double x){
		double PL = 38.02 + 10 * n * Math.log10(d/d_0) + x;
		return PL;
	}
	
	// move toward the target
	private void moveTowards_straight(Geometry pt) {
	        
	        // move the UAV
			Geometry myPoint = geography.getGeometry(this);

			double dx = pt.getCoordinate().x - myPoint.getCoordinate().x;
			double dy = pt.getCoordinate().y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}

			geography.moveByVector(this, this.speed, SI.METER, angle);
		}
		
		// move toward the target
	private void moveTowards_by_coordinate_straight(Coordinate pt) {
	        
	        // move the UAV
			Geometry myPoint = geography.getGeometry(this);
	        
			double dx = pt.x - myPoint.getCoordinate().x;
			double dy = pt.y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}

			geography.moveByVector(this, this.speed, SI.METER, angle);
		}
	
	// move toward the target
	private void moveTowards_Manhattan(Geometry pt) {
        
        // move the UAV
		Geometry myPoint = geography.getGeometry(this);
		double distance_x = distance2Coordinate(geography, myPoint.getCoordinate().y, pt.getCoordinate().x, myPoint.getCoordinate().y, myPoint.getCoordinate().x);
		double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, myPoint.getCoordinate().x, myPoint.getCoordinate().y, myPoint.getCoordinate().x);
		
		if(Math.abs(distance_y) > this.speed) {
			double dx = myPoint.getCoordinate().x - myPoint.getCoordinate().x;
			double dy = pt.getCoordinate().y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}

			geography.moveByVector(this, this.speed, SI.METER, angle);
		} else {
			double dx = pt.getCoordinate().x - myPoint.getCoordinate().x;
			double dy = myPoint.getCoordinate().y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}
			geography.moveByVector(this, this.speed, SI.METER, angle);
		}

	}
	
	// move toward the target
	private void moveTowards_by_coordinate_Manhattan(Coordinate pt) {
        
        // move the UAV
		Geometry myPoint = geography.getGeometry(this);
		double distance_x = distance2Coordinate(geography, myPoint.getCoordinate().y, pt.x, myPoint.getCoordinate().y, myPoint.getCoordinate().x);
		double distance_y = distance2Coordinate(geography, pt.y, myPoint.getCoordinate().x, myPoint.getCoordinate().y, myPoint.getCoordinate().x);
		
		if(Math.abs(distance_y) > this.speed) {
			double dx = myPoint.getCoordinate().x - myPoint.getCoordinate().x;
			double dy = pt.y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}

			geography.moveByVector(this, this.speed, SI.METER, angle);
		} else {
			double dx = pt.x - myPoint.getCoordinate().x;
			double dy = myPoint.getCoordinate().y - myPoint.getCoordinate().y;
			
			if (Math.atan2(dy, dx) < 0) {
				angle =  2 * Math.PI + Math.atan2(dy, dx);
			} else {
				angle = Math.atan2(dy, dx);
			}
			geography.moveByVector(this, this.speed, SI.METER, angle);
		}

	}
	
	// create a communication link between UAV and base station
	private void communicate_with_base_station() {
		
		Map<Object, Double> unsorted_base_station_distance_map = new HashMap<Object, Double>();
		
		Geometry pt = geography.getGeometry(this);
		
		short uav_x = (short) (distance2Coordinate(geography, -76.2597052, 43.0802922, -76.2597052, pt.getCoordinate().y)  / (18 * 5));
		short uav_y = (short) (distance2Coordinate(geography, -76.2597052, 43.0802922, pt.getCoordinate().x, 43.0802922)  / (18 * 5));
		
		List<Object> base_stations = new ArrayList<Object>();
		for (Object obj : geography.getAllObjects()) {
			if (obj instanceof Base_Station) {
				base_stations.add(obj);
			}
		}
		if (base_stations.size() > 0) {

			for	(Object obj : base_stations)
			{
				Geometry tmp = geography.getGeometry(obj);
				
				Base_Station temp = (Base_Station) obj;
				temp.remove_UAV((UAV) this);				

				short basestation_x = (short) (distance2Coordinate(geography, -76.2597052, 43.0802922, -76.2597052, tmp.getCoordinate().y)  / (18 * 5));
				short basestation_y = (short) (distance2Coordinate(geography, -76.2597052, 43.0802922, tmp.getCoordinate().x, 43.0802922)  / (18 * 5));

				double distance = Math.sqrt(Math.pow((Math.abs(uav_x - basestation_x) - 2) * 90, 2) + Math.pow((Math.abs(uav_y - basestation_y) - 2) * 90, 2));
				
				unsorted_base_station_distance_map.put(obj, distance);

			}
	        
			Map<Object,Double> sorted_base_station_distance_map = sortMapByValues(unsorted_base_station_distance_map);
	        
			Object selected_base_station = null;
        	
        	this.current_basestation = null;
        	
	        for(Map.Entry<Object, Double> entry : sorted_base_station_distance_map.entrySet()) {
	        	
	        	selected_base_station = entry.getKey();
				
				double distance = Math.floor(entry.getValue());
				
				double n = generate_n_by_probability(entry.getValue());
	        	this.log_distance_loss = this.cal_path_loss_distance(1.00, distance, 3, 0) ;
	        	
//	        	if (this.log_distance_loss > 139.5) {
//	        		this.log_distance_loss -= 10;
//	        	}
	        	
	        	Base_Station temp = (Base_Station) selected_base_station;
	        	
	        	if (temp.get_current_num_UAV() < temp.get_num_of_channels() && this.log_distance_loss <= 140.0)
	        	{
	        		this.current_basestation = (Base_Station) selected_base_station;
	        		edge_operation(base_stations,selected_base_station, this);
	        		break;
	        	}
	        }
		}
	}

	
	private void normal_step(UAV curr_uav) {
		// get the grid location of this UAV
		Geometry pt = geography.getGeometry(curr_uav);

		if (curr_uav.my_operation.return_type().contains("return")) {
			if (curr_uav.my_operation.return_connection_point().isEmpty()) {
				if (this.my_operation.return_destination_point() != "") {
		            double x = curr_uav.airport_list.get(curr_uav.my_operation.return_destination_point()).get(0);
		            double y = curr_uav.airport_list.get(curr_uav.my_operation.return_destination_point()).get(1);
		            
		    		List<Object> Target_collection = new ArrayList<Object>();
		    		for (Object obj : geography.getAllObjects()) {
		    			if (obj instanceof Target) {
		    				Target_collection.add(obj);
		    			}
		    		}
		    		
		    		Geometry target = null;
		    		
		    		for (Object temp : Target_collection) {
		    			target = geography.getGeometry(temp);
		    			if (target.getCoordinate().x == x && target.getCoordinate().y == y)
		    				break;
		    		}
		    		
					double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.getCoordinate().x);
					double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.getCoordinate().y, pt.getCoordinate().x);

					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						this.my_operation.set_destination_point("");
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						this.end_time = this.internal_time_step;
						geography.move(this, target);
						save_location("-1");
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_straight(target);
						} else {
							moveTowards_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				} else {
		            double x = curr_uav.airport_list.get(curr_uav.my_operation.return_start_point()).get(0);
		            double y = curr_uav.airport_list.get(curr_uav.my_operation.return_start_point()).get(1);

		    		List<Object> Target_collection = new ArrayList<Object>();
		    		for (Object obj : geography.getAllObjects()) {
		    			if (obj instanceof Target) {
		    				Target_collection.add(obj);
		    			}
		    		}
		    		
		    		Geometry target = null;
		    		
		    		for (Object temp : Target_collection) {
		    			target = geography.getGeometry(temp);
		    			if (target.getCoordinate().x == x && target.getCoordinate().y == y)
		    				break;
		    		}
					double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.getCoordinate().x);
					double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.getCoordinate().y, pt.getCoordinate().x);
					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						this.end_time = this.internal_time_step;

						geography.move(this, target);
						save_location("-1");
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_straight(target);
						} else {
							moveTowards_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				}

			} else {
	            double x = curr_uav.airport_list.get(curr_uav.my_operation.return_connection_point()).get(0);
	            double y = curr_uav.airport_list.get(curr_uav.my_operation.return_connection_point()).get(1);
	            
	    		List<Object> Target_collection = new ArrayList<Object>();
	    		for (Object obj : geography.getAllObjects()) {
	    			if (obj instanceof Target) {
	    				Target_collection.add(obj);
	    			}
	    		}
	    		
	    		Geometry target = null;
	    		
	    		for (Object temp : Target_collection) {
	    			target = geography.getGeometry(temp);
	    			if (target.getCoordinate().x == x && target.getCoordinate().y == y)
	    				break;
	    		}
	    		
				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.getCoordinate().x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.getCoordinate().y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					this.my_operation.set_connection_point("");
					clear_all_communication_link(curr_uav);
					//remove
					Context context = ContextUtils.getContext(this);
					this.end_time = this.internal_time_step;
					
					geography.move(this, target);
					save_location("-1");
					context.remove(this);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_straight(target);
					} else {
						moveTowards_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
			
		} else {
			if (this.my_operation.return_connection_point().isEmpty()) {
	            double x = curr_uav.airport_list.get(curr_uav.my_operation.return_destination_point()).get(0);
	            double y = curr_uav.airport_list.get(curr_uav.my_operation.return_destination_point()).get(1);

	    		List<Object> Target_collection = new ArrayList<Object>();
	    		for (Object obj : geography.getAllObjects()) {
	    			if (obj instanceof Target) {
	    				Target_collection.add(obj);
	    			}
	    		}
	    		
	    		Geometry target = null;
	    		
	    		for (Object temp : Target_collection) {
	    			target = geography.getGeometry(temp);
	    			if (target.getCoordinate().x == x && target.getCoordinate().y == y)
	    				break;
	    		}

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.getCoordinate().x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.getCoordinate().y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					clear_all_communication_link(curr_uav);
					//remove
					Context context = ContextUtils.getContext(this);
					this.end_time = this.internal_time_step;

					geography.move(this, target);
					save_location("-1");
					context.remove(this);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_straight(target);
					} else {
						moveTowards_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			} else {
	            double x = curr_uav.airport_list.get(curr_uav.my_operation.return_connection_point()).get(0);
	            double y = curr_uav.airport_list.get(curr_uav.my_operation.return_connection_point()).get(1);
	            
	    		List<Object> Target_collection = new ArrayList<Object>();
	    		for (Object obj : geography.getAllObjects()) {
	    			if (obj instanceof Target) {
	    				Target_collection.add(obj);
	    			}
	    		}
	    		
	    		Geometry target = null;
	    		
	    		for (Object temp : Target_collection) {
	    			target = geography.getGeometry(temp);
	    			if (target.getCoordinate().x == x && target.getCoordinate().y == y)
	    				break;
	    		}

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.getCoordinate().x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.getCoordinate().y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					this.my_operation.set_connection_point("");
					clear_all_communication_link(curr_uav);
					//remove
					Context context = ContextUtils.getContext(this);
					this.end_time = this.internal_time_step;

					geography.move(this, target);
					save_location("-1");
					context.remove(this);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_straight(target);
					} else {
						moveTowards_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
		}
	}
	
	private void probability_generation_step(UAV curr_uav) {
		// get the grid location of this UAV
		Geometry pt = geography.getGeometry(curr_uav);
		if (curr_uav.my_operation.return_type().contains("return")) {
			if (curr_uav.return_connection_coordinate_pair().isEmpty()) {
				if (!curr_uav.return_end_coordinate_pair().isEmpty()) {
		            double x = curr_uav.return_end_coordinate_pair().get(0);
		            double y = curr_uav.return_end_coordinate_pair().get(1);
            
		    		Coordinate target = new Coordinate(x, y);
					double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
					double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						this.end_time = this.internal_time_step;
						 
						Coordinate connection_coord = new Coordinate(x,y);
						Point geom_connection = fac.createPoint(connection_coord);
						geography.move(this, geom_connection);

						save_location("-1");
										
						curr_uav.return_end_coordinate_pair().clear();
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_by_coordinate_straight(target);
						} else {
							moveTowards_by_coordinate_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				} else {
		            double x = curr_uav.return_start_coordinate_pair().get(0);
		            double y = curr_uav.return_start_coordinate_pair().get(1);
//		            System.out.print("Here!");
		    		Coordinate target = new Coordinate(x, y);

					double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
					double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						this.end_time = this.internal_time_step;
						 if (this.if_straight_path) {
						 	moveTowards_by_coordinate_straight(target);
						 } else {
						 	moveTowards_by_coordinate_Manhattan(target);
						 }
						save_location("-1");
						
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_by_coordinate_straight(target);
						} else {
							moveTowards_by_coordinate_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				}

			} else {
	            double x = curr_uav.return_connection_coordinate_pair().get(0).get(0);
	            double y = curr_uav.return_connection_coordinate_pair().get(0).get(1);
	            
	    		Coordinate target = new Coordinate(x, y);

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					communicate_with_base_station();
					
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					curr_uav.return_connection_coordinate_pair().remove(0);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
			
		} else {
			if (curr_uav.return_connection_coordinate_pair().isEmpty()) {
	            double x = curr_uav.return_end_coordinate_pair().get(0);
	            double y = curr_uav.return_end_coordinate_pair().get(1);

	    		Coordinate target = new Coordinate(x, y);

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					this.end_time = this.internal_time_step;
					 if (this.if_straight_path) {
					 	moveTowards_by_coordinate_straight(target);
					 } else {
					 	moveTowards_by_coordinate_Manhattan(target);
					 }
					save_location("-1");
					
					clear_all_communication_link(curr_uav);
					//remove
					Context context = ContextUtils.getContext(this);
					context.remove(this);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			} else {
	            double x = curr_uav.return_connection_coordinate_pair().get(0).get(0);
	            double y = curr_uav.return_connection_coordinate_pair().get(0).get(1);
	            
	    		Coordinate target = new Coordinate(x, y);
				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);
				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					communicate_with_base_station();
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					curr_uav.return_connection_coordinate_pair().remove(0);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
		}
	}
	
	private void probability_generation_index_step(UAV curr_uav) {
		// get the grid location of this UAV
		Geometry pt = geography.getGeometry(curr_uav);
		if (curr_uav.my_operation.return_type().contains("return")) {
			if (curr_uav.return_index_pair().isEmpty()) {
				if (!curr_uav.return_end_index_pair().isEmpty()) {
		            double x = curr_uav.return_end_index_pair().get(0);
		            double y = curr_uav.return_end_index_pair().get(1);
		          
		    		Coordinate target = new Coordinate(x, y);

		            double distance_x = x - curr_uav.x;
		            double distance_y = y - curr_uav.y;

					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						this.end_time = this.internal_time_step;
						 
						Coordinate connection_coord = new Coordinate(x,y);
						Point geom_connection = fac.createPoint(connection_coord);
						geography.move(this, geom_connection);

						save_location("-1");
										
						curr_uav.return_end_coordinate_pair().clear();
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_by_coordinate_straight(target);
						} else {
							moveTowards_by_coordinate_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				} else {
		            double x = curr_uav.return_start_coordinate_pair().get(0);
		            double y = curr_uav.return_start_coordinate_pair().get(1);

		    		Coordinate target = new Coordinate(x, y);

					double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
					double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

					if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
						this.end_time = this.internal_time_step;
						 if (this.if_straight_path) {
						 	moveTowards_by_coordinate_straight(target);
						 } else {
						 	moveTowards_by_coordinate_Manhattan(target);
						 }

						save_location("-1");
						
						clear_all_communication_link(curr_uav);
						//remove
						Context context = ContextUtils.getContext(this);
						context.remove(this);
					}
					else {
						communicate_with_base_station();
						if (this.if_straight_path) {
							moveTowards_by_coordinate_straight(target);
						} else {
							moveTowards_by_coordinate_Manhattan(target);
						}
						if (this.internal_time_step % 10 ==0) {
							save_location("1");
						}
						
					}
				}

			} else {
	            double x = curr_uav.return_connection_coordinate_pair().get(0).get(0);
	            double y = curr_uav.return_connection_coordinate_pair().get(0).get(1);
	            
	    		Coordinate target = new Coordinate(x, y);

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					communicate_with_base_station();
					
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
					curr_uav.return_connection_coordinate_pair().remove(0);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
			
		} else {
			if (curr_uav.return_connection_coordinate_pair().isEmpty()) {

	            double x = curr_uav.return_end_coordinate_pair().get(0);
	            double y = curr_uav.return_end_coordinate_pair().get(1);

	    		Coordinate target = new Coordinate(x, y);

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					this.end_time = this.internal_time_step;
					 if (this.if_straight_path) {
					 	moveTowards_by_coordinate_straight(target);
					 } else {
					 	moveTowards_by_coordinate_Manhattan(target);
					 }

					save_location("-1");
					
					clear_all_communication_link(curr_uav);
					//remove
					Context context = ContextUtils.getContext(this);
					context.remove(this);
				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			} else {
	            double x = curr_uav.return_connection_coordinate_pair().get(0).get(0);
	            double y = curr_uav.return_connection_coordinate_pair().get(0).get(1);
	            
	    		Coordinate target = new Coordinate(x, y);

				double distance_x = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, pt.getCoordinate().y, target.x);
				double distance_y = distance2Coordinate(geography, pt.getCoordinate().y, pt.getCoordinate().x, target.y, pt.getCoordinate().x);

				if (distance_x < curr_uav.speed && distance_y < curr_uav.speed) {
					communicate_with_base_station();

					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
					curr_uav.return_connection_coordinate_pair().remove(0);

				}
				else {
					communicate_with_base_station();
					if (this.if_straight_path) {
						moveTowards_by_coordinate_straight(target);
					} else {
						moveTowards_by_coordinate_Manhattan(target);
					}
					if (this.internal_time_step % 10 ==0) {
						save_location("1");
					}
					
				}
			}
		}
	}
	
	// UAV's move function
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void step() {
		this.internal_time_step++;
		if (this.internal_time_step == 0) {
			save_location("1");
		}
		if (this.my_operation.return_mode() == "all_random") {
			normal_step(this);
		} else if(this.my_operation.return_mode() == "probability"){
			probability_generation_step(this);
		} else {
			normal_step(this);
		}
	}
	
	// find single target or find nearest target
	private Geometry find_target() {
		// get the grid location of this UAV
		Geometry pt = geography.getGeometry(this);

		if (targets.isEmpty()) {
			for (Object obj : geography.getAllObjects()) {
				if (obj instanceof Target) {
					targets.add(obj);
				}
			}
		}

		Geometry target = null;
		if (if_nearest_target == false) {

		    target = geography.getGeometry(targets.get(0));
		} else {

			double minimum = Double.MAX_VALUE;
			for (Object tar : targets) {
				Geometry tmp = geography.getGeometry(tar);
				
				double distance = pt.distance(tmp);
				if (distance < minimum) {
					target = geography.getGeometry(tar);
					minimum = distance;
				}
			}	
		}
		return target;
	}
	
	private void clear_all_communication_link(Object uav) {
		List<Object> base_stations = new ArrayList<Object>();
		
		for (Object obj : geography.getAllObjects()) {
			if (obj instanceof Base_Station) {
				base_stations.add(obj);
			}
		}
		
		Context<Object> context = ContextUtils.getContext(base_stations.get(0));
		
		for (Object obj : base_stations) {
			Base_Station temp = (Base_Station) obj;
			temp.remove_UAV((UAV) uav);
		}
		
	}
	
	// generate n by using the distance and probability
	private double generate_n_by_probability(double distance) {
		
		int probability = 0;
		
		if (distance <= 30000.00) {
			probability = 90;
		} else if (30000.00 < distance && distance < 60000.00) {
			probability = 80;
		} else {
			probability = 70;
		}
		
		Random r = new Random();
		int Low = 1;
		int High = 101;
		int Result = r.nextInt(High-Low) + Low;
		
		if(Result < probability) {
			return 3;
		} else {
			return 3.5;
		}
	}
	
	// add/remove channels && add/remove edges
	private void edge_operation(List<Object> base_stations, Object selected_base_station, Object uav) {
		
		Base_Station temp_base_station = (Base_Station) selected_base_station;

		// add uav into specific base station channel
		temp_base_station.add_UAV((UAV) uav);
	}
	
	// sort map by value
    private static Map<Object, Double> sortMapByValues(Map<Object, Double> aMap) {
        
        Set<Entry<Object,Double>> mapEntries = aMap.entrySet();

        // used linked list to sort, because insertion of elements in linked list is faster than an array list. 
        List<Entry<Object,Double>> aList = new LinkedList<Entry<Object,Double>>(mapEntries);

        // sorting the List
        Collections.sort(aList, new Comparator<Entry<Object,Double>>() {

            @Override
            public int compare(Entry<Object,Double> ele1,
                    Entry<Object,Double> ele2) {
                
                return ele1.getValue().compareTo(ele2.getValue());
            }
        });
        
        // Storing the list into Linked HashMap to preserve the order of insertion. 
        Map<Object,Double> aMap2 = new LinkedHashMap<Object,Double>();
        for(Entry<Object,Double> entry: aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }
        

        return aMap2;
    }
    
    // return distance in meters
    private static double distance2Coordinate(Geography g, double lon_1, double lat_1, double lon_2, double lat_2) {
		GeodeticCalculator calculator = new GeodeticCalculator(g.getCRS());
        calculator.setStartingGeographicPoint(lon_1, lat_1);
        calculator.setDestinationGeographicPoint(lon_2, lat_2);
        return calculator.getOrthodromicDistance(); 
    }
    
    // return distance in meters
    private static double distance2Coordinate(Geography g, Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(g.getCRS());
        calculator.setStartingGeographicPoint(c1.x, c1.y);
        calculator.setDestinationGeographicPoint(c2.x, c2.y);
        return calculator.getOrthodromicDistance(); 
    }
    
    // save UAV location in each time tick
    private void save_location(String finished) {
    	int id = this.hashCode();
    	String mission = this.my_operation.return_mission();

		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
    	
    	String fileName= temp_pre_fix + File.separator + "report" + File.separator + "batch_" + Integer.toString(current_batch) + File.separator + "uav_index" + ".csv";
    	String fileName_coordinate= temp_pre_fix + File.separator + "report" + File.separator + "batch_" + Integer.toString(current_batch) + File.separator + "uav_coordinate" + ".csv";
    	Geometry myPoint = geography.getGeometry(this);
    	try {
    		PrintWriter writer_uav_index = new PrintWriter(new FileOutputStream(new File(fileName),true));
    		PrintWriter writer_uav_coordinate = new PrintWriter(new FileOutputStream(new File(fileName_coordinate),true));
    		StringBuilder uav_index_string = new StringBuilder();
    		StringBuilder uav_coordinate_string = new StringBuilder();
    		
    		Double x_coordinate = BigDecimal.valueOf(myPoint.getCoordinate().x)
    			    .setScale(7, RoundingMode.HALF_UP)
    			    .doubleValue();
    		Double y_coordinate = BigDecimal.valueOf(myPoint.getCoordinate().y)
    			    .setScale(7, RoundingMode.HALF_UP)
    			    .doubleValue();
			double y = distance2Coordinate(geography, -76.2597052, 43.0802922, myPoint.getCoordinate().x, 43.0802922);
			double x = distance2Coordinate(geography, -76.2597052, 43.0802922, -76.2597052, myPoint.getCoordinate().y);

    		Double loss = this.log_distance_loss;
    		if (this.current_basestation == null) {
    			uav_index_string.append(internal_time_step);
    			uav_index_string.append(',');
    			uav_index_string.append(id);
        		uav_index_string.append(',');
        		uav_index_string.append(y);
        		uav_index_string.append(',');
        		uav_index_string.append(x);
        		uav_index_string.append(',');
        		uav_index_string.append(loss);
        		uav_index_string.append(',');
        		uav_index_string.append("-1");
        		uav_index_string.append(',');
        		uav_index_string.append(finished);
        		uav_index_string.append('\n');
        		writer_uav_index.write(uav_index_string.toString());
//    			writer.write(String.format(formatStr, internal_time_step, id, y, x, 999, "-1", finished));
    			uav_coordinate_string.append(internal_time_step);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(id);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(y_coordinate);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(x_coordinate);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(loss);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append("-1");
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(finished);
    			uav_coordinate_string.append('\n');
    			writer_uav_coordinate.write(uav_coordinate_string.toString());
    		} else {
    			uav_index_string.append(internal_time_step);
    			uav_index_string.append(',');
    			uav_index_string.append(id);
        		uav_index_string.append(',');
        		uav_index_string.append(y);
        		uav_index_string.append(',');
        		uav_index_string.append(x);
        		uav_index_string.append(',');
        		uav_index_string.append(loss);
        		uav_index_string.append(',');
        		uav_index_string.append(this.current_basestation.return_bs_id());
        		uav_index_string.append(',');
        		uav_index_string.append(finished);
        		uav_index_string.append('\n');
        		writer_uav_index.write(uav_index_string.toString());

    			uav_coordinate_string.append(internal_time_step);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(id);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(y_coordinate);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(x_coordinate);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(loss);
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(this.current_basestation.return_bs_id());
    			uav_coordinate_string.append(',');
    			uav_coordinate_string.append(finished);
    			uav_coordinate_string.append('\n');
    			writer_uav_coordinate.write(uav_coordinate_string.toString());

    		}
    		
    		writer_uav_index.close();
    		writer_uav_coordinate.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    private double getMiles(double i) {
        return i*0.000621371192;
    }
    
    private double getMeters(double i) {
        return i*1609.344;
    }
    
}

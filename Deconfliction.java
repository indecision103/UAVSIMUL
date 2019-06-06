/**
 * 
 */
package jzombies;

import java.util.concurrent.TimeUnit;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
/**
 * @author ziyizhao
 *
 */

//queue node used in BFS
class Node
{
	 // (x, y) represents matrix cell coordinates
	 // dist represent its minimum distance from the source
	public short x = 0, y = 0;
	public int t = 0; 
	public int hCost = 0; 
	public int gCost = 0;
	public int tCost = 0;
	public int fCost = hCost + gCost + tCost;
	public Node parentNode;
	public boolean changeDirection = false;
	
	
	 Node(short x, short y, int t) {
	     this.x = x;
	     this.y = y;
	     this.t = t;
	 }
	 
	 Node(short x, short y) {
	     this.x = x;
	     this.y = y;
	 }
}

//virtual base station
class VirtualBasestation
{
	 // (x, y) represents matrix cell coordinates
	 // dist represent its minimum distance from the source
	public short x = 0, y = 0;
	public int occupied_channel = 0; 
	 
	VirtualBasestation(Geography<Object> geography, double TopLeftY, double TopLeftX, double BottomRightY, double BottomRightX, double start_x, double start_y) {
		this.x = (short) (distance2Coordinate(geography, TopLeftY, TopLeftX, TopLeftY, start_y) / (18 * 5));
		this.y = (short) (distance2Coordinate(geography, TopLeftY, TopLeftX, start_x, TopLeftX) / (18 * 5));
		System.out.println(this.x * 5 + " : " + this.y * 5);
	 }
	
	private static double distance2Coordinate(Geography g, double lon_1, double lat_1, double lon_2, double lat_2) {
		GeodeticCalculator calculator = new GeodeticCalculator(g.getCRS());
        calculator.setStartingGeographicPoint(lon_1, lat_1);
        calculator.setDestinationGeographicPoint(lon_2, lat_2);
        return calculator.getOrthodromicDistance(); 
    }
	
	public void reset_channel() {
		this.occupied_channel = 0;
	} 
	
	public void occupy_channel() {
		this.occupied_channel++;
	}
	
	public void free_channel() {
		this.occupied_channel--;
	}
    
}

public class Deconfliction {
    public Deconfliction(Geography<Object> geography, int ConflictThreshold, double TopLeftY, double TopLeftX, double BottomRightY, double BottomRightX, int simulation_time) {
    	this.geography = geography;
    	this.ConflictThreshold = ConflictThreshold;
    	this.TopLeftY = TopLeftY;
    	this.TopLeftX = TopLeftX;
    	this.BottomRightY = BottomRightY;
    	this.BottomRightX = BottomRightX;
    	this.internal_time_step = -1;
    	this.max_time_allow_check = 800;
    	this.TIME = simulation_time;
    	InitializeArray();
    	InitializeVirtualBasestation();
    }
    
    public void InitializeArray() {
    	this.ROW = (int) distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, this.BottomRightX) / 90; // x is lontitude // y is latitude
    	this.COLUMN = (int) distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.BottomRightY, this.TopLeftX) / 90;     
    	
    	this.UAVTrajectory = new HashMap<Integer, HashMap<Integer, Byte>>();
    	this.UAVCondition = new byte[this.ROW][this.COLUMN];
    	for(int row = 0; row < this.ROW; row++) {
    		for(int column = 0; column < this.COLUMN; column++) {
    			this.UAVCondition[row][column] = 0;
    		}
    	}
    	
    	System.out.println("Row Length: " + this.UAVCondition.length);
    	System.out.println("Column Length: " + this.UAVCondition[0].length);
    	System.out.println();
    }
    
    public void InitializeVirtualBasestation() {
    	List<Object> base_stations = new ArrayList<Object>();
		for (Object obj : this.geography.getAllObjects()) {
			if (obj instanceof Base_Station) {
				base_stations.add(obj);
			}
		}
		
		if (base_stations.size() > 0) {
			for	(Object obj : base_stations)
			{
				Geometry tmp_basestation = geography.getGeometry(obj);
				VirtualBasestation temp_virtual_basestation = new VirtualBasestation(this.geography, this.TopLeftY, this.TopLeftX, this.BottomRightY, this.BottomRightX, tmp_basestation.getCoordinate().x, tmp_basestation.getCoordinate().y);
				this.virtual_base_stations.add(temp_virtual_basestation);
			}
		}
//		System.out.println(this.virtual_base_stations.size());
//		for (int i = 0; i < this.virtual_base_stations.size(); i++) {
//			System.out.println("X: " + this.virtual_base_stations.get(i).x + " Y: " + this.virtual_base_stations.get(i).y);
//		}
    }
    
    public boolean FillOutArray(UAV curr_uav, int curr_batch) {
    	
    	double start_x = curr_uav.return_start_coordinate_pair().get(0);
        double start_y = curr_uav.return_start_coordinate_pair().get(1);
    	double end_x = curr_uav.return_end_coordinate_pair().get(0);
        double end_y = curr_uav.return_end_coordinate_pair().get(1);
		double distance = distance2Coordinate(geography, start_x, start_y, end_x, end_y);
		int original_time = (int) (distance/curr_uav.return_speed());
		
		ArrayList<Double> start_coordinate_pair = curr_uav.return_start_coordinate_pair();
		ArrayList<ArrayList<Double>> connection_coordinate_pair = curr_uav.return_connection_coordinate_pair();
		ArrayList<Double> end_coordinate_pair = curr_uav.return_end_coordinate_pair();

		short start_row_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, start_y) / (curr_uav.return_speed() * 5));
		short start_column_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, start_x, this.TopLeftX) / (curr_uav.return_speed() * 5));
		int start_time = (int) curr_uav.return_start_time();

		System.out.println("start row index: " + start_row_index);
		System.out.println("start column index: " + start_column_index);
		System.out.println("start time: " + start_time);
		
		short end_row_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, end_y) / (curr_uav.return_speed() * 5));
		short end_column_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, end_x, this.TopLeftX) / (curr_uav.return_speed() * 5));
		int end_time = Math.abs(start_row_index - end_row_index) + Math.abs(start_column_index - end_column_index);
		System.out.println("end row index: " + end_row_index);
		System.out.println("end column index: " + end_column_index);
		
		this.internal_time_step = start_time;
		System.out.println("internal_time_step: " + this.internal_time_step);
		
		long start = System.nanoTime();
		ArrayList<Node> turn_point = ASTAR_Routing(start_row_index, start_column_index, start_time, end_row_index, end_column_index, end_time, curr_batch);
		long finish = System.nanoTime();
		long timeElapsed = finish - start;
		System.out.println("routing time: " + timeElapsed * 0.000001 + "ms");
		
        if (turn_point.isEmpty()) {
        	record_routing_information(start_time, timeElapsed * 0.000001, 0, curr_batch, 0);
            return false;
        }
        turn_point.remove(turn_point.size() - 1);
        
        System.out.println("Adding Print turn points. The number of turn points is " + turn_point.size());
        for (int w = 0; w < turn_point.size(); w++) {
            System.out.println("[" + turn_point.get(w).x + " " + turn_point.get(w).y + " " + turn_point.get(w).t + "]");
        }

		if(turn_point.size() >= 1) {
			System.out.println("Converting turn point index to coordinates. ");
			ArrayList<ArrayList<Double>> coordinate_pairs = index_to_coordinates(turn_point, start_row_index, start_column_index, start_x, start_y);
			curr_uav.set_connection_coordinate_pair(new ArrayList<ArrayList<Double>>(coordinate_pairs));
			System.out.println(coordinate_pairs.size());
		}
		record_routing_information(start_time, timeElapsed * 0.000001, 1, curr_batch, turn_point.size());
		return true;
    }
    
    public void set_block_area(short start_x, short end_x, short start_y, short end_y, int duration) {
    	int number_of_one = 0;
    	
    	for (short i = start_x; i < end_x; i++) {
            for (short j = start_y; j < end_y; j++) {
                this.UAVCondition[i][j] = (byte) this.ConflictThreshold;
                number_of_one++;
            }
        }
        System.out.println("number of one : " + number_of_one);
        
        System.out.println("block x : " + this.UAVCondition.length);
        System.out.println("block y : " + this.UAVCondition[0].length);
        int count = 0;
        for (short i = 0; i < this.UAVCondition.length; i++) {
            for (short j = 0; j < this.UAVCondition[0].length; j++) {
                if (this.UAVCondition[i][j] == this.ConflictThreshold) {
                	count++;
                }
            }
        }
        System.out.println("Set block area : " + count);
        System.out.println();
    }

    //get distance 
    private int GetDistance(Node p1, Node p2){
	    int xDif = Math.abs(p1.x - p2.x);
	    int yDif = Math.abs(p1.y - p2.y);
	    return xDif + yDif;
    }
    
	// retrace path
	private ArrayList<Node> buildPath(Node startNode, Node endNode){
		ArrayList<Node> path = new ArrayList<Node>();
		Node currentNode = endNode;
	
		while (currentNode != startNode)
		{
		    path.add(currentNode);
		    //by taking the parentNodes we assigned
		    currentNode = currentNode.parentNode;
		}
		path.add(startNode);
		return path;
	}
	
	private ArrayList<Node> getNeighbours(Node p, HashMap<Integer, Node> closedSet) {
		ArrayList<Node> neighbours = new ArrayList<Node>();
	        for (int i = 0; i < 4; i++) {
	            if (p.x + this.row[i] < this.ROW && 
	            	p.x + this.row[i] >= 0 && 
	            	p.y + this.col[i] < this.COLUMN && 
	            	p.y + this.col[i] >= 0 && 
	            	p.t + this.time[i] < this.TIME && 
	            	p.t + this.time[i] < (this.internal_time_step + 300) &&
	            	this.UAVCondition[p.x + this.row[i]][p.y + this.col[i]] < this.ConflictThreshold &&
	            	CheckUAVTrajectory(p.x + this.row[i], p.y + this.col[i], p.t + this.time[i]) &&
	            	CheckSignalStrength(p.x + this.row[i], p.y + this.col[i], p.t + this.time[i]) &&
	            	!closedSet.containsKey(generateIndex(p.x + this.row[i], p.y + this.col[i]))) {
	                neighbours.add(new Node((short)(p.x + this.row[i]), (short)(p.y + this.col[i]), p.t + this.time[i]));
	            }
	        }
	        return neighbours;
	}
	
	private void ClearUAVTrajecotry(int current_time) {
		int previous_time = current_time - 10;;
		if(this.UAVTrajectory.containsKey(previous_time)) {
			this.UAVTrajectory.remove(previous_time);
		}
	}
	
	private Boolean CheckUAVTrajectory(int x, int y, int t) {
		if(!this.UAVTrajectory.containsKey(t)) {
			return true;
		} else {
			if (!this.UAVTrajectory.get(t).containsKey(generateIndex(x, y))) {
				return true;
			} else {
				if (this.UAVTrajectory.get(t).get(generateIndex(x, y)) < this.ConflictThreshold) {
					return true;
				}
			}
		}
		return false;
	}
	
	private Boolean CheckSignalStrength(int x, int y, int t) {
		boolean flag = false;
		UpdateVirtualBasestation(t);
		ArrayList<VirtualBasestation> basestation_candidates = SortBasestation(x, y);
		double distance = Math.ceil(Math.sqrt(Math.pow((Math.abs(x - basestation_candidates.get(0).x)) * 90, 2) + Math.pow((Math.abs(y - basestation_candidates.get(0).y)) * 90, 2)));
		double log_distance_loss = this.cal_path_loss_distance(1.00, distance, 3, 0);
		if (basestation_candidates.get(0).occupied_channel < 8 && Math.ceil(log_distance_loss) <= 140) {
			flag = true;
		}
		ResetVirtualBasestation();
		return flag;
	}
	
	private void UpdateVirtualBasestation(int t) {
		if(!this.UAVTrajectory.containsKey(t)) {
			return;
		} else {
			for (int index : this.UAVTrajectory.get(t).keySet()) {
				int x = index % this.yDigit;
				int y = index / this.yDigit;
				ArrayList<VirtualBasestation> basestation_candidates = SortBasestation(x, y);

				double distance = Math.ceil(Math.sqrt(Math.pow((Math.abs(x - basestation_candidates.get(0).x)) * 90, 2) + Math.pow((Math.abs(y - basestation_candidates.get(0).y)) * 90, 2)));
				double log_distance_loss = this.cal_path_loss_distance(1.00, distance, 3, 0);
				if (basestation_candidates.get(0).occupied_channel < 8 && Math.ceil(log_distance_loss) <= 140) {
					basestation_candidates.get(0).occupy_channel();
				}
			}
		}
		return;
	}
	
	private void RecordVistualBasestation(int t) {
		if(!this.UAVTrajectory.containsKey(t)) {
			return;
		} else {
			for (int index : this.UAVTrajectory.get(t).keySet()) {
				int x = index % this.yDigit;
				int y = index / this.yDigit;
				ArrayList<VirtualBasestation> basestation_candidates = SortBasestation(x, y);

				double distance = Math.ceil(Math.sqrt(Math.pow((Math.abs(x - basestation_candidates.get(0).x)) * 90, 2) + Math.pow((Math.abs(y - basestation_candidates.get(0).y)) * 90, 2)));
				double log_distance_loss = this.cal_path_loss_distance(1.00, distance, 3, 0);
				if (basestation_candidates.get(0).occupied_channel < 8 && Math.ceil(log_distance_loss) <= 140) {
					basestation_candidates.get(0).occupy_channel();
				}
			}
		}

		System.out.println("###########################################");
		System.out.println("Current Time: " + t);
		System.out.println("The number of in-fly UAVs: " + this.UAVTrajectory.get(t).keySet().size());
		for (int i = 0; i < this.virtual_base_stations.size(); i++) {
			System.out.println("X: " + this.virtual_base_stations.get(i).x + " Y: " + this.virtual_base_stations.get(i).y 
						+ " Occupied Channels: " + this.virtual_base_stations.get(i).occupied_channel);
		}
		System.out.println("###########################################");
		ResetVirtualBasestation();
		return;
	}
	
	private double cal_path_loss_distance(double d_0, double d, double n, double x){
		double PL = 38.02 + 10 * n * Math.log10(d/d_0) + x;
		return PL;
	}
	
	private ArrayList<VirtualBasestation> SortBasestation(int x, int y) {
		ArrayList<VirtualBasestation> result = new ArrayList<VirtualBasestation>();
		Map<VirtualBasestation, Double> unsorted_base_station_distance_map = new HashMap<VirtualBasestation, Double>();
		
		for(int i = 0; i < this.virtual_base_stations.size(); i++) {
			int basestation_x = this.virtual_base_stations.get(i).x;
			int basestation_y = this.virtual_base_stations.get(i).y;
			double distance = Math.sqrt(Math.pow(Math.abs(x - basestation_x), 2) + Math.pow(Math.abs(y - basestation_y), 2));
			unsorted_base_station_distance_map.put(this.virtual_base_stations.get(i), distance);
		}
		
		Map<VirtualBasestation,Double> sorted_base_station_distance_map = sortMapByValues(unsorted_base_station_distance_map);
		
		for(Map.Entry<VirtualBasestation, Double> entry : sorted_base_station_distance_map.entrySet()) {
			result.add(entry.getKey());
		}
		
		return result;
	}
	
	// sort map by value
	private static Map<VirtualBasestation, Double> sortMapByValues(Map<VirtualBasestation, Double> aMap) {
        
        Set<Entry<VirtualBasestation,Double>> mapEntries = aMap.entrySet();

        // used linked list to sort, because insertion of elements in linked list is faster than an array list. 
        List<Entry<VirtualBasestation,Double>> aList = new LinkedList<Entry<VirtualBasestation,Double>>(mapEntries);

        // sorting the List
        Collections.sort(aList, new Comparator<Entry<VirtualBasestation,Double>>() {

            @Override
            public int compare(Entry<VirtualBasestation,Double> ele1,
                    Entry<VirtualBasestation,Double> ele2) {
                
                return ele1.getValue().compareTo(ele2.getValue());
            }
        });
        
        // Storing the list into Linked HashMap to preserve the order of insertion. 
        Map<VirtualBasestation,Double> aMap2 = new LinkedHashMap<VirtualBasestation,Double>();
        for(Entry<VirtualBasestation,Double> entry: aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }
        

        return aMap2;
    }

	
	private void ResetVirtualBasestation() {
		for (int i = 0; i < this.virtual_base_stations.size(); i++) {
			this.virtual_base_stations.get(i).reset_channel();
		}
	}
	
	 //pq comparator
	public Comparator<Node> distanceComparator = new Comparator<Node>(){

       @Override
       public int compare(Node p1, Node p2) {
//    	   if (p1.fCost == p2.fCost && p1.hCost == p2.hCost) {
//    		   if (p1.changeDirection == p2.changeDirection) {
//    			   return 0;
//    		   } else if (p1.changeDirection == false) {
//    			   return -1;
//    		   } else if (p2.changeDirection == false) {
//    			   return 1;
//    		   }
//    	   }
    	   
    	   
    	   if (p1.fCost == p2.fCost && p1.hCost == p2.hCost) {
               return p1.tCost - p2.tCost;
           }
    	   if (p1.fCost == p2.fCost) {
    		   return p1.hCost - p2.hCost;
    	   }
    	   return p1.fCost - p2.fCost;
       }
   };
   
   	
   	private ArrayList<Node> getTurnPoints(ArrayList<Node> foundPath, Node target_node) {
       ArrayList<Node> path = foundPath;
       ArrayList<Node> turn_point = new ArrayList<Node>();
       
       int pre_x = path.get(path.size() - 1).x;
       int pre_y = path.get(path.size() - 1).y;
       int pre_t = path.get(path.size() - 1).t;
       if(!this.UAVTrajectory.containsKey(pre_t)) {
       		HashMap<Integer, Byte> temp_hashmap = new HashMap<Integer, Byte>();
       		temp_hashmap.put(generateIndex(pre_x, pre_y),  (byte) 1);
       		this.UAVTrajectory.put(pre_t, temp_hashmap);
       } else {
    	   if (!this.UAVTrajectory.get(pre_t).containsKey(generateIndex(pre_x, pre_y))) {
				this.UAVTrajectory.get(pre_t).put(generateIndex(pre_x, pre_y), (byte) 1);
			} else {
	       		int number_of_uav = this.UAVTrajectory.get(pre_t).get(generateIndex(pre_x, pre_y)) + 1;
	       		this.UAVTrajectory.get(pre_t).put(generateIndex(pre_x, pre_y), (byte) number_of_uav);	
			}
       }

       for (int l = path.size() - 1; l >= 0; l--) {
       	short cube_temp_x = path.get(l).x;
       	short cube_temp_y = path.get(l).y;
       	int cube_temp_t = path.get(l).t;
//       	System.out.println("[" + cube_temp_x + " " + cube_temp_y + " " + cube_temp_t + "]");

       	if(!this.UAVTrajectory.containsKey(cube_temp_t)) {
       		HashMap<Integer, Byte> temp_hashmap = new HashMap<Integer, Byte>();
       		temp_hashmap.put(generateIndex(cube_temp_x, cube_temp_y),  (byte) 1);
       		this.UAVTrajectory.put(cube_temp_t, temp_hashmap);
       	} else {
			if (!this.UAVTrajectory.get(cube_temp_t).containsKey(generateIndex(cube_temp_x, cube_temp_y))) {
				this.UAVTrajectory.get(cube_temp_t).put(generateIndex(cube_temp_x, cube_temp_y), (byte) 1);
			} else {
	       		int number_of_uav = this.UAVTrajectory.get(cube_temp_t).get(generateIndex(cube_temp_x, cube_temp_y)) + 1;
	       		this.UAVTrajectory.get(cube_temp_t).put(generateIndex(cube_temp_x, cube_temp_y), (byte) number_of_uav);	
			}
       	}
          
       	if(path.get(l).x != pre_x && path.get(l).y != pre_y) {
       		short temp_x = path.get(l + 1).x;
       		short temp_y = path.get(l + 1).y;
       		int temp_t = path.get(l + 1).t;
       		Node temp = new Node(temp_x, temp_y, temp_t);
       		turn_point.add(temp);        		

       		pre_x = path.get(l + 1).x;
       		pre_y = path.get(l + 1).y;
       	}
       }
       
      
       if(target_node.x != pre_x && target_node.y != pre_y ) {
          short temp_x = path.get(0).x;
    	   short temp_y = path.get(0).y;
    	   int temp_t = path.get(0).t;

    	   Node temp = new Node(temp_x, temp_y, temp_t);
    	   turn_point.add(temp);
       }
       // target node T need to be checked
      	if(!this.UAVTrajectory.containsKey(target_node.t)) {
       		HashMap<Integer, Byte> temp_hashmap = new HashMap<Integer, Byte>();
       		temp_hashmap.put(generateIndex(target_node.x, target_node.y),  (byte) 1);
       		this.UAVTrajectory.put(target_node.t, temp_hashmap);
       	} else {
			if (!this.UAVTrajectory.get(target_node.t).containsKey(generateIndex(target_node.x, target_node.y))) {
				this.UAVTrajectory.get(target_node.t).put(generateIndex(target_node.x, target_node.y), (byte) 1);
			} else {
	       		int number_of_uav = this.UAVTrajectory.get(target_node.t).get(generateIndex(target_node.x, target_node.y)) + 1;
	       		this.UAVTrajectory.get(target_node.t).put(generateIndex(target_node.x, target_node.y), (byte) number_of_uav);	
			}
       	}
       
       Node temp = new Node(target_node.x, target_node.y, target_node.t);
       turn_point.add(temp);
       return turn_point;
   }
	
    
   	private int generateIndex(int x, int y) {
    	return x + this.yDigit * y;
    }
   	
   	// Find Shortest Possible Route in a matrix mat from source
    // cell (i, j, t) to destination cell (x, y, z)
    
   	private ArrayList<Node> ASTAR_Routing(short i, short j, int t, short x, short y, int z, int curr_batch) {

        ArrayList<Node> turn_point = new ArrayList<Node>();
        
        int current_UAVs = 0;
        for (Object obj : this.geography.getAllObjects()) {   
            if (obj instanceof UAV) {
            	current_UAVs++;
            }
        }
        if (current_UAVs >= 80) {
        	return turn_point;
        }
        
		if (t >= 1000 && t % 1000 == 0) {
			RecordVistualBasestation(t);
		}
        
        ClearUAVTrajecotry(t);
        Node start_node = new Node(i, j, t);
        Node target_node = new Node(x, y, z);
		PriorityQueue<Node> openSet = new PriorityQueue<Node>(distanceComparator);
        ArrayList<Node> foundPath = new ArrayList<Node>();
        HashMap<Integer, Node> closedSet = new HashMap<Integer, Node>();
        HashMap<Integer, Node> ongoingSet = new HashMap<Integer, Node>();
        
        int min_dist = Integer.MAX_VALUE;
        int original_t = t;
        
        openSet.add(start_node);
        ongoingSet.put(generateIndex(start_node.x, start_node.y), start_node);
	     while(openSet.size() > 0) {
            //pull out from openSet
            Node currNode = openSet.poll();
            //we find it!!
            if (currNode.x == x && currNode.y == y) {
            	min_dist = currNode.t;
                foundPath = buildPath(start_node, currNode);	
                break;
            }
            //add to closedSet
            closedSet.put(generateIndex(currNode.x, currNode.y), currNode);
            ongoingSet.remove(generateIndex(currNode.x, currNode.y));
            
            // go check neigohours
            for (Node neighbour : getNeighbours(currNode, closedSet)) {         
            	
            		if (ongoingSet.containsKey(generateIndex(neighbour.x, neighbour.y))) {
            			neighbour = ongoingSet.get(generateIndex(neighbour.x, neighbour.y));
            		}
            		 if ((currNode.x == i && currNode.y == j)
            				|| (currNode.parentNode.x == i && currNode.parentNode.y == j)
         	                ||((neighbour.x == currNode.x && currNode.x == currNode.parentNode.x)
         	                		&& neighbour.y - currNode.y == currNode.y - currNode.parentNode.y)
         	                ||((neighbour.y == currNode.y && currNode.y == currNode.parentNode.y)
         	                		&& neighbour.x - currNode.x == currNode.x - currNode.parentNode.x)) {
         	                    neighbour.tCost = 0;
         	                } else {
         	                    neighbour.tCost = 1;
         	                }
            		
            		
//                	Node currParentNode = currNode.parentNode;
//                	if (currParentNode != null) {
//                    	if (currNode.x - currParentNode.x == neighbour.x - currNode.x &&
//                        		currNode.y - currParentNode.y == neighbour.y - currNode.y) {
//                        		neighbour.changeDirection = false;
//                        	} else {
//                        		neighbour.changeDirection = true;
//                        	}
//                	} else {
//                		neighbour.changeDirection = false;
//                	}
            		
                    int newMovementCostToNeighbour = currNode.gCost + GetDistance(currNode, neighbour) + neighbour.tCost;
                    //int newMovementCostToNeighbour = currNode.gCost + GetDistance(currNode, neighbour);
                    //and if it's lower than the neighbour's cost
                    if (newMovementCostToNeighbour < neighbour.gCost || !openSet.contains(neighbour)) {
                        //we calculate the new costs
	
                            neighbour.gCost = newMovementCostToNeighbour;
                            neighbour.hCost = Math.abs(neighbour.x - target_node.x) + Math.abs(neighbour.y - target_node.y);
                            neighbour.fCost = neighbour.gCost + neighbour.hCost ;
                            //Assign the parent node
                            neighbour.parentNode = currNode;
                            
                            //And add the neighbour node to the open set
                            if (!openSet.contains(neighbour)) {
                                openSet.add(neighbour);
                                ongoingSet.put(generateIndex(neighbour.x, neighbour.y), neighbour);
                            }
                    }
            }
        }

        if (min_dist != Integer.MAX_VALUE) {
            System.out.println("The shortest path from source to destination " + "has length " + (min_dist - original_t));
            target_node.t = start_node.t + min_dist;
        	turn_point = getTurnPoints(foundPath, target_node);
        } else {
            System.out.println("Destination can't be reached from given source");
        }
        	return turn_point;
    }
    
    // save UAV location in each time tick
    
   	private void save_indexlocation(int internal_time_step, short x, short y, int num, int current_batch) {
    	
		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
		
    	String fileName= temp_pre_fix + File.separator + "report" + File.separator + "batch_" + Integer.toString(current_batch) + File.separator + "uavidx" + ".csv";

    	try {

    		PrintWriter fileName_uav_index = new PrintWriter(new File(fileName));
    		StringBuilder uav_index_string = new StringBuilder();
    		uav_index_string.append(internal_time_step);
    		uav_index_string.append(',');
    		uav_index_string.append(y*5);
    		uav_index_string.append(',');
    		uav_index_string.append(x*5);
    		uav_index_string.append(',');
    		uav_index_string.append(num);
    		uav_index_string.append('\n');
    		fileName_uav_index.write(uav_index_string.toString());
    		fileName_uav_index.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    
   	private static double distance2Coordinate(Geography g, Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(g.getCRS());
        calculator.setStartingGeographicPoint(c1.x, c1.y);
        calculator.setDestinationGeographicPoint(c2.x, c2.y);
        return calculator.getOrthodromicDistance(); 
    }
    
    
   	private static double distance2Coordinate(Geography g, double lon_1, double lat_1, double lon_2, double lat_2) {
		GeodeticCalculator calculator = new GeodeticCalculator(g.getCRS());
        calculator.setStartingGeographicPoint(lon_1, lat_1);
        calculator.setDestinationGeographicPoint(lon_2, lat_2);
        return calculator.getOrthodromicDistance(); 
    }
    
    
   	private static ArrayList<ArrayList<Double>> index_to_distance(ArrayList<Node> turn_point) {
    	ArrayList<ArrayList<Double>> coordinate_pairs_idx = new ArrayList<ArrayList<Double>>();
    	for(int i = 0; i < turn_point.size(); i++) {
    		ArrayList<Double> temp_index = new ArrayList<Double>();
    		
    		Node temp_node = turn_point.get(i);
    		
    		double pointX = temp_node.x * 90;
    		double pointY = temp_node.y * 90;
    		
    		temp_index.add(pointX);
    		temp_index.add(pointY);
		
    		coordinate_pairs_idx.add(new ArrayList<Double>(temp_index));
    	}
    	return coordinate_pairs_idx;
    }
    
    
   	private ArrayList<ArrayList<Double>> index_to_coordinates(ArrayList<Node> turn_point, int index_x, int index_y, double coordinate_x, double coordinate_y) {
    	ArrayList<ArrayList<Double>> coordinate_pairs = new ArrayList<ArrayList<Double>>();
    	int pre_index_x = index_x;
    	int pre_index_y = index_y;
    	double pre_coordinate_x = coordinate_x;
    	double pre_coordinate_y = coordinate_y;
//    	System.out.println("Start:" + "X: " + pre_index_x + " Y: " + pre_index_y);
    	for(int i = 0; i < turn_point.size(); i++) {
//    		System.out.println(i + 1);
    		ArrayList<Double> temp_coordinates = new ArrayList<Double>();  		
    		Node temp_node = turn_point.get(i); 		
    		double pointX = pre_coordinate_x;
    		double pointY = pre_coordinate_y;
        	double weight = 0.999;
        	int count = 1;
    		while (!converter_check(temp_node.x, temp_node.y, pointX, pointY)) {
    			double dist = Math.sqrt(Math.pow((temp_node.x - pre_index_x), 2) + Math.pow((temp_node.y - pre_index_y), 2));
        		if(temp_node.x == pre_index_x) {
        			if(temp_node.y < pre_index_y) {
//        				System.out.println("Operation 1");
    		    		pointX = pre_coordinate_x - (dist) * 90 * 0.0000089 / Math.cos(pre_coordinate_y * 0.018) * weight;
        			} else if(temp_node.y > pre_index_y) {
//        				System.out.println("Operation 2");
        				pointX = pre_coordinate_x + (dist) * 90 * 0.0000089 / Math.cos(pre_coordinate_y * 0.018) * weight;
    				}
    			} else if(temp_node.y == pre_index_y) {
    				if(temp_node.x < pre_index_x) {
//    					System.out.println("Operation 3");
    					pointY = pre_coordinate_y + (dist) * 90 * 0.0000089 * weight;
    	
    				}else if(temp_node.x > pre_index_x) {
//    					System.out.println("Operation 4");
    		    		pointY = pre_coordinate_y - (dist) * 90 * 0.0000089 * weight;
    				}
    			}
        		short row_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, pointY) / (90));
        		short column_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, pointX, this.TopLeftX) / (90));
        		pre_coordinate_x = pointX;
    			pre_coordinate_y = pointY;
        		pre_index_x = row_index;
    			pre_index_y = column_index;
    			weight = weight * weight; 
    			if(count == 10) {
    				weight = 0.999;
    			}
    			count++;
//    			System.out.println("After:" + "X: " + row_index + " Y: " + column_index);
    		}
    		pre_index_x = temp_node.x;
			pre_index_y = temp_node.y;
    		if (!converter_check(temp_node.x, temp_node.y, pointX, pointY)) {
        		short row_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, pointY) / (90));
        		short column_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, pointX, this.TopLeftX) / (90));
    			System.out.println("**********************************************************************************************");
    			System.out.println("Converter is not correct!!!!!!!");
    			System.out.println("Before:" + "X: " + temp_node.x + " Y: " + temp_node.y);
    			System.out.println("After: " + "X: " + row_index + " Y: " + column_index);
    			System.out.println("**********************************************************************************************");
    		}
    		temp_coordinates.add(pointX);
    		temp_coordinates.add(pointY);
    		coordinate_pairs.add(new ArrayList<Double>(temp_coordinates));
    	}
    	return coordinate_pairs;
    }
    
    
   	private Boolean converter_check(int index_x, int index_y, double pointX, double pointY) {
		short row_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, this.TopLeftY, pointY) / (18 * 5));
		short column_index = (short) (distance2Coordinate(this.geography, this.TopLeftY, this.TopLeftX, pointX, this.TopLeftX) / (18 * 5));
		if (row_index == index_x && column_index == index_y) {
			return true;
		}
    	return false;
    }
    
	
   	public void record_routing_information(int current_time, double d, int routing_result, int curr_batch, int turn_point_size) {
		
		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
		
	    String uav_routing_time_report = temp_pre_fix + File.separator + "report" + File.separator + "batch_" + Integer.toString(curr_batch) + File.separator + "uav_routing_time_report" + ".csv";
		try {
			PrintWriter writer_uav_routing_time_report = new PrintWriter(new FileOutputStream(new File(uav_routing_time_report),true));
			StringBuilder uav_routing_time_report_string = new StringBuilder();
			uav_routing_time_report_string.append(current_time);
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append(d);
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append(routing_result);
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append(turn_point_size);
			uav_routing_time_report_string.append('\n');
			writer_uav_routing_time_report.write(uav_routing_time_report_string.toString());
			writer_uav_routing_time_report.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    
	private Geography<Object> geography;
    private int ConflictThreshold;
    private int internal_time_step;
    private int max_time_allow_check;
    private double TopLeftX;
    private double TopLeftY;
    private double BottomRightX;
    private double BottomRightY;
    
    private int ROW;
    private int COLUMN;
    private int TIME;
    
    private byte[][] UAVCondition;
    private HashMap<Integer, HashMap<Integer, Byte>> UAVTrajectory;
    private List<VirtualBasestation> virtual_base_stations = new ArrayList<VirtualBasestation>();
    
    private int yDigit = 1000;
    
    // Below arrays details all 4 possible movements from a cell
    private static final int row[] = { -1, 0, 0, 1};
    private static final int col[] = { 0, -1, 1, 0};
    private static final int time[] = { 1, 1, 1, 1};
		
}


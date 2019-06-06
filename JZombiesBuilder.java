package jzombies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Scanner;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.io.FileUtils;


public class JZombiesBuilder implements ContextBuilder<Object> {

	public String copy_folders_local_to_temp() {
	    String line = "";
		try (BufferedReader br = new BufferedReader(new FileReader(".." + File.separator + "config.props"))) {
		    while ((line = br.readLine()) != null) {
		    	if (line.contains("model.archive")) {
		    		break;
		    	}
		    }
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		line = line.substring(line.indexOf("=") + 2, line.indexOf("output"));
		System.out.println("**********************************************************************************************");
		System.out.println("Copying local to temp: " + line);
		System.out.println("**********************************************************************************************");
		String source = line + "configuration";
		File srcDir = new File(source);

		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
		destination = temp_pre_fix + File.separator + "configuration";
		
		File destDir = new File(destination);

		try {
		    FileUtils.copyDirectory(srcDir, destDir);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return temp_pre_fix;			
	}
	
	public void copy_folders_temp_to_local(int current_batch) {
	    String line = "";
		try (BufferedReader br = new BufferedReader(new FileReader(".." + File.separator + "config.props"))) {
		    while ((line = br.readLine()) != null) {
		    	if (line.contains("model.archive")) {
		    		break;
		    	}
		    }
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		line = line.substring(line.indexOf("=") + 2, line.indexOf("output"));
		System.out.println("**********************************************************************************************");
		System.out.println("Copying temp to local: " + line);
		System.out.println("**********************************************************************************************");
		String destination = line + "report";
		File destDir = new File(destination);

		String source = System.getProperty("user.dir");
		int last_indes = source.lastIndexOf(File.separator);
		String temp_pre_fix = source.substring(0, last_indes);
		source = temp_pre_fix + File.separator + "report" + File.separator + "batch_" + current_batch;
		
		File srcDir = new File(source);

		try {
		    FileUtils.copyDirectory(srcDir, destDir);
		} catch (IOException e) {
		    e.printStackTrace();
		}			
	}
	
	@Override
	public Context build(Context<Object> context) {
		String temp_destination = copy_folders_local_to_temp();
		// TODO Auto-generated method stub
		context.setId("jzombies");
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
        //////////////////////////////////////////////////////////////////
        //Initialize parameters

        Parameters params = RunEnvironment.getInstance().getParameters();

        //boolean if_Config_from_File = params.getBoolean("Config_from_File");

        long UAVCount;
        int simulation_time;
        boolean if_different_start_time;
        boolean if_straight;
        long targetCount;
        long basestationCount;
        long base_station_channels;
        int numberofruntime;
        int uav_generation_rate;
        int current_random;
        boolean if_routing;

        //if (if_Config_from_File) {
        //UAVCount = mytool.read_uav_count_JSON();
        //if_different_start_time = mytool.read_if_different_start_time_JSON();
        ////
        ////targetCount = mytool.read_target_count_JSON();
        ////
        //basestationCount = mytool.read_basestation_count_JSON();
        //base_station_channels = mytool.read_channel_count_JSON();
        //}
        //else {
        UAVCount = 0;//params.getInteger("UAVCount");
        simulation_time = params.getInteger("simulation_time");
        if_different_start_time = true; //params.getBoolean("if_different_start_time");
        if_routing = true;

        if (params.getString("if_straight").toLowerCase().equals("straight")) {
            if_straight = true;
        } else {
        	if_straight = false;
        }
        
        if (params.getString("if_routing").toLowerCase().equals("yes")) {
        	if_routing = true;
        } else {
        	if_routing = false;
        }

        basestationCount = 10; //params.getInteger("basestationCount");
        base_station_channels = params.getInteger("ChannelsCount");
        numberofruntime = params.getInteger("NumberOfRunTime");
        uav_generation_rate = params.getInteger("UAVsGenerationRate");
        current_random = params.getInteger("randomSeed");

        //}
        //////////////////////////////////////////////////////////////////
		
	
	    //////////////////////////////////////////////////////////////////
	    // initialize record		

        create_folder(temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime));

        String fileName_simulation_parameters  = temp_destination + File.separator + "report" + File.separator + "simulation_parameters" + ".csv";
	    String fileName_basestation = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "basestation" + ".csv";
	    String fileName_mission = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "mission_report" + ".csv";
	    String fileName_uav_index = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "uav_index" + ".csv";
	    String fileName_uav_coordinate = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "uav_coordinate" + ".csv";
	    String fileName_uav_count_report = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "uav_count_report" + ".csv";
	    String fileName_uav_routing_time_report = temp_destination + File.separator + "report" + File.separator + "batch_" + Integer.toString(numberofruntime) + File.separator + "uav_routing_time_report" + ".csv";
	    String fileName_random_seed_report = temp_destination + File.separator + "report" + File.separator + "random_seed_report" + ".csv";
	   
    	
		try {
			PrintWriter writer_simulation_parameters = new PrintWriter(new FileOutputStream(new File(fileName_simulation_parameters), true));
			PrintWriter writer_basestation = new PrintWriter(new File(fileName_basestation));
			PrintWriter writer_mission = new PrintWriter(new File(fileName_mission));
			PrintWriter writer_uav_index = new PrintWriter(new File(fileName_uav_index));
			PrintWriter writer_uav_coordinate = new PrintWriter(new File(fileName_uav_coordinate));
			PrintWriter writer_uav_count_report = new PrintWriter(new File(fileName_uav_count_report));
			PrintWriter writer_uav_routing_time_report = new PrintWriter(new File(fileName_uav_routing_time_report));
			PrintWriter writer_random_seed_report = new PrintWriter(new File(fileName_random_seed_report));
			
			StringBuilder run_string = new StringBuilder();
			StringBuilder basestations_string = new StringBuilder();
			StringBuilder channels_string = new StringBuilder();
			StringBuilder generation_rate_string = new StringBuilder();
			StringBuilder routing_string = new StringBuilder();
			StringBuilder trajectory_type_string = new StringBuilder();
			
			run_string.append("run");
			run_string.append(',');
			run_string.append(numberofruntime);
			run_string.append('\n');
			writer_simulation_parameters.write(run_string.toString());
			
			basestations_string.append("basestations");
			basestations_string.append(',');
			basestations_string.append(basestationCount);
			basestations_string.append('\n');
			writer_simulation_parameters.write(basestations_string.toString());
			
			channels_string.append("channels");
			channels_string.append(',');
			channels_string.append(base_station_channels);
			channels_string.append('\n');
			writer_simulation_parameters.write(channels_string.toString());
			
			generation_rate_string.append("generation_rate");
			generation_rate_string.append(',');
			generation_rate_string.append(uav_generation_rate);
			generation_rate_string.append('\n');
			writer_simulation_parameters.write(generation_rate_string.toString());
			
			if (if_routing) {
				routing_string.append("routing");
				routing_string.append(',');
				routing_string.append("yes");
				routing_string.append('\n');
				writer_simulation_parameters.write(routing_string.toString());
			} else {
				routing_string.append("routing");
				routing_string.append(',');
				routing_string.append("no");
				routing_string.append('\n');
				writer_simulation_parameters.write(routing_string.toString());
			}
			if (if_straight) {
				trajectory_type_string.append("trajectory_type");
				trajectory_type_string.append(',');
				trajectory_type_string.append("straight");
				trajectory_type_string.append('\n');
				writer_simulation_parameters.write(trajectory_type_string.toString());
			} else {
				trajectory_type_string.append("trajectory_type");
				trajectory_type_string.append(',');
				trajectory_type_string.append("manhattan");
				trajectory_type_string.append('\n');
				writer_simulation_parameters.write(trajectory_type_string.toString());
			}
			
			StringBuilder uav_count_report_string = new StringBuilder();
			StringBuilder uav_routing_time_report_string = new StringBuilder();
			StringBuilder basestation_string = new StringBuilder();
			StringBuilder mission_string = new StringBuilder();
			StringBuilder uav_index_string = new StringBuilder();
			StringBuilder uav_coordinate_string = new StringBuilder();
			
			uav_count_report_string.append("Time_Step");
			uav_count_report_string.append(',');
			uav_count_report_string.append("The_Number_of_UAVs");
			uav_count_report_string.append('\n');
			writer_uav_count_report.write(uav_count_report_string.toString());
			
			uav_routing_time_report_string.append("Time_Step");
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append("Routing_Time_of_UAVs");
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append("Routing_Result");
			uav_routing_time_report_string.append(',');
			uav_routing_time_report_string.append("Number_of_Turn_point");
			uav_routing_time_report_string.append('\n');
			writer_uav_routing_time_report.write(uav_routing_time_report_string.toString());
			
			basestation_string.append("Time_Step");
			basestation_string.append(',');
			basestation_string.append("Basestation_ID");
			basestation_string.append(',');
			basestation_string.append("The_Number_of_Occupied_Channels");
			basestation_string.append('\n');
			writer_basestation.write(basestation_string.toString());
			
			mission_string.append("Mission_ID");
			mission_string.append(',');
			mission_string.append("Mission_Type");
			mission_string.append(',');
			mission_string.append("UAV_ID");
			mission_string.append(',');
			mission_string.append("Start_Time");
			mission_string.append(',');
			mission_string.append("End_Time");
			mission_string.append(',');
			mission_string.append("Flight_Time");
			mission_string.append(',');
			mission_string.append("Start_Location");
			mission_string.append(',');
			mission_string.append("Connection_Location");
			mission_string.append(',');
			mission_string.append("End_Location");
			mission_string.append('\n');
			writer_mission.write(mission_string.toString());
			
			uav_index_string.append("Time_Step");
			uav_index_string.append(',');
			uav_index_string.append("UAV_ID");
			uav_index_string.append(',');
			uav_index_string.append("X_Distance");
			uav_index_string.append(',');
			uav_index_string.append("Y_Distance");
			uav_index_string.append(',');
			uav_index_string.append("Signal_strength");
			uav_index_string.append(',');
			uav_index_string.append("Current_Basestation");
			uav_index_string.append(',');
			uav_index_string.append("Finished");
			uav_index_string.append('\n');
			writer_uav_index.write(uav_index_string.toString());
			
			uav_coordinate_string.append("Time_Step");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("UAV_ID");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("Latitude");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("Longitude");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("Signal_strength");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("Current_Basestation");
			uav_coordinate_string.append(',');
			uav_coordinate_string.append("Finished");
			uav_coordinate_string.append('\n');
			writer_uav_coordinate.write(uav_coordinate_string.toString());
			
			
			writer_simulation_parameters.close();
			writer_uav_count_report.close();
			writer_uav_routing_time_report.close();
			writer_basestation.close();
			writer_mission.close();
			writer_uav_index.close();
			writer_uav_coordinate.close();
			writer_random_seed_report.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }		
		
		record_random_seed(numberofruntime, current_random);
        //////////////////////////////////////////////////////////////////	
		
		GeographyParameters<Object> params_geo = new GeographyParameters<Object>();
		GeographyFactory geo_factory = GeographyFactoryFinder.createGeographyFactory(null);
		
		
		Geography<Object> geography = geo_factory.createGeography("geography", context, params_geo);
		
		GeometryFactory fac = new GeometryFactory();
		
		
		String mode = "load";
		
		TransportNetwork my_transport_network = new TransportNetwork(mode);
		Map<String, ArrayList<Double>> airport_list = my_transport_network.return_airport_list();
		Operator my_operator = new Operator(mode);
		
        Util mytool = new Util();
        
        Map<String, terrain_node> terrain_start_location = mytool.terrain_start_location_config();
        
        Map<String, terrain_node> terrain_end_location = mytool.terrain_end_location_config();
		
//        mytool.print_trainport_network_config(my_transport_network.return_airport_list());
//        mytool.print_trainport_network_connection(my_transport_network.return_airport_connection());
        
//        mytool.print_start_destination_list(my_transport_network.return_start_destination_list());
//        
//        mytool.print_mission_list(my_operator.return_mission_list());
//        
//        ArrayList<String> missions = new ArrayList();
//        missions.add("book delivery erie walmart");
//        missions.add("book delivery fairmount walmart");
//        missions.add("food delivery fayetteville wegmans");
//        missions.add("food delivery onondaga wegmans");
//        missions.add("paper delivery syracuse university");
//        missions.add("home delivery nobhill apartment");
//        missions.add("product delivery fayetteville onondaga wegmans");
//        missions.add("product delivery erie fairmount walmart");
//        missions.add("payment send onondaga wegmans");
//        missions.add("payment send fayetteville wegmans");
//        for (String name : missions) {
//        	mytool.print_specific_mission_node(mytool.load_specific_operation(name));
//        }
		
        
//////////////////////////////////////////////////////////////////
//add all the elements
		ArrayList<UAV> uavs_list = new ArrayList<UAV>();
		try {
			uavs_list = read_operation(geography, if_straight, numberofruntime);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //////////////////////////////////////////////////////////////////
        //add UAVs

//		for (String name : missions) {
//			
//			Random r_1 = new Random(); 
//			double low = 27;
//			double high = 45;
//			double speed = (high - low) * r_1.nextDouble() + low; 
//			
//			Random r_2 = new Random();
//			int min = 1;
//			int max = 1001;
//			int start_time = r_2.nextInt(max-min) + min;
//			
//			if (!if_different_start_time) {
//				start_time = 0;
//			}
//			uavs_list.add(new UAV(geography, 400, speed, if_straight, start_time, 0, 0, true, name
//					, my_transport_network.return_airport_list(), my_operator, numberofruntime));
//
//		} 
//		
//		for (int i = 0; i < UAVCount; i++) {
//			
//			Random r_1 = new Random(); 
//			double low = 27;
//			double high = 45;
////			double speed = (high - low) * r_1.nextDouble() + low;
//			double speed = 18;
//			
//			Random r_2 = new Random();
//			int min = 1;
//			int max = 1001;
//			int start_time = r_2.nextInt(max-min) + min;
//			
//			if (!if_different_start_time) {
//				start_time = 0;
//			}
//			uavs_list.add(new UAV(geography, 400, speed, if_straight, start_time, 0, 0, true, "all_random"
//					, my_transport_network.return_airport_list(), numberofruntime));
//
//		} 

        //////////////////////////////////////////////////////////////////	
		
        //////////////////////////////////////////////////////////////////
        //Add Targets
		for (int i = 0; i < airport_list.size(); i++) {
			context.add(new Target(geography));
		}
        //////////////////////////////////////////////////////////////////

        //////////////////////////////////////////////////////////////////
        //add BaseStations
		for (int i = 0; i < basestationCount; i++) {
			context.add(new Base_Station(geography, base_station_channels, 0, 0, 0, numberofruntime, i));
		}
        //////////////////////////////////////////////////////////////////

		
//////////////////////////////////////////////////////////////////	

//////////////////////////////////////////////////////////////////
// Move element to the position
		
        //////////////////////////////////////////////////////////////////
        // Move all the elements
		for (Object obj : context) {
			
//			Geometry pt = geography.getGeometry(obj);

			Coordinate coord = new Coordinate(0, 0);
			Point geom =  fac.createPoint(coord);
			geography.move(obj, geom);
			
		}
        //////////////////////////////////////////////////////////////////
		
		
        //////////////////////////////////////////////////////////////////
        // Move Targets
		List<Object> Target_collection = new ArrayList<Object>();
		for (Object obj : geography.getAllObjects()) { // grid.getObjects()
			if (obj instanceof Target) {
				Target_collection.add(obj);
			}
		}
		
		Collection<ArrayList<Double>> airport_coordination_set = airport_list.values();
		
		ArrayList<ArrayList<Double>> airport_coordination_list = new ArrayList<ArrayList<Double>>();
		
		for (ArrayList<Double> temp : airport_coordination_set) {
			airport_coordination_list.add(temp);
		}

		for (int target_index = 0; target_index < Target_collection.size(); target_index++) {
			
			Coordinate coord_target = new Coordinate(airport_coordination_list.get(target_index).get(0), airport_coordination_list.get(target_index).get(1));
			Point geom_target =  fac.createPoint(coord_target);
			geography.move(Target_collection.get(target_index), geom_target);
			
		}
        //////////////////////////////////////////////////////////////////
		
        //////////////////////////////////////////////////////////////////
        // Move basestations
		List<Object> basestation_collection = new ArrayList<Object>();

		for (Object obj : geography.getAllObjects()) { // grid.getObjects()
			if (obj instanceof Base_Station) {
				basestation_collection.add(obj);
			}
		}

			Coordinate coord_basestation_0 = new Coordinate(-76.0983752, 43.0540116);
			Point geom_basestation_0 =  fac.createPoint(coord_basestation_0);
			geography.move(basestation_collection.get(0), geom_basestation_0);
			
			Coordinate coord_basestation_1 = new Coordinate(-76.108094, 43.015082);
			Point geom_basestation_1 =  fac.createPoint(coord_basestation_1);
			geography.move(basestation_collection.get(1), geom_basestation_1);
			
			Coordinate coord_basestation_2 = new Coordinate(-76.1357863, 43.0462901);
			Point geom_basestation_2 =  fac.createPoint(coord_basestation_2);
			geography.move(basestation_collection.get(2), geom_basestation_2);
			
			Coordinate coord_basestation_3 = new Coordinate(-76.1722502, 43.0685097);
			Point geom_basestation_3 =  fac.createPoint(coord_basestation_3);
			geography.move(basestation_collection.get(3), geom_basestation_3);
			
			Coordinate coord_basestation_4 = new Coordinate(-76.1904584, 43.0101927);
			Point geom_basestation_4 =  fac.createPoint(coord_basestation_4);
			geography.move(basestation_collection.get(4), geom_basestation_4);
			
			Coordinate coord_basestation_5 = new Coordinate(-76.1277564, 42.9987133);
			Point geom_basestation_5 =  fac.createPoint(coord_basestation_5);
			geography.move(basestation_collection.get(5), geom_basestation_5);
			
			Coordinate coord_basestation_6 = new Coordinate(-76.0561521, 43.039665);
			Point geom_basestation_6 =  fac.createPoint(coord_basestation_6);
			geography.move(basestation_collection.get(6), geom_basestation_6);
			
			Coordinate coord_basestation_7 = new Coordinate(-76.1319434, 43.0737731);
			Point geom_basestation_7 =  fac.createPoint(coord_basestation_7);
			geography.move(basestation_collection.get(7), geom_basestation_7);
			
			Coordinate coord_basestation_8 = new Coordinate(-76.221530, 43.061099);
			Point geom_basestation_8 =  fac.createPoint(coord_basestation_8);
			geography.move(basestation_collection.get(8), geom_basestation_8);
			
			Coordinate coord_basestation_9 = new Coordinate(-76.224270, 43.026221);
			Point geom_basestation_9 =  fac.createPoint(coord_basestation_9);
			geography.move(basestation_collection.get(9), geom_basestation_9);
			
//			Coordinate coord_basestation_10 = new Coordinate(-76.155671, 43.053733);
//			Point geom_basestation_10 =  fac.createPoint(coord_basestation_10);
//			geography.move(basestation_collection.get(10), geom_basestation_10);
//			
//			Coordinate coord_basestation_11 = new Coordinate(-76.219673, 43.072004);
//			Point geom_basestation_11 =  fac.createPoint(coord_basestation_11);
//			geography.move(basestation_collection.get(11), geom_basestation_11);
//			
//			Coordinate coord_basestation_12 = new Coordinate(-76.058747, 43.060882);
//			Point geom_basestation_12 =  fac.createPoint(coord_basestation_12);
//			geography.move(basestation_collection.get(12), geom_basestation_12);
//			
//			Coordinate coord_basestation_13 = new Coordinate(-76.068295, 42.989459);
//			Point geom_basestation_13 =  fac.createPoint(coord_basestation_13);
//			geography.move(basestation_collection.get(13), geom_basestation_13);
//			
//			Coordinate coord_basestation_14 = new Coordinate(-76.180916, 43.042607);
//			Point geom_basestation_14 =  fac.createPoint(coord_basestation_14);
//			geography.move(basestation_collection.get(14), geom_basestation_14);
//			
//			Coordinate coord_basestation_15 = new Coordinate(-76.021167, 43.043436);
//			Point geom_basestation_15 =  fac.createPoint(coord_basestation_15);
//			geography.move(basestation_collection.get(15), geom_basestation_15);
//			
//			Coordinate coord_basestation_16 = new Coordinate(-76.227590, 43.012104);
//			Point geom_basestation_16 =  fac.createPoint(coord_basestation_16);
//			geography.move(basestation_collection.get(16), geom_basestation_16);
//			
//			Coordinate coord_basestation_17 = new Coordinate(-76.149936, 43.005875);
//			Point geom_basestation_17 =  fac.createPoint(coord_basestation_17);
//			geography.move(basestation_collection.get(17), geom_basestation_17);
//			
//			Coordinate coord_basestation_18 = new Coordinate(-76.049454, 43.016090);
//			Point geom_basestation_18 =  fac.createPoint(coord_basestation_18);
//			geography.move(basestation_collection.get(18), geom_basestation_18);
//			
//			Coordinate coord_basestation_19 = new Coordinate(-76.083820, 43.010624);
//			Point geom_basestation_19 =  fac.createPoint(coord_basestation_19);
//			geography.move(basestation_collection.get(19), geom_basestation_19);

        //////////////////////////////////////////////////////////////////
			
	        //////////////////////////////////////////////////////////////////
	        //Add UAVmanagement
			UAVmanagement my_uav_management = new UAVmanagement(geography, fac, uavs_list, airport_list, terrain_start_location, terrain_end_location, numberofruntime, uav_generation_rate, if_straight, if_routing, simulation_time);
//			my_uav_management.set_block_area((short)40, (short)70, (short)60, (short)100, simulation_time);
//			my_uav_management.set_block_area((short)55, (short)90, (short)104, (short)124, simulation_time);
//			my_uav_management.set_block_area((short)60, (short)100, (short)0, (short)16, simulation_time);
//			my_uav_management.set_block_area((short)0, (short)30, (short)0, (short)16, simulation_time);
			context.add(my_uav_management);
	        //////////////////////////////////////////////////////////////////
			
		if (RunEnvironment.getInstance().isBatch()) {
//			RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			RunEnvironment.getInstance().endAt(simulation_time);
		}
		
		return context;
//////////////////////////////////////////////////////////////////
	}
	
    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';

    public static List<String> parseLine(String cvsLine) {
        return parseLine(cvsLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    public static List<String> parseLine(String cvsLine, char separators) {
        return parseLine(cvsLine, separators, DEFAULT_QUOTE);
    }

    public static List<String> parseLine(String cvsLine, char separators, char customQuote) {

        List<String> result = new ArrayList<>();

        //if empty, return!
        if (cvsLine == null && cvsLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;

        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {

            if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {

                    //Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }

                }
            } else {
                if (ch == customQuote) {

                    inQuotes = true;

                    //Fixed : allow "" in empty quote enclosed
                    if (chars[0] != '"' && customQuote == '\"') {
                        curVal.append('"');
                    }

                    //double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }

                } else if (ch == separators) {

                    result.add(curVal.toString());

                    curVal = new StringBuffer();
                    startCollectChar = false;

                } else if (ch == '\r') {
                    //ignore LF characters
                    continue;
                } else if (ch == '\n') {
                    //the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }

        }

        result.add(curVal.toString());

        return result;
    }
	
    public ArrayList<UAV> read_operation(Geography<Object> geography, boolean if_straight, int current_batch) throws FileNotFoundException {
    	ArrayList<UAV> uavs_list = new ArrayList<UAV>();
    	
		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
    	
        Scanner scanner = new Scanner(new File(temp_pre_fix + File.separator + "configuration" + File.separator + "operation.csv"));
        scanner.useDelimiter(",");
        // remove the first line
        if(scanner.hasNext()) {
            scanner.nextLine();
        }
        // get all operations
        while(scanner.hasNext()) {
            List<String> line = parseLine(scanner.nextLine());
            System.out.println("Operation [id= " + line.get(0) +
                               ", start_x= " + line.get(1) + " , start_y=" + line.get(2) +
                               ", end_x= " + line.get(3) + ", end_x= " + line.get(4) +
                               ", height= " + line.get(5) +
                               ", speed= " + line.get(6) + 
                               ", start_time= " + line.get(7) +"]");
			UAV temp_uav = new UAV(geography, Double.valueOf(line.get(5)), Double.valueOf(line.get(6)), if_straight, Integer.valueOf(line.get(7)), 0, 0, true, "probability", null, current_batch);
			temp_uav.set_internal_time(0);
			
			ArrayList<Double> start_coordinate_pair = new ArrayList<Double>();
			start_coordinate_pair.add(Double.valueOf(line.get(1)));
			start_coordinate_pair.add(Double.valueOf(line.get(2)));
			
            ArrayList<Double> end_coordinate_pair = new ArrayList<Double>();
            end_coordinate_pair.add(Double.valueOf(line.get(3)));
            end_coordinate_pair.add(Double.valueOf(line.get(4)));
			
            ArrayList<ArrayList<Double>> connection_coordinate_pair = new ArrayList<ArrayList<Double>>();
            ArrayList<ArrayList<Double>> connection_index_pair = new ArrayList<ArrayList<Double>>();
            
            temp_uav.set_start_coordinate_pair(start_coordinate_pair);
            temp_uav.set_connection_coordinate_pair(connection_coordinate_pair);
            temp_uav.set_connection_index_pair(connection_index_pair);
            temp_uav.set_end_coordinate_pair(end_coordinate_pair);
            uavs_list.add(temp_uav);
        }
        scanner.close();
        return uavs_list;
    }
	
	private void create_folder(String folder_name) {
		File theDir = new File(folder_name);
        System.out.println(folder_name);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    System.out.println("creating directory: " + theDir.getName());
		    boolean result = false;

		    try{
		        theDir.mkdirs();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
	}
	
	void record_random_seed(int current_batch, int random_seed) {
		
		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
		
	    String uav_count_report = temp_pre_fix + File.separator + "report" + File.separator + "random_seed_report" + ".csv";
	    StringBuilder uav_count_report_string = new StringBuilder();

		try {
			PrintWriter writer_random_seed_report = new PrintWriter(new FileOutputStream(new File(uav_count_report),true));
			uav_count_report_string.append(current_batch);
			uav_count_report_string.append(',');
			uav_count_report_string.append(random_seed);
			uav_count_report_string.append('\n');
			writer_random_seed_report.write(uav_count_report_string.toString());

	        writer_random_seed_report.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

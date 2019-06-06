package jzombies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.grid.Grid;

public class Base_Station {
	
	private Geography<Object> geography;
	
//  Base Station attributions
	private long num_of_channels;
	private double antenna_height;
	private double tx_power;
	private double rx_threshold;
    private ArrayList<UAV> current_channels;
	private int internal_time_step;
	private int current_batch;
	private int bs_id;
	
	public Base_Station(Geography<Object> geography,
			            long base_station_channels, double antenna_height, double tx_power, double rx_threshold, int current_batch, int bs_id){
		this.geography = geography;
		this.num_of_channels = base_station_channels;
		this.antenna_height = antenna_height;
		this.tx_power = tx_power;
		this.rx_threshold = rx_threshold;
		this.current_channels = new ArrayList<UAV>();
		this.internal_time_step = -1;
		this.current_batch = current_batch;
		this.bs_id = bs_id;
	}
	
	public long get_num_of_channels() {
		return this.num_of_channels;
	}
	
	public int return_bs_id() {
		return this.bs_id;
	}
	
	public boolean add_UAV(UAV uav) {
		if (current_channels.contains(uav)) {
			return false;
		} else {
			return current_channels.add(uav);
		}
	}
	
	public void remove_UAV(UAV uav) {
		current_channels.remove(uav);
	}
	
	public int get_current_num_UAV() {
		return current_channels.size();
	}
   
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		this.internal_time_step++;
		save_location();
	}
	
    // save Basestation location in each time tick
    private void save_location() {
    	int id = this.bs_id;
    	
		String destination = System.getProperty("user.dir");
		int last_indes = destination.lastIndexOf(File.separator);
		String temp_pre_fix = destination.substring(0, last_indes);
		
    	String fileName= temp_pre_fix + File.separator + "report" + File.separator + "batch_" + Integer.toString(current_batch) + File.separator + "basestation" + ".csv";
    	Geometry myPoint = geography.getGeometry(this);
    	try {
    		PrintWriter writer_basestation = new PrintWriter(new FileOutputStream(new File(fileName),true));
    		StringBuilder basestations_string = new StringBuilder();
    		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    		String time = format.format(new Date());
    		basestations_string.append(internal_time_step);
    		basestations_string.append(',');
    		basestations_string.append(id);
    		basestations_string.append(',');
    		basestations_string.append(this.current_channels.size());
    		basestations_string.append('\n');
    		writer_basestation.write(basestations_string.toString());
    		writer_basestation.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
}

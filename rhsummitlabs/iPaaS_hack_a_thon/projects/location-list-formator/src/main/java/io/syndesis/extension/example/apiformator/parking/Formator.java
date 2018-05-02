package io.syndesis.extension.example.apiformator.parking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class Formator {
	
	public static final String PARKING = "Parking";
	public static final String ATM = "ATM";
	public static final String STORE = "Store";
	public static final String BAR = "Bar";
	public static final String RESTARUANT = "Restaruant";
	
	
	List<Map<String,Object>> allparkinglocation = new ArrayList<Map<String,Object>>();
	
	public List<Map<String,Object>> parseList(String locationtype, String body) {
		System.out.println("parseList!!!!!!--->");
		allparkinglocation = new ArrayList<Map<String,Object>>();
		System.out.println("locationtype---->["+locationtype+"]");;
		
		DocumentContext context = JsonPath.parse(body);
		List<Map<String, Object>> allAPIlocations = context.read("$");
		
		for(Map<String, Object> apilocation: allAPIlocations) {
			String id = (String) apilocation.get("ID");
			Double lat = Double.valueOf(apilocation.get("LAT").toString());
			Double lng = Double.valueOf(apilocation.get("LNG").toString());
			
			String title = "PLACENAME";
			if(Formator.PARKING.equalsIgnoreCase(locationtype))
				title = apilocation.get("PLACENAME").toString();
			else if(Formator.ATM.equalsIgnoreCase(locationtype))
				title = apilocation.get("BANKNAME").toString();
			else if(Formator.BAR.equalsIgnoreCase(locationtype))
				title = apilocation.get("BARNAME").toString();
			else if(Formator.STORE.equalsIgnoreCase(locationtype))
				title = apilocation.get("STORENAME").toString();
			else if(Formator.RESTARUANT.equalsIgnoreCase(locationtype))
				title = apilocation.get("PLACENAME").toString();
			
			addList(lat,lng,title,id,locationtype);
		}
		
		return allparkinglocation;
		
	}
	
	private void addList(double lat, double lng, String title, String id, String locationtype) {
		Map<String,Object> locationlist = new HashMap<String,Object>();
		Map<String,Double> thelocation = new HashMap<String,Double>();
		
		thelocation.put("lat", lat);
		thelocation.put("lng", lng);
		
		locationlist.put("location", thelocation);
		locationlist.put("title", title);
		locationlist.put("type", locationtype);
		locationlist.put("id", id);
		
		allparkinglocation.add(locationlist);
	}
	
	
}

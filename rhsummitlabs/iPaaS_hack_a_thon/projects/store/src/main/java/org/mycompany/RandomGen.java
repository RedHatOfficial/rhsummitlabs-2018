package org.mycompany;

import java.util.Random;

public class RandomGen {
	
	public String getAlcholTypes() {
		String alchols="Vodka, Gin, Whiskey,  Rum, Wine, Beer";
		String[] optionaltype = {"Cider", "Stout", "Jack Daniels"};
		
		int cnt0 = randInt(0, 1);
		if (cnt0 == 1)
			alchols += (","+optionaltype[0]);
			
		int cnt1 = randInt(0, 1);
		if (cnt1 == 1)
			alchols += (","+optionaltype[1]);
		
		int cnt2 = randInt(0, 1);
		if (cnt2 == 1)
			alchols += (","+optionaltype[2]);
		
		return alchols;
	}
	
	public String getMusicType() {
		
		String[] musictype = {
				"alternative",
				"metallica",
				"acoustic",
				"jazz",
				"blues",
				"classical",
				"live band",
				"country",
				"easy Listening",
				"electronic",
				"hip-hop",
				"R&B",
				"soul",
				"rock",
				"world"
		};
		
		return musictype[randInt(0, 14)];
	} 
	
	public int randparking() {
		return this.randInt(0, 200);
	}
	
	private int randInt(int min, int max) {


		Random ran = new Random();
	    int randomNum = ran.nextInt((max - min) + 1) + min;

	    return randomNum;
	}

}

package org.mycompany;

import java.util.Random;

public class RandomGen {
	
	public String addescapeChar(String val) {
		String newadd = val;
		if(val.contains("'")) {
			newadd = val.replace("'", "");
			//System.out.println("newadd------->"+newadd);
		}
		return newadd;
	}
	
	public double randprice() {
		
		double[] prices = {
				4,
				4.5,
				5,
				5.5,
				6,
				6,5,
				7
		};
		
		return prices[randInt(0,6)];
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

package org.mycompany;

import java.util.Random;

public class RandomGen {
	
	public int randparking() {
		return this.randInt(0, 200);
	}
	
	private int randInt(int min, int max) {


		Random ran = new Random();
	    int randomNum = ran.nextInt((max - min) + 1) + min;

	    return randomNum;
	}

}

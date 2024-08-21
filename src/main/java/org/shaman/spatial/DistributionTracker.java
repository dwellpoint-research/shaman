package org.shaman.spatial;

import java.util.Comparator;
import java.util.TreeSet;
import weka.core.Instance;

public class DistributionTracker {
	int [][] distributionOverGeneration = new int[SpatialEnsembleLearning.EPOCHS][10];
	double [][] weightOverGeneration = new double[SpatialEnsembleLearning.EPOCHS][10];
	
	public DistributionTracker() {
		//initialize the arrays
		for(int i =0; i< SpatialEnsembleLearning.EPOCHS; i++ ){
			for(int j=0; j< 10; j++){
				distributionOverGeneration[i][j]=0;
				weightOverGeneration[i][j]=0;
			}
		}
	}
	

	
	public void weightDistribution(double value, int epoch, double weight) {
		
		int bin =0;
		//circle
//		if(value > 0.1 && value <= 0.2)
//			bin= 1;
//		else if(value > 0.2 && value <=0.3)
//			bin= 2;
//		else if(value > 0.3 && value <= 0.4)
//			bin= 3;
//		else if(value > 0.4 && value <= 0.5)
//			bin= 4;
//		else if(value > 0.5 && value <= 0.6)
//			bin= 5;
//		else if(value > 0.6 && value <= 0.7)
//			bin= 6;
//		else if(value > 0.7 && value <= 0.8)
//			bin= 7;
//		else if(value > 0.8 && value <= 0.9)
//			bin= 8;
//		else if(value > 0.9 && value <= 1.0)
//			bin= 9;
		//gaussian
		if(value > 2 && value <= 6)
			bin= 1;
		else if(value > 6 && value <=10)
			bin= 2;
		else if(value > 10 && value <= 14)
			bin= 3;
		else if(value > 14 && value <= 18)
			bin= 4;
		else if(value > 18 && value <= 22)
			bin= 5;
		else if(value > 22 && value <= 26)
			bin= 6;
		else if(value > 26 && value <= 30)
			bin= 7;
		else if(value > 30 && value <= 34)
			bin= 8;
		else if(value > 34 && value <= 38)
			bin= 9;

//		if(value >= 0.4 && value < 0.8)
//			bin= 1;
//		else if(value >= 0.8 && value < 1.2)
//			bin= 2;
//		else if(value >= 1.2 && value < 1.6)
//			bin= 3;
//		else if(value >= 1.6 && value < 2.0)
//			bin= 4;
//		else if(value >= 2.0 && value < 2.4)
//			bin= 5;
//		else if(value >= 2.4 && value < 2.8)
//			bin= 6;
//		else if(value >= 2.8 && value < 3.2)
//			bin= 7;
//		else if(value >= 3.2 && value < 3.6)
//			bin= 8;
//		else if(value >= 3.6 && value < 4.0)
//			bin= 9;
		weightOverGeneration[epoch][bin] += weight;
		this.distributionOverGeneration[epoch][bin] ++;
	}
	
	
	public void printDistribution(){
		for(int i =0; i<SpatialEnsembleLearning.EPOCHS; i++ ){
			System.err.print(this.distributionOverGeneration[i][0]);
			for(int j=1; j< 10; j++){
				System.err.print("\t"+this.distributionOverGeneration[i][j]);
			}
			System.err.println();
		}
		
		System.err.println("---------------------------------------------------------------");
		for(int i =0; i< SpatialEnsembleLearning.EPOCHS; i++ ){
			System.err.print(this.weightOverGeneration[i][0]);
			for(int j=1; j< 10; j++){
				System.err.print("\t"+this.weightOverGeneration[i][j]);
			}
			System.err.println();
		}
	}

}

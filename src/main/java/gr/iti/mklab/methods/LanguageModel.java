package gr.iti.mklab.methods;

import gr.iti.mklab.geo.GeoCell;
import gr.iti.mklab.util.CellCoder;
import gr.iti.mklab.util.EasyBufferedReader;
import gr.iti.mklab.util.MyHashMap;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * It is the implementation of the language model. Here the term-cell probabilities are loaded and all calculation for the estimated location take place.
 * The model calculate the cell probabilities summing up the term-cell probabilities for every different cell based on the terms that are contained in the query sentence.
 * 		  S
 * p(c) = Σ p(t|c)*(N(e)
 * 		 t=1
 * The cell with that maximizes this summation considering as the Most Likely Cell for the query sentence.
 * @author gkordo
 *
 */
public class LanguageModel {

	protected Map<String,Map<Long,Double>> termCellProbsMap;

	protected Map<String,Double[]> termWeights;
	protected NormalDistribution gdWeight;
	
	protected static double confRange;
	protected static int confItemNumber;

	private static Logger logger = Logger.getLogger("gr.iti.mklab.methods.LanguageModel");

	// The function that compose the other functions to calculate and return the Most Likely Cell for a query tweet.
	public GeoCell calculateLanguageModel(Set<String> sentence) {

		Map<Long, GeoCell> cellMap = calculateCellsProbForImageTags(sentence);

		GeoCell mlc = findMLC(cellMap);

		return mlc;
	}

	// find the Most Likely Cell.
	private GeoCell findMLC(Map<Long, GeoCell> cellMap) {

		cellMap = MyHashMap.sortByMLCValues(cellMap);

		GeoCell mlc = null;

		if (!cellMap.isEmpty()){
			Long mlcId = Long.parseLong(cellMap.keySet().toArray()[0].toString());
			
			mlc = cellMap.get(mlcId);
			mlc.setConfidence((float) calculateConfidence(cellMap,mlcId));
		}

		return mlc;
	}

	// Calculate confidence for the estimated location
	private static double calculateConfidence(Map<Long, GeoCell> cellMap, Long mlc) {
		
		Double sum = 0.0, total = 0.0;

		for(Entry<Long, GeoCell> entry:cellMap.entrySet()){
			double[] mCell = CellCoder.cellDecoding(mlc);
			double[] cell = CellCoder.cellDecoding(entry.getKey());
			if((cell[0] >= (mCell[0]-confRange)) 
					&& (cell[0] <= (mCell[0]+confRange))
					&& (cell[1] >= (mCell[1]-confRange)) 
					&& (cell[1] <= (mCell[1]+confRange))){
				sum += entry.getValue().getTotalProb();
			}
			total += entry.getValue().getTotalProb();
		}

		return sum/total;
	}

	/**
	 * This is the function that calculate the cell probabilities.
	 * @param sentence : list of terms contained in tweet text
	 * @return a map of cell
	 */
	private Map<Long, GeoCell> calculateCellsProbForImageTags (Set<String> sentence) {

		Map<Long,GeoCell> cellMap = new HashMap<Long,GeoCell>();

		Long cell;
		for(String term:sentence){
			if(termCellProbsMap.containsKey(term)){
				double locality= termWeights.get(term)[1];
				double entropy= termWeights.get(term)[0];
				
				for(Entry<Long, Double> entry: termCellProbsMap.get(term).entrySet()){
					cell = entry.getKey();
					if(cellMap.containsKey(cell)){
						cellMap.get(cell).addProb(entry.getValue()
								*(0.8*locality+0.2*entropy), term);
					}else{
						GeoCell tmp = new GeoCell(cell);
						tmp.addProb(entry.getValue()
								*(0.8*locality+0.2*entropy), term);
						cellMap.put(cell,tmp);
					}
				}
			}
		}
		return cellMap;
	}

	/**
	 * This is the constructor function load the term-cell probabilities file and create
	 * the respective map. The generated map allocate a significant amount of memory.
	 * @param termCellProbsFile : file that contains the term-cell probabilities
	 */
	public LanguageModel(String termCellProbsFile, double confRange, int confItemNumber){

		LanguageModel.confRange = confRange;
		LanguageModel.confItemNumber = confItemNumber;
		
		EasyBufferedReader reader = new EasyBufferedReader(termCellProbsFile);

		termCellProbsMap = new HashMap<String,Map<Long,Double>>();;

		termWeights = new HashMap<String,Double[]>();

		String line;
		String term;

		logger.info("opening file" + termCellProbsFile);
		logger.info("loading cells' probabilities for all tags");

		long t0 = System.currentTimeMillis();

		while ((line = reader.readLine())!=null){

			term = line.split("\t")[0];

			Double[] weights = {Double.parseDouble(line.split("\t")[2]), Double.parseDouble(line.split("\t")[1])};
			
			termWeights.put(term, weights); // load term weights
			
			String[] inputCells = 
					(line.split("\t").length>3?line.split("\t")[3].split(" "):new String[] {});
			Map<Long, Double> tmpCellMap = new HashMap<Long,Double>();

			for(int i=0;i<inputCells.length;i++){
				long cellCode = CellCoder.cellEncoding(inputCells[i].split(">")[0]);
				String cellProb = inputCells[i].split(">")[1];
				tmpCellMap.put(cellCode, Double.parseDouble(cellProb));
			}
			termCellProbsMap.put(term, tmpCellMap);
		}
		logger.info(termCellProbsMap.size() + " terms loaded in " + (System.currentTimeMillis()-t0)/1000.0 + "s");
		logger.info("closing file" + termCellProbsFile);

		reader.close();
	}
}

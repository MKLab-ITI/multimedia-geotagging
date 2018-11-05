package gr.iti.mklab.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import gr.iti.mklab.methods.MultipleGrid;
import gr.iti.mklab.data.Estimation;
import gr.iti.mklab.data.GeoCell;
import gr.iti.mklab.methods.LanguageModel;
import gr.iti.mklab.methods.SimilaritySearch;
import gr.iti.mklab.methods.TermCellProbs;
import gr.iti.mklab.metrics.Entropy;
import gr.iti.mklab.metrics.Locality;
import gr.iti.mklab.tools.DataManager;
import gr.iti.mklab.tools.SimilarityCalculator;
import gr.iti.mklab.util.EasyBufferedReader;
import gr.iti.mklab.util.EasyBufferedWriter;
import gr.iti.mklab.util.Progress;
import gr.iti.mklab.util.Utils;

/**
 * The main class that combines all the other class in order to implement the method.
 * For memory allocation issues the main method has been separated in three steps, create,
 * train, FS (Feature Selection), LM (language model), IG (multiple grid technique) and 
 * SS (similarity search).
 * @author gkordo
 *
 */
public class MultimediaGeotagging {

	static Logger logger = Logger.getLogger("gr.iti.mklab.MainPlacingTask");

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception{

		Properties properties = new Properties();

		logger.info("Program Started");

		properties.load(new FileInputStream("properties/config.properties"));
		String dir = properties.getProperty("dir");

		String trainFolder = properties.getProperty("trainFolder");
		String testFile = properties.getProperty("testFile");

		String process = properties.getProperty("process");

		int coarserScale = Integer.parseInt(properties.getProperty("coarserScale"));
		int finerScale = Integer.parseInt(properties.getProperty("finerScale"));

		int k = Integer.parseInt(properties.getProperty("k"));
		String resultFile = properties.getProperty("resultFile");


		// Built of the Language Model
		if(process.contains("train") || process.equals("all")){
			Set<String> testIDs = DataManager.getSetOfImageIDs(dir + testFile);
			Set<String> usersIDs = DataManager.getSetOfUserID(dir + testFile);

			TermCellProbs trainLM = new TermCellProbs(testIDs, usersIDs);

			trainLM.calculatorTermCellProb(dir, trainFolder, 
					"Term-Cell Probs/scale_" + coarserScale, coarserScale);

			trainLM.calculatorTermCellProb(dir, trainFolder, 
					"Term-Cell Probs/scale_" + finerScale, finerScale);
		}

		// Feature Selection and Feature Weighting (Locality and Spatial Entropy Calculation)
		if(process.contains("FS")){
			logger.warn("Depricated Feature Selection and Feature Weighting functions.");
			logger.warn("We advise you to use the provided weights instead.");
			Entropy.calculateEntropyWeights(dir, "Term-Cell Probs/scale_" + coarserScale
					+ "/term_cell_probs");

			Locality loc = new Locality(dir + testFile, coarserScale);
			loc.calculateLocality(dir, trainFolder);
		}

		// Language Model
		if(process.contains("LM") || process.equals("all")){
			MultimediaGeotagging.computeMLCs(dir, testFile, "resultLM_scale" + coarserScale, 
					"Term-Cell Probs/scale_" + coarserScale + "/term_cell_probs", 
					"weights", true);

			MultimediaGeotagging.computeMLCs(dir, testFile, "resultLM_scale" + finerScale, 
					"Term-Cell Probs/scale_" + finerScale + "/term_cell_probs", 
					"weights", false);
		}

		// Multiple Grid Technique
		if(process.contains("MG") || process.equals("all")){
			MultipleGrid.determinCellIDsForSS(dir + "results/", 
					"resultLM_mg" + coarserScale + "-" + finerScale,
					"resultLM_scale"+coarserScale, "resultLM_scale"+finerScale);
		}

		//Similarity Search
		if(process.contains("SS") || process.equals("all")){
			SimilarityCalculator calculator = new SimilarityCalculator(dir + testFile, dir + 
					"results/resultLM_mg" + coarserScale + "-" + finerScale);
			calculator.performSimilarityCalculation(dir, trainFolder, "temp/similar_images");

			SimilaritySearch searcher = new SimilaritySearch(1);
			searcher.search(dir + testFile, dir + "results/resultLM_mg" + coarserScale + "-" + finerScale, 
					dir + "temp/similar_images/image_similarities", dir + "results/" + resultFile, k);
		}

		logger.info("Program Finished");
	}

	/**
	 * Function that perform language model method for a file provided and in the determined scale
	 * @param dir : directory of the project
	 * @param testFile : the file that contains the testset images
	 * @param resultFile : the name of the file that the results of the language model will be saved
	 * @param termCellProbsFile : the file that contains the term-cell probabilities
	 * @param weightFolder : the folder that contains the files of term weights
	 * @param thetaG : feature selection accuracy threshold
	 * @param thetaT : feature selection frequency threshold
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void computeMLCs(String dir, 
			String testFile, String resultFile, String termCellProbsFile, 
			String weightFolder, boolean confidenceFlag) throws InterruptedException, ExecutionException, IOException{

		logger.info("Process: Language Model MLC\t|\t"
				+ "Status: INITIALIZE\t|\tFile: " + testFile);

		new File(dir + "results").mkdir();
		EasyBufferedReader reader = new EasyBufferedReader(dir + testFile);

		// initialization of the Language Model
		LanguageModel lmItem = new LanguageModel();
		Map<String, Map<String, Double>> termCellProbsMap = lmItem.loadTermCellProbsAndWeights
				(dir + testFile, dir + termCellProbsFile, dir + weightFolder);

		logger.info("Process: Language Model MLC\t|\t"
				+ "Status: STARTED");


		long count = 0, total = Utils.countLines(dir + testFile);
		long startTime = System.currentTimeMillis();
		Progress prog = new Progress(startTime,total,10,60, "calculate",logger);
		ExecutorService pool = Executors.newCachedThreadPool();
		List<Future<GeoCell>> future = new ArrayList<Future<GeoCell>>();
		List<String> ids = new ArrayList<String>();

		String line;
		while ((line = reader.readLine())!=null && count<=total){
			String[] metadata = line.split("\t");
			future.add(pool.submit(new Estimation(metadata, lmItem, termCellProbsMap, confidenceFlag)));
			ids.add(metadata[1]);
			prog.showProgress(count, System.currentTimeMillis());
			count++;	
		}
		
		EasyBufferedWriter writer = new EasyBufferedWriter(dir + "results/" + resultFile);
		EasyBufferedWriter writerCE = new EasyBufferedWriter(dir + "results/" +
				resultFile + "_conf_evid");
		for (int i=0; i<ids.size(); i++) {
			String id = ids.get(i);
			GeoCell result = future.get(i).get();

			// write the results
			if(result != null && !result.equals("null")){
				writer.write(id + "\t" + result.getID());
				if(confidenceFlag)
					writerCE.write(id + "\t" + result.getConfidence());
			}else{
				writer.write(id + "\tN/A");
				if(confidenceFlag)
					writerCE.write(id + "\tN/A");
			}
			writer.newLine();
			if(confidenceFlag)
				writerCE.newLine();
		}

		logger.info("Process: Language Model MLC\t|\tStatus: COMPLETED\t|\tTotal Time: " +
				(System.currentTimeMillis()-startTime)/60000.0+"m");
		reader.close();
		writer.close();
		writerCE.close();

	}
}

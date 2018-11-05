package gr.iti.mklab.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import gr.iti.mklab.methods.LanguageModel;
import gr.iti.mklab.util.TextUtil;

public class Estimation implements Callable<GeoCell>{

	private Set<String> terms = new HashSet<String>();
	private String description;
	private LanguageModel lm;
	private Map<String, Map<String, Double>> termCellProbsMap;
	private Boolean confidenceFlag;
	
	
	public Estimation(String[] metadata, LanguageModel lm, Map<String, Map<String, Double>> termCellProbsMap, Boolean confidenceFlag){
		// Pre-procession of the tags and title
		TextUtil.parse(metadata[10], this.terms);
		TextUtil.parse(metadata[8], this.terms);
		
		this.description = metadata[9];
		this.lm = lm;
		this.termCellProbsMap = termCellProbsMap;
		this.confidenceFlag = confidenceFlag;
	}
	
	@Override
	public GeoCell call() throws Exception {
		GeoCell result = lm.calculateLanguageModel(terms, termCellProbsMap, confidenceFlag);
		if(result==null&&!description.isEmpty()){ // no result from tags and title procession
			// give image's description in the language model (if provided)
			TextUtil.parse(description, terms);					
			result = lm.calculateLanguageModel(terms, termCellProbsMap, confidenceFlag);
		}
		return result;
	}
}

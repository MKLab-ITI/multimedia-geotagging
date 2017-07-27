package gr.iti.mklab.util;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pre-process of a query sentence removes redundant white space, punctuation and symbols that may exist inside it.
 * @author gkordo
 *
 */
public class TextUtil {

	public static String deAccent(String str) {
		String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		return pattern.matcher(nfdNormalizedString).replaceAll("");
	}

	public static Set<String> cleanText (String text, boolean useBigrams){

		Set<String> wordSet = new HashSet<String>();

		if (text!=null&&!text.isEmpty()){		
			text = text.replaceAll("(\r\n|\n)", " ");

			String cText = "";
			for(String word:text.trim().split(" ")){
				if(!word.matches("[0-9]+")&&!word.contains("http")){
					cText+=word+" ";
				}
			}	
			cText = cText.trim();
			cText = cText.replaceAll("-"," ").replaceAll("'"," ");
			cText = cText.replaceAll("[\\p{Punct}]", "");
			cText = cText.toLowerCase();
			cText = deAccent(cText);

			if(!cText.isEmpty()){
				for(String word:cText.split(" ")){
					if(!word.matches("[0-9]+")&&!word.isEmpty()){
						wordSet.add(word);
					}
				}
			}

			if (useBigrams){
				for(int i=0;i<cText.split(" ").length-1;i+=2){
					String bigram = cText.split(" ")[i]+" "+cText.split(" ")[i+1];
					if(!bigram.equals(" ")
							&&!cText.split(" ")[i].matches("[0-9]+")
							&&!cText.split(" ")[i+1].matches("[0-9]+")){
						wordSet.add(bigram);
					}
				}
			}
		}
		return wordSet;
	}
}

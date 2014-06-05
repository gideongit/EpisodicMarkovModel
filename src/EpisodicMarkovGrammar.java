

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;



public class EpisodicMarkovGrammar {

	/** the `rules' of the episodic grammar are treelets filled with traces
	 * Integer is HC of treelet 
	 */
	protected HashMap<Integer, Wordlet> episodicWordlets = new HashMap<Integer, Wordlet>();
	
	/**
	 * HashMap&lt;String: associatedRule.ruleString, HashMap&lt;String: goalCategory, HashMap&lt;Integer: regPosition, Integer: hc of the treelet&gt;&gt;&gt; 
	 */
	public static HashMap<String, Integer> treeletLookupTable = new HashMap<String, Integer>();
	
	protected HashSet<String> lexicon = new HashSet<String>();
	
	protected static HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();

	protected HashMap<String, Double> unigramProbabilities = new HashMap<String, Double>();
	
	protected static int treeletUniqueIDGenerator=0;
	
	public EpisodicMarkovGrammar(ArrayList<String[]> preprocessedSentences) throws Exception {
		
		/*
		//create special START and END Wordlet
		Wordlet startTreelet = new Wordlet("<s>");
		int treeletHC = addToTreeletLookupTable("<s>");
		episodicWordlets.put(treeletHC, startTreelet);
		
		//endTreelet:
		Wordlet endTreelet = new Wordlet("</s>");
		treeletHC = addToTreeletLookupTable("</s>");
		episodicWordlets.put(treeletHC, endTreelet);
		*/
		
		int totalWordCount=0;
		//HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
		//HashMap<String, Double> unigramProbabilities = new HashMap<String, Double>();
		
		for (String[] mySentence : preprocessedSentences) {

			for (String myWord : mySentence) {
				lexicon.add(myWord);
				
				if (!(myWord.equals("<s>"))) {	//no unigram statistics for start 
					totalWordCount++;
					if (wordCounts.get(myWord)==null)
						wordCounts.put(myWord, 1);
					else wordCounts.put(myWord, wordCounts.get(myWord) + 1);
				}
			}
		}
		
		for (String myWord : wordCounts.keySet()) {
			unigramProbabilities.put(myWord, ((double) wordCounts.get(myWord) / (double) totalWordCount));
		}
		
		//create Wordlet for every word from the lexicon
		for (String myWord : lexicon) {
			Wordlet myWordlet = new Wordlet(myWord);
		
			int wordletID = addToTreeletLookupTable(myWord);
		
			episodicWordlets.put(wordletID, myWordlet);	
		}
		
	}

	
	
	/** 
	 * Fills episodic wordlets with traces
	 */
	public void fillTreeletsWithTraces(ArrayList<String[]> sentences) throws Exception {

		int myCounter = 0;

		for (String[] sentence : sentences) {

			if (myCounter % 5000 ==0) 
				System.out.println("############    " + myCounter + " out of " + sentences.size() + "  sentences processed for episodic traces   #############");	//myParseTree=" + myParseTree.printWSJFormat()
			
			createTracesForSentence(sentence, myCounter);
			
			myCounter++;
		}
		
	}

	public void createTracesForSentence(String[] mySentence, int sentenceNr) {
		
		//trace for START treelet ("<s>")
		int currentWordletID = treeletLookupTable.get(mySentence[0]);
		Wordlet currentWordlet = episodicWordlets.get(currentWordletID);
		int nextWordletID = 0;
		
		/*
		//get reference to Wordlet of the next word
		int nextWordletID = treeletLookupTable.get(mySentence[0]);
		Trace myTrace = new Trace(sentenceNr, 0, nextWordletID);
		//put trace in START wordlet
		currentWordlet.addTrace(myTrace);
		
		currentWordletID = nextWordletID;
		currentWordlet = episodicWordlets.get(currentWordletID);
		*/
		
		//traces for words
		String nextWord = "", myWord = "";
		
		//traces for END treelet is not necessary
		for (int wordPosition = 0; wordPosition < mySentence.length-1; wordPosition++) {	
			myWord = mySentence[wordPosition];
			//get reference to Wordlet of the next word
			nextWord = mySentence[wordPosition+1];
		
			nextWordletID = treeletLookupTable.get(nextWord);
			Trace myTrace = new Trace(sentenceNr, wordPosition, nextWordletID);
			//store trace in currentWordlet
			currentWordlet.addTrace(myTrace);
			
			currentWordletID = nextWordletID;
			currentWordlet = episodicWordlets.get(currentWordletID);

		}
		
	}
	
	public static int addToTreeletLookupTable(String myWord) {
	
		
		//check if exists
		if (treeletLookupTable.get(myWord)==null) {
			//make unique TreeletID
			treeletUniqueIDGenerator++;
			treeletLookupTable.put(myWord, treeletUniqueIDGenerator);
			return treeletUniqueIDGenerator;
		}
		 return treeletLookupTable.get(myWord);
		
	}
	
	public HashMap<Integer, Wordlet> getEpisodicWordlets() {
		return this.episodicWordlets;
	}
	
	public HashMap<String, Double> getUnigramProbabilities() {
		return this.unigramProbabilities;
	}
	
	public HashSet<String> getLexicon() {
		return lexicon;
	}
}

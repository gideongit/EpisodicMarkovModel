

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

	

	///////////////////////////////////////////////////
	/////////////////      DATA        ////////////////
	///////////////////////////////////////////////////
	public static String TRAIN_CORPUS = "./input/f0-20.unk10.flat.txt"; 	
	protected static String TEST_CORPUS = "./input/f21-22.unk10.flat500.txt";	//f21-22.unk10.flat_small	
	
	protected static double overallCorpusLogWordTransitionPrb =0d;
	protected static int overallNrWordsInCorpus =0;
	
	public static boolean PRINT_PREFIX_PROBABILITIES = false;
	
	public static boolean ADD_START_AND_END_SYMBOL = true;
	public static boolean SMOOTH_FOR_UNKNOWN_WORDS = false;
	
	/**
	 * back-off factor used for smoothing unigrams
	 * (1 - LAMBDA_0)*episodicP + LAMBDA_0*unigramP			
	 */
	public static double LAMBDA_0 = 0.3;
	
	/**
	 * Precomputed weights per pathlength/history, for weighing the contribution of a trace (ETN)
	 * in the parse move decision by its history. A weight equals back-off-value ^(history)
	 */
	public static HashMap<Integer, Double> activationLookupTable = new HashMap<Integer, Double>();
	
	/**
	 * how many steps of history are used
	 */
	public static int CUTOFF_HISTORY = 100;
	
	/**
	 * Used for exponential weighing of the common path in episodic parser. 
	 * weight = Math.pow(BACK_OFF_FACTOR, (history));
	 */
	protected static double EPISODIC_DECAY_FACTOR = 3.;
	protected static boolean POLYNOMIAL_WEIGHTS = true;

	//  ######################################################
	
	public static void main(String[] args)  throws Exception {

		readCommandLineArgs(args);

		//precompute the activations for traces as function of common history
		double weight=0.;
		for (int history=1; history<=CUTOFF_HISTORY; history++) {
			if (Main.POLYNOMIAL_WEIGHTS) weight = Math.pow(((double) history), EPISODIC_DECAY_FACTOR);
			else weight = Math.pow(EPISODIC_DECAY_FACTOR, ((double) history-1));	//weight=1 is reserved for back-off
			activationLookupTable.put(history, weight);
		}	
		
		//reading train sentences
		System.out.println("Reading treebank...");


		//############################################
		//########   PREPROCESSING STEPS     #########
		//############################################

		ArrayList<String[]> trainSentences = readSentencesFromFile(TRAIN_CORPUS, ADD_START_AND_END_SYMBOL);
		
		//smoothing : not used
		if (SMOOTH_FOR_UNKNOWN_WORDS) {
			SentencePreprocessor_not_used myTrainSetPreprocessor = new SentencePreprocessor_not_used(TRAIN_CORPUS, true);
			trainSentences = myTrainSetPreprocessor.getPreprocessedSentences();
			myTrainSetPreprocessor.smoothUnknownWordsInTrainSet(trainSentences);
		}

		
		//############################################
		//#############      TRAIN      ##############
		//############################################
		
		/*
		 * 1) determine vocabulary
		 * 2) create wordLet for every word, plus start/end plus unk word categories
		 * 3) run through the sentences, and fill wordlets with traces (beginning with START)
		 */
		
		EpisodicMarkovGrammar myGrammar = new EpisodicMarkovGrammar(trainSentences);
			
		ArrayList<String> lexiconArray = new ArrayList<String>();
		lexiconArray.addAll(myGrammar.getLexicon());

		System.out.println("Filling treelets with traces...");
		myGrammar.fillTreeletsWithTraces(trainSentences);
		
		
		//############################################
		//#############      TEST       ##############
		//############################################

		// read test sentences
		ArrayList<String[]>  testSentences = readSentencesFromFile(TEST_CORPUS, ADD_START_AND_END_SYMBOL);
		
		if (SMOOTH_FOR_UNKNOWN_WORDS) {
			SentencePreprocessor_not_used myTestSetPreprocessor = new SentencePreprocessor_not_used(TEST_CORPUS, false);
			myTestSetPreprocessor.replaceUnknownWordsInTestSet(myGrammar.getLexicon()); //only from unknownWordClassesInTrainSet
		}
			
		Wordlet currentWordlet = null;
		WordletState currentWordState = null, previousWordState = null;
		HashMap<Integer, Wordlet> episodicWordlets = myGrammar.getEpisodicWordlets();
		
		HashMap<String, Double> unigramProbabilities = myGrammar.getUnigramProbabilities();
		
		int sentenceCounter = 0;
		for (String[] testSentence : testSentences) {
			
			String wholeSentence = "";
			for (String myWord : testSentence)
				wholeSentence += myWord + " ";
			
			System.out.println("Computing prefix probabilities for sentence " + sentenceCounter + ": " + wholeSentence);
			// 1) loop through words of a sentence; for every word:
		 	// 2) get reference to wordlet, create wordlet state; copy traces; 
		 	// 2a) update CH, determine wordlet state transition probs to other wordlets
		 	// 3) compute word transition prob. and keep track of sentence prob.
			// 4) output prefix prob: prob of string
			
			double[] prefixProbabilities = new double[testSentence.length];
			double[] logWordTransitionPrbs = new double[testSentence.length];
			double prefixProbability =1d, wordTransitionPr=1d, episodicPr=0d;
			double totalLogWordTransitionPrb = 0d;
			
			//get reference to start state (<s>) 
			Wordlet previousWordlet = episodicWordlets.get(EpisodicMarkovGrammar.treeletLookupTable.get(testSentence[0]));
			previousWordState = new WordletState(previousWordlet, 0);
			//set trace activations of start state to zero
			previousWordState.initializeTraceActivations();
			previousWordState.computeEpisodicProbabilities(episodicWordlets);
			
			for (int i=1; i<testSentence.length; i++) {	
				
				String currentWord = testSentence[i];
				
				//get reference to Wordlet with traces
				currentWordlet = episodicWordlets.get(EpisodicMarkovGrammar.treeletLookupTable.get(currentWord));
				//create empty new wordletState
				currentWordState = new WordletState(currentWordlet, i);
				
				//update CH's
				if (i < testSentence.length-1) {
					currentWordState.updateCHofTraces(previousWordState, episodicWordlets);
					currentWordState.computeEpisodicProbabilities(episodicWordlets);
				}
				
				//if (previousWordState.getWordletTransitionProbs()==null)
				//	System.out.println("previousWordState is null; word=" + previousWordState.getAssociatedWordlet().word);
				
				if (previousWordState.getWordletTransitionProbs().get(currentWordlet)==null) episodicPr = 0.;
				else episodicPr = previousWordState.getWordletTransitionProbs().get(currentWordlet);
				
				if (PRINT_PREFIX_PROBABILITIES)
					System.out.println("episodicP up to " + currentWord + "=" + episodicPr + "; unigramP=" + unigramProbabilities.get(currentWord));
				
				wordTransitionPr = (1. - LAMBDA_0)*episodicPr + LAMBDA_0*unigramProbabilities.get(currentWord);
				
				prefixProbability = prefixProbability * wordTransitionPr;
				prefixProbabilities[i] = prefixProbability;
				logWordTransitionPrbs[i] = Math.log10(wordTransitionPr);
				totalLogWordTransitionPrb += logWordTransitionPrbs[i];
				overallCorpusLogWordTransitionPrb += logWordTransitionPrbs[i];
				overallNrWordsInCorpus +=1;
				
				previousWordState = currentWordState;
			}
			
			NumberFormat numberFormatter;
			numberFormatter = new DecimalFormat("#.######");

			//perplexity:
			double per_word_perplexity = Math.pow(10., -1.*totalLogWordTransitionPrb/((double) (testSentence.length-1)));
			System.out.println("average perplexity (for " + (testSentence.length-1) + " words) =" + per_word_perplexity);
			
			//compute prefixProbabilities
			if (PRINT_PREFIX_PROBABILITIES) {
				
				for (int p=0; p<prefixProbabilities.length; p++) 
					System.out.println("PrefixPr for " + testSentence[p] + ": " + prefixProbabilities[p]); //numberFormatter.format(
			}
			
			sentenceCounter++;
		}
		
		double overall_per_word_perplexity = Math.pow(10., -1.*overallCorpusLogWordTransitionPrb/((double) overallNrWordsInCorpus));
		System.out.println("overall average perplexity =" + overall_per_word_perplexity);
		
	}		

	public static ArrayList<String[]> readSentencesFromFile(String corpusFile, boolean ADD_START_AND_END_SYMBOL) throws IOException {


    	ArrayList<String[]> sentences = new ArrayList<String[]>();
    	BufferedReader buff = null;
    	String mySentence;
    	buff = new BufferedReader(new FileReader(corpusFile));
	    while ((mySentence = buff.readLine()) !=null){
	    	if (!mySentence.startsWith("%")) {
	    		if (ADD_START_AND_END_SYMBOL)
	    			mySentence = "<s> " + mySentence.trim() + " </s>";
	    		sentences.add(mySentence.trim().split(" "));
	    	}
	    }
	    return sentences;
    }
	
	/**
	 * reads command line options
	 * @param args
	 * @throws NumberFormatException
	 */
	private static void readCommandLineArgs(String[] args) throws NumberFormatException {

		for (String s: args) {
				
			if (s.toLowerCase().startsWith("traincorpus=")) 
				TRAIN_CORPUS = "./input/" + s.split("=")[1];
			
			if (s.toLowerCase().startsWith("testcorpus=")) 
				TEST_CORPUS = "./input/" + s.split("=")[1];
			
		}
	}
}




import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//import parser.Node;
//import parser.parseTree;

public class SentencePreprocessor_not_used {

	protected ArrayList<String[]> sentencesFromCorpus; 
	
	/**
	 * Classes of infrequent words in train treebank, containing at least 
	 * MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS unique word types.
	 * Used to classify unknown words in test sentences.
	 */
	protected static HashSet<String> unknownWordClassesInTrainSet = new HashSet<String>(); 

	protected static HashMap<String, HashSet<Integer>> duplicateSentenceNrsInTrainSet = new HashMap<String, HashSet<Integer>>(); 
	
	protected static boolean CONVERT_INITIAL_WORD2LOWERCASE = false;
	
	protected static int MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS =3;	
	
	/**
	 * Word types in treebank with frequency < threshold are 
	 * replaced by unknown word classes
	 */
	protected static final int WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING = 4;
	
	/**
	 * Replaces unknown word ending on -s with its stem if the stem
	 * occurs in train treebank, and vice versa
	 */
	protected static final boolean DO_LEMMATIZATION = false;
	
	
    public static boolean REMOVE_PUNCTUATION = false;
    
      public static boolean CASE_SENSITIVITY = true;
    /**
     * EXTRACT_POSTAGS: reads only postags from the treebank, and puts these in the parsetree; 
     * if you don't use POSTAGS then it replaces POSTAGS by lexical items
     */
    public static boolean EXTRACT_POSTAGS = false;
    
	public static HashSet<String> lowerCaseConversions = new HashSet<String>();
	
	final static String containsDigitMatch = ".*\\d.*";
	final static String containsAlphaMatch = ".*[a-zA-Z].*";

	static boolean useFirstWord, useFirstCap, useAllCap;
	static boolean useDash, useForwardSlash, useDigit, useAlpha, useDollar;
	static String[] affixes, suffixes;
	static boolean allowToCombineSuffixAffix;
	static boolean useASFixWithDash, useASFixWithSlash, useASFixWithCapital;

	/**
	 * Constructs sentencePreprocessor: reads parses from treebank file
	 * 
	 * @param sentenceFile fileName of the treebank
	 * @param blnCaseConversion if true, invokes convertOpenClassWordsInInitialSentencePosition2LowerCase method
	 * @param blnTrainset if true, ignores lower and upper limits of treebank sentences, 
	 * imposed by experiments.FIRSTTESTSENTENCE experiments.LASTTESTSENTENCE
	 */
	public SentencePreprocessor_not_used(String sentenceFile, boolean blnTrainset) throws FileNotFoundException, IOException {

		loadDefaultParameters();
		lowerCaseConversions = new HashSet<String>();
		
		sentencesFromCorpus = readSentencesFromCorpus(sentenceFile, blnTrainset);
		System.out.println(sentencesFromCorpus.size() + " parsetrees were processed");
		
	}

	public SentencePreprocessor_not_used()throws FileNotFoundException, IOException {

		loadDefaultParameters();
		lowerCaseConversions = new HashSet<String>();
	}
	
	/**
	 * Extracts parseTrees from strings in WSJ format, given in treebankFile
	 * if treebankFile=Tuebingen does preprocessing to convert it to WSJ format
	 * 
	 * @param sentenceFile fileName of the treebank
	 * @param blnTrainset if true, ignores lower and upper limits of treebank sentences, 
	 * imposed by experiments.FIRSTTESTSENTENCE experiments.LASTTESTSENTENCE
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<String[]> readSentencesFromCorpus(String treebankFile, boolean blnTrainset) throws FileNotFoundException, IOException {
		//ArrayList<parseTree> parseTrees = new ArrayList<parseTree>();
		BufferedReader buff = null;

		//ArrayList<String> labeledSentences = new ArrayList<String> ();

		int myCounter = 0;
		String mySentence;
	
		ArrayList<String[]> sentences = new ArrayList<String[]>();
		//HashSet<String> uniqueSentences = new HashSet<String>();
		buff = new BufferedReader(new FileReader(treebankFile));

		while ((mySentence = buff.readLine()) !=null) {
			
			if (!(mySentence.startsWith("%"))) {
		
				mySentence = mySentence.trim();
						
				if (CONVERT_INITIAL_WORD2LOWERCASE) //does not work
					convertOpenClassWordsInInitialSentencePosition2LowerCase(mySentence);
				
				sentences.add(mySentence.split(" "));
				
				myCounter++; 
				if (myCounter % 500 ==0) System.out.println("############ reading " + myCounter + " parses from " + treebankFile + " ################");

			}	//if (!(mySentence.startsWith("%")))
		}	//while ((mySentence = buff.readLine()) !=null)
		return sentences;
	}


	/**
	 * Replaces infrequent words in the train treebank with unknown word classes,
	 * which are determined by morphology in method findUnknownWordClassesForInfrequentWords.
	 * If there are sufficiently many types in unknown word class, it stores this class in
	 * unknownWordClassesInTrainSet, to be used for classifying unknown words in test set
	 * 
	 */
	public void smoothUnknownWordsInTrainSet(ArrayList<String[]> sentencesFromTrainSet) {

		/** 
		 * unique word types plus token frequency
		 */
		HashMap<String, Integer> wordCounts = doWordFrequencyCounts(sentencesFromTrainSet);

		HashMap<String, Integer> unknownWordClassFrequencies = new HashMap<String, Integer>();
		HashMap<String, String> infrequentWordsPlusReplacements = new HashMap<String, String>();


		infrequentWordsPlusReplacements = findUnknownWordClassesForInfrequentWords(wordCounts);

		//some statistics (you need at least MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS unique words in every unknownWordClass)
		for (String infrequentWord : infrequentWordsPlusReplacements.keySet()) {
			String unknownWordClass = infrequentWordsPlusReplacements.get(infrequentWord);
			if (unknownWordClassFrequencies.get(unknownWordClass)==null) unknownWordClassFrequencies.put(unknownWordClass, 1);
			else unknownWordClassFrequencies.put(unknownWordClass, unknownWordClassFrequencies.get(unknownWordClass) + 1);
		}


		for (String unknownWordClass : unknownWordClassFrequencies.keySet()) {
			if (unknownWordClassFrequencies.get(unknownWordClass)>=MINIMUM_NUMBER_UNIQUE_WORDS_PER_UNKNOWN_CLASS) unknownWordClassesInTrainSet.add(unknownWordClass);
			else {
				//replace unknownWordClass back with the "unk" class, because there are not enough samples!
				for (String infrequentWord : infrequentWordsPlusReplacements.keySet()) {
					if (infrequentWordsPlusReplacements.get(infrequentWord).equals(unknownWordClass))
						infrequentWordsPlusReplacements.put(infrequentWord, "unk");
				}
			}
			//print smoothing statistics
			//System.out.println("smoothing class: " + unknownWordClass + "; frequency=" + unknownWordClassFrequencies.get(unknownWordClass));
		}

		for (String[] mySentence : sentencesFromTrainSet) {
			//replace infrequent words in the parseTrees: checks if it is open class
			replaceInfrequentOpenClassWordsInTrainset(mySentence, infrequentWordsPlusReplacements);
		}

	}

	
	/**
	 * Replaces unknown words (not in terminalUnits of grammar) in test sentences 
	 * by unknown word classes if these are also found in unknownWordClassesInTrainSet. <br>
	 * First checks whether the same word with or without final "s", and with or without
	 * initial capital letter is perhaps found in the train set
	 * 
	 * @param lexicon yields the known terminals in the train treebank 
	 * 
	 * @return HashMap of unknownWordReplacements <unknown word, unknown word class>
	 * that can be reused in reranker for smoothing the 10 best parses, since the 
	 * replacements need to be found only once in the gold standard test sentences.
	 */	
	public HashMap<String, String> replaceUnknownWordsInTestSet(HashSet<String> lexicon){

		HashMap<String, String> unknownWordReplacements = new HashMap<String, String>();
		//HashSet<String> wordExistsWithOrWithoutFinal_s = new HashSet<String>();

		//HashSet<String> lexicon = (HashSet) myGrammar.getTerminalUnits().keySet();

		for (String[] mySentence : this.sentencesFromCorpus) {
			int wordPosition=0;
			for (String myWord : mySentence) {

					
					int wlen = myWord.length();

					//check if it is unknown word
					if (!(lexicon.contains(myWord))) {

						
						//informed guesses: check if unknown word does not occur with/out -s or with/out capital letter)
						//does the unknown word occur in the train set without final "s"? or with?
						boolean blnStemKnownOrCapitalLetter = false;

						if (DO_LEMMATIZATION) {
							//XXX weggehaald
						}	//if (experiments.DO_LEMMATIZATION)

						//capital letter at initial position of test sentence: kijk of het zonder hoofdletter voorkomt in train.
						if (!(myWord.equals(myWord.toLowerCase()))) {
							if (wordPosition==0) {
								if (lexicon.contains(myWord.toLowerCase())) {
									myWord = myWord.toLowerCase();
									//myNode.setProductionName(wordLabel.toLowerCase());
									unknownWordReplacements.put(myWord, myWord.toLowerCase());
									blnStemKnownOrCapitalLetter = true;
								}
							}
						}

						//replace word with unknownWordClass (if unknown word does not occur with/out -s or with/out capital letter)
						if (!blnStemKnownOrCapitalLetter) {
							String smoothClass = SentencePreprocessor_not_used.getFeatureOfWord(myWord.trim());
							
							//replace production in terminal, but only if the unknown wordclass also occurs in train set at least 10 times
							if (unknownWordClassesInTrainSet.contains(smoothClass)) {
								//if (smoothClass.equals("1capY_dashY_slshN_alfY_digN_sfx:NONE_afx:NONE")) System.out.println("rare ding in train");
								myWord = smoothClass;
								//myNode.setProductionName(smoothClass);
								unknownWordReplacements.put(myWord, smoothClass);	//this is for nBest Charniak parses
							}
							else {
								myWord = "unk";
								//myNode.setProductionName(smoothClass);
								unknownWordReplacements.put(myWord, "unk");
							}
						}
					}	//if (!(lexicon.contains(myNode.getProductionName()))) {
					wordPosition++;
				}	//for (Node myNode : testParseTree.getNodes())
		}	//for (parseTree testParseTree : goldStandardParses)

		return unknownWordReplacements;
	}

	
	/**
	 * Counts token frequencies of words in treebank
	 * @param mySentences
	 * @return wordCounts
	 */
	public static HashMap<String, Integer> doWordFrequencyCounts(ArrayList<String[]> mySentences) {
		HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
		
		for (String[] mySentence : mySentences) {
			for (String myWord : mySentence) {
				
				if (wordCounts.get(myWord)==null)
					wordCounts.put(myWord, 1);
				else wordCounts.put(myWord, wordCounts.get(myWord) + 1);
				
			}
		}
		return wordCounts;
	}

	/**
	 * Determines classes of infrequent words in train treebank, 
	 * based on their frequency in wordCounts.
	 * Note no actual replacement done here. For local use only.
	 * 
	 * @param wordCounts words plus token frequencies
	 * @return wordReplacements: <word, unknownWordClass>
	 */
	public HashMap<String, String> findUnknownWordClassesForInfrequentWords(HashMap<String, Integer> wordCounts){
		
		HashMap<String, String> wordReplacements = new HashMap<String, String>();
		//rare words are those that occur in wordCounts with frequency < WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING
		//determine their unknownWordClass
		for (String rareWord : wordCounts.keySet()) {	//loop over unique words (lexicon)
			if (wordCounts.get(rareWord) <= WORD_FREQUENCY_THRESHOLD_FOR_SMOOTHING) {
				String unknownWordClass = SentencePreprocessor_not_used.getFeatureOfWord(rareWord.trim());
				wordReplacements.put(rareWord, unknownWordClass);
			}
		}

		return wordReplacements;
	}

	/**
	 * Has been replaced by getFeatureOfWord
	 * Determines unknown word classes based on morphology of the word.
	 * If word is not in one of the fixed classes, then classify it as "unk".
	 * @param rareWord
	 * @return wordClass
	 */
	public static String findWordClass(String rareWord) {
		//String wordClass = "";

		if (isValidInteger(rareWord) || isValidDouble(rareWord)) return "classNumber";
		if (rareWord.contains("-")) return "classHyphen";
		if (rareWord.contains(":")) return "classTime";
		//assuming initial sentence position words have been turned to lower case!!!
		if (!(rareWord.equals(rareWord.toLowerCase())))	 return "classCapital";
		//see: http://www.prefixsuffix.com/affixes.php
		//prefixes: see http://en.wikipedia.org/wiki/English_prefixes
		if (rareWord.endsWith("ble")) return "classBLE";
		if (rareWord.endsWith("ly")) return "classLY";
		if (rareWord.endsWith("ve")) return "classVE";
		if (rareWord.endsWith("ment")) return "classMENT";
		if (rareWord.endsWith("ent")) return "classENT";
		if (rareWord.endsWith("ence")) return "classENCE";
		if (rareWord.endsWith("en")) return "classEN";
		if (rareWord.endsWith("ism")) return "classISM";
		if (rareWord.endsWith("ist")) return "classIST";
		if (rareWord.endsWith("hood")) return "classHOOD";
		if (rareWord.endsWith("ity")) return "classITY";
		if (rareWord.endsWith("ness")) return "classNESS";
		if (rareWord.endsWith("ess")) return "classESS";
		if (rareWord.endsWith("er")) return "classER";
		if (rareWord.endsWith("est")) return "classEST";
		if (rareWord.endsWith("ing")) return "classING";
		if (rareWord.endsWith("ed")) return "classED";
		if (rareWord.endsWith("n't")) return "classNOT";
		if (rareWord.endsWith("ful")) return "classFUL";
		if (rareWord.endsWith("tion")) return "classTION";
		if (rareWord.endsWith("ion")) return "classION";
		if (rareWord.endsWith("ify")) return "classIFY";
		if (rareWord.endsWith("ise") || rareWord.endsWith("ize")) return "classISE";
		if (rareWord.endsWith("es")) return "classES";
		if (rareWord.endsWith("s")) return "classS";

		if (rareWord.startsWith("ab")) return "classAB";
		if (rareWord.startsWith("anti")) return "classANTI";
		if (rareWord.startsWith("auto")) return "classAUTO";
		if (rareWord.startsWith("con") || rareWord.startsWith("com")) return "classCON";
		if (rareWord.startsWith("dis")) return "classDIS";
		if (rareWord.startsWith("extra") || rareWord.startsWith("inter") || rareWord.startsWith("intra") || rareWord.startsWith("under")) return "classEXTRA";
		if (rareWord.startsWith("ex")) return "classEX";
		if (rareWord.startsWith("in")) return "classIN";
		if (rareWord.startsWith("non")) return "classNON";
		if (rareWord.startsWith("pre") || rareWord.startsWith("pro") || rareWord.startsWith("post")) return "classPRE";
		if (rareWord.startsWith("syn") || rareWord.startsWith("sym") || rareWord.startsWith("sys")) return "classSYN";
		if (rareWord.startsWith("un")) return "classUN";
		if (rareWord.startsWith("hetero") || rareWord.startsWith("homo")) return "classHETERO";
		if (rareWord.startsWith("sur") || rareWord.startsWith("super") || rareWord.startsWith("sub") || rareWord.startsWith("sup") || rareWord.startsWith("sus") || rareWord.startsWith("trans") || rareWord.startsWith("hyper")) return "classSUR";
		if (rareWord.startsWith("pseudo") || rareWord.startsWith("quasi") || rareWord.startsWith("semi") || rareWord.startsWith("neo")  || rareWord.startsWith("micro")  || rareWord.startsWith("macro")) return "classQUASI";
		if (rareWord.startsWith("re")) return "classRE";
		if (rareWord.startsWith("per")) return "classPER";


		return "unk";
	}

	public static boolean isValidInteger(String s) {
		try {
			int i = java.lang.Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	public static boolean isValidDouble(String s) {
		try {
			double d = java.lang.Double.parseDouble(s);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	/**
	 * Replaces the names of terminal nodes in parseTree if they occur 
	 * as key in infrequentWordReplacements, but only if the word (terminal) 
	 * belongs to an open postag class  
	 * 
	 * @param myParseTree
	 * @param infrequentWordReplacements
	 */
	public static void replaceInfrequentOpenClassWordsInTrainset(String[] mySentence, HashMap<String, String> infrequentWordReplacements) {
		
			for (String myWord : mySentence) {
					if (infrequentWordReplacements.containsKey(myWord)) {
						//only replace if POSTAG is open class
						//see: Building a Large Annotated Corpus of English: The Penn Treebank (page 5)
						//http://acl.ldc.upenn.edu/J/J93/J93-2004.pdf
						//determine postag
						//String postag = myNode.getParentNode().getName();

						boolean includeNNP=true;
						//if (memberOfOpenClass(postag, includeNNP)) {
							//System.out.println("old name=" + myNode.getName() + "; new name=" + rareWordReplacements.get(myNode.getName()));
							//myNode.setName(infrequentWordReplacements.get(myNode.getName()));
							myWord = infrequentWordReplacements.get(myWord);
						//}
				}
			}
		
	}


	public ArrayList<String[]> getPreprocessedSentences(){
		return this.sentencesFromCorpus;
	}

	
	/**
	 * Converts the first word of the sentence to lower case,
	 * but only if the first word belongs to an open postag class
	 * 
	 * @param mySentence
	 */
	public static HashSet<String> convertOpenClassWordsInInitialSentencePosition2LowerCase(String mySentence) {

		
    		String[] splitSentence = mySentence.split(" ");
    		String firstWord = splitSentence[0];
    		
    		/*
    		 * works only if you know the postags
    		 * 
			//capital letter at initial position of test sentence: kijk of het zonder hoofdletter voorkomt in train.
			if (!(firstWord.equals(firstWord.toLowerCase()))) {
				//check open class
				String firstPostag = myNode.getParentNode().getName();
				if (memberOfOpenClass(firstPostag, false) && !wordLabel.contains("*")) {
					myNode.setName(wordLabel.toLowerCase());
					lowerCaseConversions.add(wordLabel);
				}
			}
    		*/	
    	
    	return lowerCaseConversions;
    }
	
	protected void loadDefaultParameters() {

		useFirstWord = true;
		useFirstCap = true;
		useAllCap = false;
		useDash = true; 
		useForwardSlash = true; 
		useDigit = true;
		useAlpha = true;
		useDollar = false;
		allowToCombineSuffixAffix = false;
		useASFixWithDash = false;
		useASFixWithSlash = false;
		useASFixWithCapital = false;


		//String affixesAll = "inter trans under over non com con dis pre pro co de in re un";
		String suffixesAll = "ments ance dent ence ists line ment ship time ans ant are " +
		"ate ble cal ess est ful ian ics ing ion ist ive man ons ory ous son tor " +
		"ure al ce ck cy de ds ed er es et ey fy gs gy ic is ks ld le ls ly ne rd " +
		"rs se sh sm th ts ty ze s y";

		affixes = null;
		//affixes = affixesAll.split("\\s");
		suffixes = suffixesAll.split("\\s");

	}

	public static String getFeatureOfWord(String word) {	//, boolean firstWord
		StringBuilder result = new StringBuilder();		
		//if (useFirstWord) {
		//	result.append(firstWord ? "_1stY" : "_1stN");
		//}
		boolean firstCapital = false;
		if (useFirstCap) {
			char firstChar = word.charAt(0);
			firstCapital = Character.isUpperCase(firstChar);
			result.append(firstCapital ? "_1capY" : "_1capN");
		}
		boolean allCapital = false;
		if (useAllCap) {			
			allCapital = allCapitals(word);
			result.append(allCapital ? "_AcapY" : "_AcapN");
		}
		boolean hasDash = false;
		if (useDash) {
			hasDash = word.indexOf('-')!=-1;
			result.append(hasDash ? "_dashY" : "_dashN");
		}	
		boolean hasForwardSlash = false;
		if (useForwardSlash) {
			hasForwardSlash = word.indexOf('/')!=-1;
			result.append(hasForwardSlash ? "_slshY" : "_slshN");
		}
		if (useDollar) {
			boolean hasDollar = word.indexOf('$')!=-1;
			result.append(hasDollar ? "_$Y" : "_$N");
		}
		if (useAlpha) {
			boolean hasDigit = word.matches(containsAlphaMatch);			
			result.append(hasDigit ? "_alfY" : "_alfN");
		}
		if (useDigit) {
			boolean hasDigit = word.matches(containsDigitMatch);			
			result.append(hasDigit ? "_digY" : "_digN");
		}

		if ( (useASFixWithDash || !hasDash) &&
				(useASFixWithSlash || !hasForwardSlash) && 	
				(useASFixWithCapital || (!firstCapital && !allCapital)) ){			
			String wordLower = word.toLowerCase();
			boolean foundSuffix = false;
			if (suffixes!=null) {
				String suff = getSuffix(wordLower);
				if (suff==null) result.append("_sfx:NONE");
				else {
					result.append("_sfx:" + suff);
					foundSuffix = true;
				}
			}
			if (affixes!=null) {
				if (!allowToCombineSuffixAffix && foundSuffix) {
					result.append("_afx:NONE");
				}
				else {
					String aff = getAffix(wordLower);
					result.append(aff==null ? "_afx:NONE" : ("_afx:" + aff));
				}
			}
		}
		else {
			result.append("_sfx:NONE");
			result.append("_afx:NONE");
		}
		return result.substring(1);		
	}

	public static boolean allCapitals(String w) {
		char[] charArray = w.toCharArray();
		for(char c : charArray) {
			if (Character.isLowerCase(c)) return false;
		}
		return true;
	}

	public static String getAffix(String wordLower) {
		int index = 0;
		for(String a : affixes) {
			if (wordLower.startsWith(a)) {
				return a;
			}
			index++;
		}
		return null;
	}

	public static String getSuffix(String wordLower) {
		int index = 0;
		for(String a : suffixes) {
			if (wordLower.endsWith(a)) {
				return a;
			}
			index++;
		}
		return null;
	}

	
}

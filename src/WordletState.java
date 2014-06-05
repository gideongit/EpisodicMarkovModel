

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WordletState {

	//protected StateLC aState;
	protected Wordlet associatedWordlet;	

	protected int sentencePosition;
	
	//episodic transition probabilities to all wordlets; for most probable episodic parser alone 
	//Integer is hc of successorWordlet 
	HashMap<Wordlet, Double> wordletTransitionProbabilities = null;
	
	/**
	 * constructor
	 */
	public WordletState(Wordlet wordletType, int sentencePosition) {	

		
		//treeletType must be CLONED 
		this.associatedWordlet = (Wordlet) wordletType.clone();
		this.sentencePosition = sentencePosition;
		
		//which may result in treelet w/o traces. in that case add dummy trace.
		if (this.associatedWordlet.getTraces().size()==0) {
			//System.out.println("ZERO TRACES");
			Trace dummyTrace = new Trace(9999,0,0);
			
			this.associatedWordlet.addTrace(dummyTrace);
		}
			
		wordletTransitionProbabilities = new HashMap<Wordlet, Double>();
		
	}

	public void updateCHofTraces(WordletState predecessorState, HashMap<Integer, Wordlet> myEpisodicTreelets) {

		int hcOfPredecessorTrace = 0;
		Trace myTrace = null, predecessorTrace = null;
		
		HashMap<Integer, Trace> predecessorTraces = predecessorState.getAssociatedWordlet().getTraces();
		HashMap<Integer, Trace> currentStateTraces = this.associatedWordlet.getTraces();
		
		//for all traces in current WordletState
		for (Integer hc : currentStateTraces.keySet()) {

			//long currentTime =System.currentTimeMillis(); 
			
			myTrace = currentStateTraces.get(hc);

			//by default set the CH of the trace to 1
			myTrace.setCH(1);
			
			//construct hc: -1 for predecessor
			hcOfPredecessorTrace = hc-1;
	
			//find predecessorTrace
			//check if exists direct predecessorTrace in predecessorState (previous word)
			predecessorTrace = predecessorTraces.get(hcOfPredecessorTrace);
			if (!(predecessorTrace==null)) { //there is direct predTrace) 
				//int newCH = predecessorTrace.getCH() + 1;
				//if this is greater than previous CH then update it
				//if (newCH > myTrace.getCH())
					myTrace.setCH(predecessorTrace.getCH() + 1);
			}
	 
		}	//for (Integer hc : this.associatedTreelet.getTraces().keySet()) {
	}

	public void initializeTraceActivations() {

		HashMap<Integer, Trace> currentStateTraces = this.associatedWordlet.getTraces();
		
		//for all traces in current WordletState
		for (Integer hc : currentStateTraces.keySet()) {
			
			currentStateTraces.get(hc).setCH(1);
		}
	}
	
	
	public void computeEpisodicProbabilities(HashMap<Integer, Wordlet> myWordlets) {
		
		double totalActivation=0d, activation=0d;
		Double aggregActivation=0d;
		
		//sum of activation of all traces pointing to same treeletHC
		HashMap<Integer, Double> totalActivationPerSuccessorWordlet = new HashMap<Integer, Double>(); 
		
		int CH_max=0, successorWordletHC=0;
		
		//loop over traces
		for (Integer traceID : this.getAssociatedWordlet().getTraces().keySet()) {
			
			Trace myTrace = this.getAssociatedWordlet().getTraces().get(traceID);

		    CH_max = Math.min(Main.CUTOFF_HISTORY, myTrace.getCH());

		    successorWordletHC = myTrace.getHashOfSuccessorWordlet();
		    
		    if (!(successorWordletHC==0)) {  //successorTreeletHC=0 in case of smoothing for unknown shift rules
		    	//System.out.println("successorTreeletHC=0; this=" + this.nicerString());
		    
			    //if (CH_max>0) {	//is always the case
					
				    //activation = Math.pow(BACK_OFF_FACTOR, ((double) history));	//contribution=1 is reserved for back-off
					//if CH=1 (minimum) then activation=1.
				    activation = Main.activationLookupTable.get(CH_max);
			   // }
			   // else activation=0d;	
			    
			    aggregActivation = totalActivationPerSuccessorWordlet.get(successorWordletHC);
			    if (aggregActivation==null)
				    totalActivationPerSuccessorWordlet.put(successorWordletHC, activation);
			    else totalActivationPerSuccessorWordlet.put(successorWordletHC, aggregActivation + activation);
			   
			    totalActivation += activation;
		    }
		    
		} // for (Integer traceID : this.getAssociatedTreelet().getTraces().keySet()) {
	
		if (totalActivation==0) totalActivation =1;
		
		//compute episodic probability = totalActivationPerSuccessorTreelet /totalActivation;
		for (Integer successorTreeletHC2 : totalActivationPerSuccessorWordlet.keySet()) {
		
			//get reference to associated rule of treelet
			
			Wordlet myRule = myWordlets.get(successorTreeletHC2);
			this.wordletTransitionProbabilities.put(myRule, totalActivationPerSuccessorWordlet.get(successorTreeletHC2)/totalActivation);
			//System.out.println("successorTreelet=" + myRule.toString() + "; P=" + totalActivationPerSuccessorTreelet.get(successorTreeletHC2)/totalActivation);
		}
		
	}

	public HashMap<Wordlet, Double> getWordletTransitionProbs() {
		return this.wordletTransitionProbabilities;
	}
	
	
	public Wordlet getAssociatedWordlet() {
		return this.associatedWordlet;
	}


	//public int getOverallMinimumInnerVSD() {
	//	return this.overallMinimumInnerVSD;
	//}

	public boolean equals(Object obj){

        if(!(obj instanceof WordletState)){
           return false;
       }

        WordletState otherState = (WordletState) obj;

        if (!(otherState.associatedWordlet.equals(this.associatedWordlet))) return false;
        if (!(otherState.sentencePosition == this.sentencePosition)) return false;

       return true;

   }

	public int hashCode(){
		 int hash = 3;
		 
		 hash = 7 * hash + this.associatedWordlet.hashCode();
		 hash = 7 * hash + this.sentencePosition;
		 return hash;
	
	
	}
	 
}

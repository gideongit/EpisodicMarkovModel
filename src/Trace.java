

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Trace implements Cloneable {

	protected int sentenceNr;
	protected int sequenceNr;
	
	/**
	 * HashCode of treelet that succeeds the current trace in train derivation
	 */
	protected int hashOfSuccessorWordlet;
	
	
	protected int commonHistory = 0;	//common history
	
		
	//  ######################################################################  //
	
	/**
	 * Constructor
	 */
	public Trace(int sentenceNr, int sequenceNr, int pointerToSuccessorWordlet) {	

		this.sentenceNr = sentenceNr;
		this.sequenceNr = sequenceNr;
		
		this.hashOfSuccessorWordlet = pointerToSuccessorWordlet;
		
	}
	
	public int getSentenceNr() {
		return this.sentenceNr;
	}
	public int getSequenceNr() {
		return this.sequenceNr;
	}
	
	public int getHashOfSuccessorWordlet() {
		return this.hashOfSuccessorWordlet;
	}
	
	
	public int getCH() {
		return this.commonHistory;
	}
	
	public void setCH(int commonHistory) {
		this.commonHistory = commonHistory;
	}
		
	public Object clone() {
        try {
            Trace aobj = (Trace) super.clone();
            //compressorNode associatedNode
            aobj.sentenceNr = sentenceNr;
            aobj.sequenceNr = sequenceNr;
            aobj.hashOfSuccessorWordlet = hashOfSuccessorWordlet;
            
           	return aobj;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
	
	public String toString() {
		return "" + this.sentenceNr + "-" + this.sequenceNr;
		
		
	}
	 public boolean equals(Object obj){
	        
         if(!(obj instanceof Trace)){
            return false;
        }
        
         Trace otherState = (Trace) obj;

         if (!(otherState.sentenceNr==this.sentenceNr)) return false;
         if (!(otherState.sequenceNr==this.sequenceNr)) return false;
         return true;
        
    }
	
	 public int hashCode(){

		 return 1000*this.sentenceNr + this.sequenceNr;
	  } 
}

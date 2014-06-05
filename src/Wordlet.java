

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class Wordlet implements Cloneable {

	protected String word;
	/**
	 * traces in the treelet are indexed by their hashCode
	 */
	protected HashMap<Integer, Trace> traces = new HashMap<Integer, Trace>();
	
	
	////  ###############################################################  ////
	
	public Wordlet(String myWord) {
		this.word = myWord;
	}
	
	
	//traces in the treelet are indexed by their hashCode
	public HashMap<Integer, Trace> getTraces() {
		return this.traces;
	}
	
	public void addTrace(Trace myTrace) {
		//if (this.toString().equals("PRO --> she; gc=PRO; reg=1"))
		//	System.out.println("@&#TRACE ADDED");
		//create HC for the trace
		int hc = myTrace.hashCode();
		traces.put(hc, myTrace);
	}
	
	 public String toString() {
		
		return this.word;
		 	 
	 }
	 
	
	/**
	 * used to clone treelet type (including traces) to a treeletState when creating a new state
	 */
	public Object clone() {
        try {
        	Wordlet aobj = (Wordlet) super.clone();
            
            aobj.word = word;
            
            HashMap<Integer, Trace> cloneOfTraces = new HashMap<Integer, Trace>();
            
            for (Integer hc : traces.keySet()) {
            	cloneOfTraces.put(hc, (Trace) traces.get(hc).clone());	
            }
         
            aobj.traces = cloneOfTraces;
            
        	return aobj;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
	
	public boolean equals(Object obj){
        
       if(!(obj instanceof Wordlet))  return false;
       
       Wordlet otherState = (Wordlet) obj;

       if(!(otherState.word.equals(this.word))) return false;
       
       return true;
       
   }
	
	
	public int hashCode(){
		   return this.word.hashCode(); 
    }
	
	
}

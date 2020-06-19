package com.drajer.eca.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.drajer.eca.model.EventTypes.JobStatus;

public class MatchTriggerStatus extends EicrStatus {
	
	private String 					actionId;
	private EventTypes.JobStatus    jobStatus;
	private Boolean					triggerMatchStatus; // Did anything match or not
	//private Set<String>				matchedCodes;
	private List<MatchedTriggerCodes> matchedCodes;
	
	
	public String getActionId() {
		return actionId;
	}
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}
	public EventTypes.JobStatus getJobStatus() {
		return jobStatus;
	}
	public void setJobStatus(EventTypes.JobStatus jobStatus) {
		this.jobStatus = jobStatus;
	}
	public Boolean getTriggerMatchStatus() {
		return triggerMatchStatus;
	}
	public void setTriggerMatchStatus(Boolean triggerMatchStatus) {
		this.triggerMatchStatus = triggerMatchStatus;
	}
	public List<MatchedTriggerCodes> getMatchedCodes() {
		return matchedCodes;
	}
	public void setMatchedCodes(List<MatchedTriggerCodes> matchedCodes) {
		this.matchedCodes = matchedCodes;
	}
	
	public MatchedTriggerCodes getMatchedTriggerCodes(String path, String valueSet, String valuesetVersion) {
		
		for(MatchedTriggerCodes mtc : matchedCodes) {
			
			if(mtc.getMatchedPath().contains(path) &&  
			   mtc.getValueSet().contains(valueSet) && 
			   mtc.getValueSetVersion().contains(valuesetVersion) )
				return mtc;
		}
		
		return null;
	}
	
	public void addMatchedCodes(Set<String> codes, String valueSet, String path, String valuesetVersion) {
		
		MatchedTriggerCodes mtc = getMatchedTriggerCodes(path, valueSet, valuesetVersion);
		
		if(mtc == null) {
			mtc = new MatchedTriggerCodes();
			mtc.setMatchedCodes(codes);
			mtc.setValueSet(valueSet);
			mtc.setValueSetVersion(valuesetVersion);
			mtc.setMatchedPath(path);
			matchedCodes.add(mtc);
		}
		else {
			mtc.addCodes(codes);			
		}
	}
	
	
	public MatchTriggerStatus() {
		actionId = "";
		matchedCodes = new ArrayList<MatchedTriggerCodes>();
		triggerMatchStatus = false;
		jobStatus = JobStatus.NOT_STARTED;
	}


}
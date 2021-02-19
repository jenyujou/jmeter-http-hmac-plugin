package com.sbux.jmeter.assertions;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.jayway.jsonpath.JsonPath;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.IntegerProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sbux.jmeter.util.JmeterUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat; 
import java.util.Date; 
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import java.text.DateFormat; 
import java.text.ParseException; 
import java.text.SimpleDateFormat; 
import java.util.Calendar; 
import java.util.Date; 
import java.util.Locale; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern;
import java.util.concurrent.*;


/**
 * Test element to handle Response Data Assertions, @see AssertionGui
 *
 * @author jjou
 *
 */
public class ResponseJSONTablesAssertion extends AbstractScopedAssertion implements Serializable, Assertion {

    private static final long serialVersionUID = 260L;

    private static final String TEST_FIELD_BASELINE = "assertion_baseline_JSON"; // $NON-NLS-1$ 

    private static final String TEST_FIELD_TARGET = "assertion_response_JSON"; // $NON-NLS-1$

    private static final String TEST_REQUIRED_FIELD = "assertion_required_field"; // $NON-NLS-1$

    private static final String TEST_SKIPPED_FIELDS = "assertion_skipped_fields"; // $NON-NLS-1$

    private static final String TEST_TYPE = "assertion_test_type"; // $NON-NLS-1$

    /*
     * Mask values for TEST_TYPE TODO: remove either MATCH or CONTAINS - they
     * are mutually exclusive
     */
    private static final int IGNORE = 1; // 1 << 0;

    private static final int CONTAINS = 1 << 1;

    private static final int EQUALS = 1 << 2;
    
    public static String errmsg = "";
    
    public static Date today = new Date();



    // Mask should contain all types (but not NOT)
    private static final int TYPE_MASK = IGNORE | EQUALS | CONTAINS ;

    public ResponseJSONTablesAssertion() {
        setProperty(new CollectionProperty(TEST_FIELD_TARGET, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_FIELD_BASELINE, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_REQUIRED_FIELD, new ArrayList<String>()));
        setProperty(TEST_SKIPPED_FIELDS, "");
   }

    @Override
    public void clear() {
        super.clear();
    }

    public void clearTestStrings() {
    	CollectionProperty[] temp = getTestStrings();
    	for(CollectionProperty c : temp){
    		c.clear();
    	}
    	getSkippedStrings().setObjectValue("");
    }

    public CollectionProperty[] getTestStrings() {
    	CollectionProperty[] tempCol = new CollectionProperty[3];
    	tempCol[0] = (CollectionProperty) getProperty(TEST_FIELD_TARGET);
    	tempCol[1] = (CollectionProperty) getProperty(TEST_FIELD_BASELINE);
    	tempCol[2] = (CollectionProperty) getProperty(TEST_REQUIRED_FIELD);
       return tempCol;
    }
    
    public JMeterProperty getSkippedStrings() {
    	return getProperty(TEST_SKIPPED_FIELDS);
    }

    public void addTestString(String responseField, String jmeterVariable, String requiredField) {
    	getTestStrings()[0].addProperty(new StringProperty(String.valueOf(responseField.hashCode()), responseField));
    	getTestStrings()[1].addProperty(new StringProperty(String.valueOf(jmeterVariable.hashCode()), jmeterVariable));
    	getTestStrings()[2].addProperty(new StringProperty(String.valueOf(requiredField.hashCode()), requiredField));
    }
    
    public void addSkippedStrings(String skippedFields) {
    	getSkippedStrings().setObjectValue(String.valueOf(skippedFields));
    }

    private void setTestTypeMasked(int testType) {
        int value = getTestType() & ~(TYPE_MASK) | testType;
        setProperty(new IntegerProperty(TEST_TYPE, value));
    }

    public void setToContainsType() {
        setTestTypeMasked(CONTAINS);
    }

    public void setToIgnoreCaseType() {
        setTestTypeMasked(IGNORE);
    }

    public void setToEqualsType() {
        setTestTypeMasked(EQUALS);
    }

    public int getTestType() {
        JMeterProperty type = getProperty(TEST_TYPE);
        if (type instanceof NullProperty) {
            return IGNORE;
        }
        return type.getIntValue();
    }

    public boolean isEqualsType() {
        return (getTestType() & EQUALS) != 0;
    }

    public boolean isIgnoreType() {
        return (getTestType() & IGNORE) != 0;
    }

    public boolean isContainsType() {
        return (getTestType() & CONTAINS) != 0;
    }

    @Override
    public AssertionResult getResult(SampleResult response) {
        AssertionResult result = new AssertionResult();
		try {
			result = evaluateResponse(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
	        //StringWriter sw = new StringWriter();
	        //e.printStackTrace(new PrintWriter(sw));
	        result.setFailureMessage(e.toString());
			result.setFailure(true);
	        result.setError(true);
		}
        return result;
    }
    


    /**
     * Make sure the response satisfies the specified assertion requirements.
     *
     * @param response
     *            an instance of SampleResult
     * @return an instance of AssertionResult
     * @throws Exception 
     */
    @SuppressWarnings("unused")
	private AssertionResult evaluateResponse(SampleResult response) throws Exception {
        AssertionResult result = new AssertionResult(getName());
        String jmeterVariable = "";
        String targetVariable = "";
        String sourceVariable = "";
        errmsg = "";
        Boolean bfound = false;
		String targetData = "";
		String sourceData = "";
        String originalJSON = response.getResponseDataAsString();
        ArrayList<String> sourceValues;
        ArrayList<String> targetValues;
        JSONArray jsonSourceArray = null;
        JSONObject jsonSourceObject = null;
        JSONArray jsonTargetArray = null;
        JSONObject jsonTargetObject = null;
        boolean bsourceJsonObject = false;
        boolean btargetJsonObject = false;
        boolean contains = isContainsType(); // do it once outside loop
        boolean equals = isEqualsType();
        boolean ignorecase = isIgnoreType();
        Boolean ifound = false;
        String sourceNodeValue = ""; // The target node value to check
        String targetNodeValue = "";
        String ssourcedata = "";
        String sourceVariables = "";
		
        // If baseline/source data are empty or null, tests are skipped as passed.
        if(originalJSON.isEmpty() || originalJSON == null || originalJSON.equals("[]")) {
    		result.setFailure(false);
            result.setError(false);
            return result;
        }
        
        // Now determine whether the target JSON data type are either JSONObject or JSONArray.
        JsonElement jsonElement = new JsonParser().parse(originalJSON);
        if(jsonElement.isJsonObject()) {
        	jsonTargetObject = new JSONObject(originalJSON);
        	btargetJsonObject = true;
        } else {
        	jsonTargetArray = new JSONArray(originalJSON);
        }

        // Get baseline and respond data.
        CollectionProperty[] data = getTestStrings();
		
        // Now determine whether the source JSON data type are either JSONObject or JSONArray.
	  	jmeterVariable = data[1].get(0).getStringValue();
		sourceNodeValue = getThreadContext().getVariables().get(jmeterVariable.substring(0, jmeterVariable.indexOf("."))); // get source JSON content.
		JsonElement sourceElement = new JsonParser().parse(sourceNodeValue);
        if(sourceElement.isJsonObject()) {
        	jsonSourceObject = new JSONObject(sourceNodeValue);
        	bsourceJsonObject = true;
        } else {
        	jsonSourceArray = new JSONArray(sourceNodeValue);
        }

	  	// Now get the list of target nodes.
		// This, need to find out the least node and then start iterate thru its children.
		for (int j=0; j<data[0].size(); j++){
   		  	  ifound = false;
    	  	  targetVariable = data[0].get(j).getStringValue();
	  		  if(btargetJsonObject) {
	    	  		ifound = comparePartnersData(jsonTargetObject,jsonSourceArray);
//	    	  		ifound = compareJobs(jsonTargetObject,jsonSourceArray);
	  		  }
	  		  else {
	    	  	ifound = comparePartnersData(jsonTargetArray,jsonSourceArray);
    	  		ifound = compareJobs(jsonTargetArray,jsonSourceArray);
	  		  }
//    	  	  if(targetVariable.contains("jobDetails[???]")){
//    	  		  if(btargetJsonObject) {
//    	    	  		ifound = compareJobs(jsonTargetObject,jsonSourceArray);
//    	  		  }
//    	  		  else {
//  	    	  		ifound = compareJobs(jsonTargetArray,jsonSourceArray);
//    	  		  }
//              }
//    	  	  else{
//                  if(!btargetJsonObject) {
//                      targetValues = JmeterUtils.getTargetData(jsonTargetArray, targetVariable);
//                  } else {
//                      targetValues = JmeterUtils.getTargetData(jsonTargetObject, targetVariable);
//                  }
//
//                  // Now get the list of source nodes.
//                  sourceVariable = data[1].get(j).getStringValue().substring(data[1].get(j).getStringValue().indexOf(".")+1);// extract source variable which contains JSON path.
//                  if(!bsourceJsonObject) {
//                      sourceValues = JmeterUtils.getTargetData(jsonSourceArray, sourceVariable);
//                  } else {
//                      sourceValues = JmeterUtils.getTargetData(jsonSourceObject, sourceVariable);
//                  }
//
//                  // Now validate if we can find the matched data...
//                  // reset ifound value to false...
//                  for (int l=0; l<targetValues.size(); l++){
//                      ifound = false;
//                      sourceVariables = "";
//                      //targetNodeValue = JmeterUtils.stripDiacritics(targetValues.get(l));
//                      targetNodeValue = targetValues.get(l);
//                      for(int m=0; m<sourceValues.size();m++){
//                          //ssourcedata = JmeterUtils.stripDiacritics(sourceValues.get(m));
//                          ssourcedata = sourceValues.get(m);
//                          if(JmeterUtils.getValidDateTime(ssourcedata,"yyyy-MM-dd'T'HH:mm:ss'Z'")!=0) {
//                              ssourcedata = String.format("/Date(%s+0000)",JmeterUtils.getValidDateTime(ssourcedata,"yyyy-MM-dd'T'HH:mm:ss'Z'"));
//                          }
//                          if(targetNodeValue.equals("") && (null==ssourcedata)
//                                  || !targetNodeValue.equals("") && JmeterUtils.compare(targetNodeValue,ssourcedata,contains,equals,ignorecase)){
//                              ifound = true;
//                              break;
//                          }
//                          else {
//                              sourceVariables += ssourcedata + ",\n\t";
//                          }
//                      }
//                      if(!ifound){
//                          localErrorMsg +="\r\nCan't find matched value between target and source... \r\n target json path: " + targetVariable + " target value: "
//                                  + targetNodeValue + " source json path: " + sourceVariable + " source values: " + sourceVariables;
//                      }
//                  }
//              }
	  }

      if(errmsg != "" && !bfound){
		result.setFailure(true);
        result.setError(true);
        result.setFailureMessage(errmsg);
	    bfound = false;
        }
        else{
			result.setFailure(false);
	        result.setError(false);
        }
      return result;
    }

    /**
     *
     * @param JSONObject target jobs
     * @param JSONArray source jobs
     * This method is specific for compare FIM jobs.
     *
     * @return String error info.
     */
	/*
	 * private boolean compareJobs(JSONObject targetJobs, JSONArray sourceJobs){
	 * String sTargetJobs = targetJobs.toString(); //String sSourceJobs =
	 * sourceJobs.toString(); net.minidev.json.JSONArray slist = new
	 * net.minidev.json.JSONArray(); net.minidev.json.JSONArray tlist;
	 * net.minidev.json.JSONArray polist; polist =
	 * JsonPath.read(sTargetJobs,"$.._id"); String partnerNumber =
	 * polist.get(0).toString(); tlist = JsonPath.read(sTargetJobs,"$..jobDetails");
	 * JSONObject sourceObject = new JSONObject(); for(int i=0;
	 * i<sourceJobs.length();i++){
	 * if(sourceJobs.getJSONObject(i).getString("employeeID").equals(partnerNumber))
	 * { sourceObject = sourceJobs.getJSONObject(i); break; } }
	 * if(sourceObject!=null){ slist = JsonPath.read(sourceObject.toString(),
	 * "$..jobs"); }
	 * 
	 * // First, compare current record. ArrayList<Object> targetEffectiveDates =
	 * JsonPath.read(tlist, "$..effectiveDate"); ArrayList<Object>
	 * sourceEffectiveDates = JsonPath.read(slist, "$..jobStartDate");
	 * ArrayList<Object> sourceJobEndDates = JsonPath.read(slist, "$..jobEndDate");
	 * Date today = new Date(); String tempTargetDate=""; String tempSourceDate="";
	 * String tempTargetJobNumber=""; String tempSourceJobNumber=""; String
	 * tempTargetJobName=""; String tempSourceJobName=""; String
	 * tempSourceStartDate=""; String tempSourceEndDate=""; String
	 * tempSourceTitle=""; String tempSourceEmployeeType=""; String
	 * tempSourceonTLA=""; String tempSourceJobGroup=""; String
	 * tempSourceSbuxIsHourly=""; String tempSourceSbuxPosName=""; String
	 * localErrorMsg = "";
	 * 
	 * // Get target's current date int iCountFutureRecords=0; for(int i=0;
	 * i<targetEffectiveDates.size(); i++){
	 * if(today.after(JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(
	 * ), "yyyy-MM-dd"))) { tempTargetDate =
	 * JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(),
	 * "yyyy-MM-dd").toString(); tempTargetJobNumber =
	 * ((ArrayList<String>)JsonPath.read(tlist, "$..jobNumber")).get(i);
	 * tempTargetJobName = ((ArrayList<String>)JsonPath.read(tlist,
	 * "$..name")).get(i); } else { iCountFutureRecords++; } }
	 * 
	 * // Get source's current date tempSourceDate =
	 * JmeterUtils.getValidDate(sourceEffectiveDates.get(0).toString(),
	 * "yyyy-MM-dd").toString(); tempSourceJobNumber =
	 * ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(0);
	 * tempSourceJobName = ((ArrayList<String>)JsonPath.read(slist,
	 * "$..name")).get(0); if(sourceJobEndDates.size()==1){ tempSourceEndDate =
	 * null; } else{ DateTimeFormatter formatter =
	 * DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH); LocalDate ld =
	 * LocalDate.parse(targetEffectiveDates.get(targetEffectiveDates.size()-
	 * iCountFutureRecords).toString(),formatter).minusDays(1); tempSourceEndDate =
	 * ld.toString(); }
	 * 
	 * // Now compare current Job records... if(tempSourceEndDate!=null &&
	 * sourceJobEndDates.get(0)!= null &&
	 * !tempSourceEndDate.equals(sourceJobEndDates.get(0).toString())){
	 * localErrorMsg +=String.
	 * format("Current Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
	 * ,tempSourceEndDate ,sourceJobEndDates.get(0)); }
	 * if(!tempTargetDate.equals(tempSourceDate)){ localErrorMsg +=String.
	 * format("Current effectiveDate is mismatched!!! \n\tTarget effectiveDate: %s\r\nSource effectiveDate: %s\r\n"
	 * ,tempTargetDate ,tempSourceDate); }
	 * if(!tempTargetJobNumber.equals(tempSourceJobNumber)){ localErrorMsg +=String.
	 * format("Current Job number is mismatched!!! \n\tTarget Job number: %s\r\nSource Job number: %s\r\n"
	 * ,tempTargetJobNumber ,tempSourceJobNumber); }
	 * if(!tempTargetJobName.equals(tempSourceJobName)){ localErrorMsg +=String.
	 * format("Current Job name is mismatched!!! \n\tTarget Job name: %s\r\nSource Job name: %s\r\n"
	 * ,tempTargetJobName ,tempSourceJobNumber); }
	 * 
	 * // Second, compare all furture records. int iSequenc=2; boolean bFound=false;
	 * int iCount=0; for(int i=targetEffectiveDates.size()-iCountFutureRecords;
	 * i<targetEffectiveDates.size(); i++){ bFound=false; tempTargetDate =
	 * JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(),
	 * "yyyy-MM-dd").toString(); tempTargetJobNumber =
	 * ((ArrayList<String>)JsonPath.read(tlist, "$..jobNumber")).get(i);
	 * tempTargetJobName = ((ArrayList<String>)JsonPath.read(tlist,
	 * "$..name")).get(i);
	 * if(today.before(JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString
	 * (), "yyyy-MM-dd"))) { for(int j=0; j<sourceEffectiveDates.size();j++){
	 * tempSourceDate =
	 * JmeterUtils.getValidDate(sourceEffectiveDates.get(j).toString(),
	 * "yyyy-MM-dd").toString(); tempSourceJobNumber =
	 * ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(j);
	 * tempSourceJobName = ((ArrayList<String>)JsonPath.read(slist,
	 * "$..name")).get(j);
	 * if(today.before(JmeterUtils.getValidDate(sourceEffectiveDates.get(j).toString
	 * (), "yyyy-MM-dd"))){ if(tempTargetDate.equals(tempSourceDate) &&
	 * tempTargetJobNumber.equals(tempSourceJobNumber) &&
	 * tempTargetJobName.equals(tempSourceJobName) &&
	 * String.valueOf(iSequenc).equals(((ArrayList<String>)JsonPath.read(slist,
	 * "$..sequence")).get(j).toString())) { iSequenc++;
	 * if(j<sourceEffectiveDates.size()-1){ DateTimeFormatter formatter =
	 * DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH); LocalDate ld =
	 * LocalDate.parse(targetEffectiveDates.get(i).toString(),formatter).minusDays(1
	 * ); tempSourceEndDate = ld.toString();
	 * if(!tempSourceEndDate.equals(sourceJobEndDates.get(j-iCount))){ localErrorMsg
	 * +=String.
	 * format("Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
	 * ,tempSourceEndDate ,sourceJobEndDates.get(j)); bFound=false; } else {
	 * bFound=true; break; } } else{ if(null!=sourceJobEndDates.get(j)){
	 * localErrorMsg +=String.
	 * format("Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
	 * ,null ,sourceJobEndDates.get(j)); bFound=false; } } bFound=true; break; } }
	 * else { iCount++; } } } if(!bFound){ localErrorMsg +=String.
	 * format("Future Job in sequence %s is mismatched!!! \r\nTarget Job name:%s  Source Job name:%s"
	 * + "\r\nTarget Job number:%s  Source Job number:%s" +
	 * "\r\nTarget EffectiveDate:%s  Source EffectiveDate:%s" ,iSequenc
	 * ,tempTargetJobName ,tempSourceJobName ,tempTargetJobNumber
	 * ,tempSourceJobNumber ,tempTargetDate ,tempSourceDate); } } return bFound; }
	 */
    /**
     *
     * @param JSONArray target jobs
     * @param JSONArray source jobs
     * This method is specific for compare FIM jobs.
     *
     * @return String error info.
     * @throws ParseException 
     */
    private boolean compareJobs(JSONArray targetJobs, JSONArray sourceJobs) throws ParseException{
        String sTargetJobs = targetJobs.toString();
        //String sSourceJobs = sourceJobs.toString();
        net.minidev.json.JSONArray slist = new net.minidev.json.JSONArray();
        net.minidev.json.JSONArray tlist = new net.minidev.json.JSONArray();
        net.minidev.json.JSONArray joblist;
        net.minidev.json.JSONArray polist;
        polist = JsonPath.read(sTargetJobs,"$.._id");
        String partnerNumber = polist.get(0).toString();
        JSONObject sourceObject = new JSONObject();
        JSONObject targetObject = new JSONObject();
        ArrayList<Object> targetEffectiveDates = new ArrayList<Object>();
        ArrayList<Object> targetEndDates = new ArrayList<Object>();
        ArrayList<Object> jobEffectiveDates = new ArrayList<Object>();
        ArrayList<Object> sourceEffectiveDates = new ArrayList<Object>();
        ArrayList<Object> sourceJobEndDates = new ArrayList<Object>();
        String tempTargetDate="";
        String tempTargetEndDate="";
        String tempEndDate ="";
        String tempSourceDate="";
        String tempTargetJobNumber="";
        String tempSourceJobNumber="";
        String tempSourceStartDate="";
        String tempSourceEndDate="";
        String tempTargetTitle="";
        String tempSourceTitle="";
        String tempTargetEmployeeType="";
        String tempSourceEmployeeType="";
        String tempTargetEmployeeClass="";
        String tempTargetOnTla="";
        String tempSourceOnTla="";
        String tempTargetJobGroup="";
        String tempSourceJobGroup="";
        String tempTargetIsHourly="";
        String tempSourceIsHourly="";
        String tempSourceSbuxIsHourly="";
        String tempTargetPosName="";
        String tempSourceSbuxPosName="";
        String tempSource ="";
        Integer tempSourceSeqNo;
        boolean bFound=false;
        String localErrorMsg = "";

        
        for(int it=0; it<polist.size(); it++) {
        	bFound = false;
        	localErrorMsg = "";
        	partnerNumber = polist.get(it).toString();
        	targetObject = targetJobs.getJSONObject(it);
            for(int i=0; i<sourceJobs.length();i++){
                if(sourceJobs.getJSONObject(i).getString("employeeID").equals(partnerNumber)){
                    sourceObject = sourceJobs.getJSONObject(i);
                    break;
                }
                else {
                	sourceObject = null;
                }
            }
            if(sourceObject!=null){
                slist = JsonPath.read(sourceObject.toString(), "$..jobs");
            }
            
           
            // First, compare current record.
            // Get target's current date
            tlist = JsonPath.read(targetObject.toString(),"$..EmpJob");
        	//tlist = JsonPath.read(targetObject.toString(), "$..PerEmail");

           // joblist = JsonPath.read(targetObject.toString(),"$..jobDetails");
            //jobEffectiveDates = JsonPath.read(joblist, "$..effectiveDate");
            targetEffectiveDates = JsonPath.read(tlist, "$..startDate");
            targetEndDates = JsonPath.read(tlist, "$..endDate");

            sourceEffectiveDates = JsonPath.read(slist, "$..jobStartDate");
            sourceJobEndDates = JsonPath.read(slist, "$..jobEndDate");
            int iCountFutureRecords=0;
            
            if(0 != tlist.size() && null != sourceObject) {
                // Now try to get current postionDetails and jobDetails data from DB...
                for(int i=0; i<targetEffectiveDates.size(); i++){               
                	String msDateString= targetEffectiveDates.get(i).toString();
                	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                	Matcher dateMSMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString);               	 	              	 	
               	    if(dateMSMatcher.find())
                      calendar.setTimeInMillis(Long.parseLong(dateMSMatcher.group(1), 10));
               	    Date targetEffectdate = calendar.getTime();
                    
                    DateFormat format = new SimpleDateFormat("YYYY-MM-dd");
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String targetStartdate="";
                    targetStartdate = format.format(targetEffectdate); 
                    
                    String msDateString1= targetEndDates.get(i).toString();
                	Calendar calendar1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                	Matcher dateMSMatcher1 = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString1);            	 	
               	    if(dateMSMatcher1.find())
                      calendar1.setTimeInMillis(Long.parseLong(dateMSMatcher1.group(1), 10));
               	    Date targetEnddate = calendar1.getTime();
                    
                    DateFormat format1 = new SimpleDateFormat("YYYY-MM-dd");
                    format1.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String targetEnddate1="";
                    targetEnddate1 = format1.format(targetEnddate); 
    				if(targetEnddate1.equals("10000-12-31")){
    					targetEnddate1 = "9999-12-31"; 
    					}
          
                    
                    if(today.after(targetEffectdate) && today.before(targetEnddate)) {
                        tempTargetDate = targetStartdate;
                        tempTargetEndDate = targetEnddate1;
                        tempTargetJobNumber = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("jobCode").toString();
                        tempTargetPosName = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("position").toString();
                        tempTargetIsHourly = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("employmentType").toString();
                        if (tempTargetIsHourly.equals("Hourly Pay")) {
                        	tempTargetIsHourly = "true";                        		
                    	}
                    	else {
                    		tempTargetIsHourly = "false";
                    	}
                        tempTargetEmployeeType = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("customString10").toString();
                        	if (tempTargetEmployeeType.equals("Retail") || tempTargetEmployeeType.equals("Retail Field Operations")) {
                        		tempTargetEmployeeType = "RETAIL";                        		
                        	}
                        	else {
                        		tempTargetEmployeeType = "NONRETAIL";
                        	}
                        tempTargetEmployeeClass = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("employeeClass").toString();
//                        if (tempTargetEmployeeClass.equals("CW")) {
//                        	tempTargetEmployeeType = "CW";
//                        }
                        
                        		
                        tempTargetTitle = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("positionName").toString().toLowerCase();
                        tempTargetOnTla = null;
                        tempTargetOnTla = (String) ((ArrayList)JsonPath.read(tlist, "$..customString31")).get(i);
                        if(tempTargetOnTla==null) {
                        	tempTargetOnTla="false";
                        }
                        else {
                        	tempTargetOnTla="true";
                        }
                        
                        /*// Now try to find the targeting current job details data...
                        for(int j=0; j<jobEffectiveDates.size(); j++) {
                            if(JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd").after(JmeterUtils.getValidDate(jobEffectiveDates.get(j).toString(), "yyyy-MM-dd"))
                            		|| 0 == JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd").compareTo(JmeterUtils.getValidDate(jobEffectiveDates.get(j).toString(), "yyyy-MM-dd"))) {
                                tempTargetJobNumber = ((HashMap)((ArrayList)joblist.get(0)).get(j)).get("jobNumber").toString();
                                tempTargetJobGroup = ((HashMap)((ArrayList)joblist.get(0)).get(j)).get("jobGroup").toString();
                            }
                        }*/
                        
                    }
                    else if (today.before(targetEffectdate) && today.before(targetEnddate)){
                        iCountFutureRecords++;
                    }
                }

              /*  if(partnerNumber.equals("28041592")) {
                	tempSource = null;                	
                }*/
                if(((ArrayList<String>)JsonPath.read(slist, "$..jobStartDate")).size()==0) {
                	tempSourceDate = null;
                	//localErrorMsg +=String.format("jobStartDate is missing !!!\n");                	

                	
                }
                else {
                	tempSourceDate = ((ArrayList<String>)JsonPath.read(slist, "$..jobStartDate")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).size()==0){
                	tempSourceJobNumber = null;
                }
                else {
                	tempSourceJobNumber = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..jobEndDate")).size()==0) {
                	tempSourceEndDate = null;
                	if (tempTargetEndDate.equals("9999-12-31")) {
                		tempTargetEndDate = null;
                	}

                }
                else {
                    tempSourceEndDate = ((ArrayList<String>)JsonPath.read(slist, "$..jobEndDate")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..sbuxIsHourly")).size()==0) {
                	tempSourceIsHourly = null;
                	//localErrorMsg +=String.format("sbuxIsHourly is missing !!!\n");                	

                }
                else {
                	tempSourceIsHourly = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxIsHourly")).get(0);
                }
                
                if(((ArrayList<Integer>)JsonPath.read(slist, "$..sequence")).size()==0) {
                	tempSourceSeqNo=0;
                	//localErrorMsg +=String.format("sequence is missing !!!\n");                	

                }
                else {
                tempSourceSeqNo = ((ArrayList<Integer>)JsonPath.read(slist, "$..sequence")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..employeeType")).size()==0) {
                	tempSourceEmployeeType = null;
                	//localErrorMsg +=String.format("employeeType is missing !!!\n");                	
                }
                else {
                	tempSourceEmployeeType = ((ArrayList<String>)JsonPath.read(slist, "$..employeeType")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..sbuxPosName")).size()==0) {
                	tempSourceSbuxPosName = null;
                	//localErrorMsg +=String.format("sbuxPosName is missing !!!\n");                	

                }
                else {
                	tempSourceSbuxPosName = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxPosName")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..title")).size()==0) {
                	tempSourceTitle = null;
                	//localErrorMsg +=String.format("title is missing !!!\n");                	

                }
                else {
                tempSourceTitle = ((ArrayList<String>)JsonPath.read(slist, "$..title")).get(0);
                }
                
                if(((ArrayList<String>)JsonPath.read(slist, "$..sbuxOnTLA")).size()==0) {
                	tempSourceTitle = null;
                	//localErrorMsg +=String.format("Source OnTla is missing !!!\n");                	

                }
                else {
                	tempSourceOnTla = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxOnTLA")).get(0);
                }

                // Now compare current Job records...
                if(tempTargetEndDate!=null && tempSourceEndDate!= null && !tempTargetEndDate.equals(tempSourceEndDate)){
                	localErrorMsg +=String.format("Current Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
                            ,tempSourceEndDate
                            ,sourceJobEndDates.get(0));
                }
                if(tempTargetDate!=null && tempSourceDate!= null && !tempTargetDate.equals(tempSourceDate)){
                	localErrorMsg +=String.format("Current Job Start Date is mismatched!!! \n\tTarget startDate: %s\r\nSource effectiveDate: %s\r\n"
                            ,tempTargetDate
                            ,tempSourceDate);
                }
                if(tempTargetJobNumber!=null && tempSourceJobNumber!= null && !tempTargetJobNumber.equals(tempSourceJobNumber)){
                	localErrorMsg +=String.format("Current Job number is mismatched!!! \n\tTarget Job number: %s\r\nSource Job number: %s\r\n"
                            ,tempTargetJobNumber
                            ,tempSourceJobNumber);
                }
                if(tempTargetTitle!=null && tempSourceTitle!= null && !tempTargetTitle.equals(tempSourceTitle)){
                	localErrorMsg +=String.format("Current title is mismatched!!! \n\tTarget title: %s\r\nSource title: %s\r\n"
                            ,tempTargetTitle
                            ,tempSourceTitle);
                }
                if(!tempTargetOnTla.equals(tempSourceOnTla)){
                	localErrorMsg +=String.format("Current onTla is mismatched!!! \n\tTarget onTla: %s\r\nSource onTla: %s\r\n"
                            ,tempTargetOnTla
                            ,tempSourceOnTla);
                }
                
                if(!tempTargetIsHourly.equals(tempSourceIsHourly)){
                	localErrorMsg +=String.format("Current isHourly is mismatched!!! \n\tTarget isHourly: %s\r\nSource isHourly: %s\r\n"
                            ,tempTargetIsHourly
                            ,tempSourceIsHourly);
                }
                if(tempTargetEmployeeType!=null && tempSourceEmployeeType!= null && !tempTargetEmployeeType.equals(tempSourceEmployeeType)){
                	localErrorMsg +=String.format("Current employeeType is mismatched!!! \n\tTarget employeeType: %s\r\nSource employeeType: %s\r\n"
                            ,tempTargetEmployeeType
                            ,tempSourceEmployeeType);
                }

                if(tempTargetPosName!=null && tempSourceSbuxPosName!= null && !tempTargetPosName.equals(tempSourceSbuxPosName)){
                	localErrorMsg +=String.format("Current posName is mismatched!!! \n\tTarget posName: %s\r\nSource posName: %s\r\n"
                            ,tempTargetPosName
                            ,tempSourceSbuxPosName);
                }
                
                if(tempSourceSeqNo!= null && !tempSourceSeqNo.equals(1)){
                	localErrorMsg +=String.format("Current Seq is mismatched!!! \n\tTarget posName: %s\r\nSource posName: %s\r\n"
                            ,1
                            ,tempSourceSeqNo);
                }

                //Compare all future records.
                int iSequenc=2;
                if(iCountFutureRecords==targetEffectiveDates.size()) {
                	iSequenc=1; // In the case there're all future records only...
                }
                int iCount=0;
                for(int i=targetEffectiveDates.size()-iCountFutureRecords; i<targetEffectiveDates.size(); i++){
                    bFound=false;
                    String msDateString= targetEffectiveDates.get(i).toString();
                	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                	Matcher dateMSMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString);
               	    if(dateMSMatcher.find())
                      calendar.setTimeInMillis(Long.parseLong(dateMSMatcher.group(1), 10));
               	    Date targetEffectdate = calendar.getTime();
                    
                    DateFormat format = new SimpleDateFormat("YYYY-MM-dd");
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String targetStartdate ="";
                    targetStartdate = format.format(targetEffectdate); 
                    
                    String msDateString1= targetEndDates.get(i).toString();
                	Calendar calendar1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                	Matcher dateMSMatcher1 = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString1);
               	    if(dateMSMatcher1.find())
                      calendar1.setTimeInMillis(Long.parseLong(dateMSMatcher1.group(1), 10));
               	    Date targetEnddate = calendar1.getTime();
                    
                    DateFormat format2 = new SimpleDateFormat("YYYY-MM-dd");
                    format2.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String targetEnddate1 ="";
                    targetEnddate1 = format2.format(targetEnddate); 
                    if(targetEnddate1.equals("10000-12-31")){
    					targetEnddate1 = "9999-12-31"; 
    					}
                    //if(today.before(targetEffectdate) && today.before(targetEnddate)) {
	                    tempTargetDate = targetStartdate;
	                    tempTargetEndDate = targetEnddate1;
	                    tempTargetJobNumber = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("jobCode").toString();
	                    tempTargetPosName  = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("position").toString();
	                    tempTargetIsHourly = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("employmentType").toString();
	                    if (tempTargetIsHourly.equals("Hourly Pay")) {
	                    	tempTargetIsHourly = "true";                        		
	                	}
	                	else {
	                		tempTargetIsHourly = "false";
	                	}	
	                    tempTargetEmployeeType = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("event").toString();
	                    if (tempTargetEmployeeType.equals("Retail") || tempTargetEmployeeType.equals("Retail Field Operations")) {
	                		tempTargetEmployeeType.equals("RETAIL") ;                        		
	                	}
	                	else {
	                		tempTargetEmployeeType = "NONRETAIL";
	                	}
	                    tempTargetEmployeeClass = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("employeeClass").toString();
	                    if (tempTargetEmployeeClass.equals("CW")) {
	                    	tempTargetEmployeeType = "CW";
	                    }
	                    
	                    tempTargetTitle = ((HashMap)((ArrayList)tlist.get(0)).get(i)).get("positionName").toString().toLowerCase();
	                    tempTargetOnTla = (String) ((ArrayList)JsonPath.read(tlist, "$..customString31")).get(i);
	
	                    if(tempTargetOnTla==null) {
	                    	tempTargetOnTla="false";
	                    }
	                    else {
	                    	tempTargetOnTla="true";
	                    }
                    
                     if(today.before(targetEffectdate) && today.before(targetEnddate)) {
                        for(int j=sourceEffectiveDates.size()-iCountFutureRecords; j<sourceEffectiveDates.size();j++){
                            tempSourceDate = ((ArrayList<String>)JsonPath.read(slist, "$..jobStartDate")).get(j);
                            try {
                                tempSourceEndDate = ((ArrayList<String>)JsonPath.read(slist, "$..jobEndDate")).get(j);
                            }
                            catch(Exception e){
                            	tempSourceEndDate = null;
                            	tempTargetEndDate = null;
                            }
                            tempSourceJobNumber = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(j);
                            tempSourceOnTla = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxOnTLA")).get(j);
                            tempSourceIsHourly = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxIsHourly")).get(j);
                            tempSourceEmployeeType = ((ArrayList<String>)JsonPath.read(slist, "$..employeeType")).get(j);
                            tempSourceSbuxPosName = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxPosName")).get(j);
                            tempSourceTitle = ((ArrayList<String>)JsonPath.read(slist, "$..title")).get(j);
                            if(today.before(targetEffectdate)){
                                if(tempTargetDate.equals(tempSourceDate)
                                	&& tempTargetEndDate==tempSourceEndDate	
                                    && tempTargetJobNumber.equals(tempSourceJobNumber)
                                    && String.valueOf(iSequenc).equals(((ArrayList<String>)JsonPath.read(slist, "$..sequence")).get(j).toString())
                                    && tempTargetIsHourly.equals(tempSourceIsHourly)
                                    && tempTargetEmployeeType.equals(tempSourceEmployeeType)
                                    && tempTargetPosName.equals(tempSourceSbuxPosName)
                                    && tempTargetOnTla.equals(tempSourceOnTla)
                                    && tempTargetTitle.equals(tempSourceTitle)) {
                                    iSequenc++;
                                    bFound=true;
                                    break;
                                }
                            }
                            else {
                            	iCount++;
                            }
                        
                        }
                     
                    if(!bFound){
                    	localErrorMsg +=String.format("future Job in sequence %s is mismatched!!! \r\nTarget position name:%s  Source position name:%s"
                                        + "\r\nTarget Job number:%s  Source Job number:%s"
                                        + "\r\nTarget Job Start date:%s  Source Job Start date:%s"
                                        + "\r\nTarget onTla:%s  Source onTla:%s"
                                        + "\r\nTarget isHourly:%s  Source isHourly:%s"
                                        + "\r\nTarget employeeType:%s  Source employeeType:%s"
                                        + "\r\nTarget position name:%s  Source position name:%s"
                                        + "\r\nTarget Job End date:%s  Source Job End date:%s"
                                        + "\r\nTarget EffectiveDate:%s  Source EffectiveDate:%s"
                                        + "\r\nTarget Title:%s  Source Title:%s"

                                ,iSequenc
                                ,tempTargetPosName
                                ,tempSourceSbuxPosName
                                ,tempTargetJobNumber
                                ,tempSourceJobNumber
                                ,tempTargetDate
                                ,tempSourceDate
                                ,tempTargetOnTla
                                ,tempSourceOnTla
                                ,tempTargetIsHourly
                                ,tempSourceIsHourly
                                ,tempTargetEmployeeType
                                ,tempSourceEmployeeType
                                ,tempTargetPosName
                                ,tempSourceSbuxPosName
                                ,tempTargetEndDate
                                ,tempSourceEndDate
                                ,tempTargetDate
                                ,tempSourceDate
                                ,tempTargetTitle
                                ,tempSourceTitle);
                    }else {
                        break;
                    }
                }
              }
            }
            if(!localErrorMsg.equals("")) {
            	errmsg += String.format("\r\n\r\n\r\nPartner %s has following data mismatched: \r\n%s", partnerNumber, localErrorMsg);
            }
       }
       if(errmsg.equals("")) {
    	   bFound=true;
       }
       return bFound;
    }

    /**
     *
     * @param JSONObject target jobs
     * @param JSONArray source jobs
     * This method is specific for compare FIM jobs.
     *
     * @return String error info.
     */
    private boolean comparePartnersData(JSONObject targetJobs, JSONArray sourceJobs){
        String sTargetJobs = targetJobs.toString();
        //String sSourceJobs = sourceJobs.toString();
        net.minidev.json.JSONArray slist = new net.minidev.json.JSONArray();
        net.minidev.json.JSONArray tlist;
        net.minidev.json.JSONArray polist;
        polist = JsonPath.read(sTargetJobs,"$.._id");
        String partnerNumber = polist.get(0).toString();
        tlist = JsonPath.read(sTargetJobs,"$..jobDetails");
        JSONObject sourceObject = new JSONObject();
        SimpleDateFormat datetimeLong = new SimpleDateFormat("yyyy-MM-dd'T'hh24:mm:ss'Z'");
        for(int i=0; i<sourceJobs.length();i++){
            if(sourceJobs.getJSONObject(i).getString("employeeID").equals(partnerNumber)){
                sourceObject = sourceJobs.getJSONObject(i);
                break;
            }
        }
        if(sourceObject!=null){
            slist = JsonPath.read(sourceObject.toString(), "$..jobs");
        }

        // First, compare current record.
        ArrayList<Object> targetEffectiveDates = JsonPath.read(tlist, "$..effectiveDate");
        ArrayList<Object> sourceEffectiveDates = JsonPath.read(slist, "$..jobStartDate");
        ArrayList<Object> sourceJobEndDates = JsonPath.read(slist, "$..jobEndDate");
        Date today = new Date();
        String tempTargetDate="";
        String tempSourceDate="";
        String tempTargetJobNumber="";
        String tempSourceJobNumber="";
        String tempTargetJobName="";
        String tempSourceJobName="";
        String tempSourceStartDate="";
        String tempSourceEndDate="";
        String tempSourceTitle="";
        String tempSourceEmployeeType="";
        String tempSourceonTLA="";
        String tempSourceJobGroup="";
        String tempSourceSbuxIsHourly="";
        String tempSourceSbuxPosName="";
        String localErrorMsg = "";

        // Get target's current date
        int iCountFutureRecords=0;
        for(int i=0; i<targetEffectiveDates.size(); i++){
            if(today.after(JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd"))) {
                tempTargetDate = JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd").toString();
                tempTargetJobNumber = ((ArrayList<String>)JsonPath.read(tlist, "$..jobNumber")).get(i);
                tempTargetJobName = ((ArrayList<String>)JsonPath.read(tlist, "$..name")).get(i);
            }
            else {
                iCountFutureRecords++;
            }
        }

        // Get source's current date
        tempSourceDate = JmeterUtils.getValidDate(sourceEffectiveDates.get(0).toString(), "yyyy-MM-dd").toString();
        tempSourceJobNumber = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(0);
        tempSourceJobName = ((ArrayList<String>)JsonPath.read(slist, "$..name")).get(0);
        if(sourceJobEndDates.size()==1){
            tempSourceEndDate = null;
        }
        else{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            LocalDate ld = LocalDate.parse(targetEffectiveDates.get(targetEffectiveDates.size()-iCountFutureRecords).toString(),formatter).minusDays(1);
            tempSourceEndDate = ld.toString();
        }

        // Now compare current Job records...
        if(tempSourceEndDate!=null && sourceJobEndDates.get(0)!= null && !tempSourceEndDate.equals(sourceJobEndDates.get(0).toString())){
        	localErrorMsg +=String.format("Current Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
                    ,tempSourceEndDate
                    ,sourceJobEndDates.get(0));
        }
        if(!tempTargetDate.equals(tempSourceDate)){
        	localErrorMsg +=String.format("Current effectiveDate is mismatched!!! \n\tTarget effectiveDate: %s\r\nSource effectiveDate: %s\r\n"
                    ,tempTargetDate
                    ,tempSourceDate);
        }
        if(!tempTargetJobNumber.equals(tempSourceJobNumber)){
        	localErrorMsg +=String.format("Current Job number is mismatched!!! \n\tTarget Job number: %s\r\nSource Job number: %s\r\n"
                    ,tempTargetJobNumber
                    ,tempSourceJobNumber);
        }
        if(!tempTargetJobName.equals(tempSourceJobName)){
        	localErrorMsg +=String.format("Current Job name is mismatched!!! \n\tTarget Job name: %s\r\nSource Job name: %s\r\n"
                    ,tempTargetJobName
                    ,tempSourceJobNumber);
        }

        // Second, compare all furture records.
        int iSequenc=2;
        boolean bFound=false;
        int iCount=0;
        for(int i=targetEffectiveDates.size()-iCountFutureRecords; i<targetEffectiveDates.size(); i++){
            bFound=false;
            tempTargetDate = JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd").toString();
            tempTargetJobNumber = ((ArrayList<String>)JsonPath.read(tlist, "$..jobNumber")).get(i);
            tempTargetJobName = ((ArrayList<String>)JsonPath.read(tlist, "$..name")).get(i);
            if(today.before(JmeterUtils.getValidDate(targetEffectiveDates.get(i).toString(), "yyyy-MM-dd"))) {
                for(int j=0; j<sourceEffectiveDates.size();j++){
                    tempSourceDate = JmeterUtils.getValidDate(sourceEffectiveDates.get(j).toString(), "yyyy-MM-dd").toString();
                    tempSourceJobNumber = ((ArrayList<String>)JsonPath.read(slist, "$..sbuxJobNumber")).get(j);
                    tempSourceJobName = ((ArrayList<String>)JsonPath.read(slist, "$..name")).get(j);
                    if(today.before(JmeterUtils.getValidDate(sourceEffectiveDates.get(j).toString(), "yyyy-MM-dd"))){
                        if(tempTargetDate.equals(tempSourceDate)
                            && tempTargetJobNumber.equals(tempSourceJobNumber)
                            && tempTargetJobName.equals(tempSourceJobName)
                            && String.valueOf(iSequenc).equals(((ArrayList<String>)JsonPath.read(slist, "$..sequence")).get(j).toString())) {
                            iSequenc++;
                            if(j<sourceEffectiveDates.size()-1){
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                                LocalDate ld = LocalDate.parse(targetEffectiveDates.get(i).toString(),formatter).minusDays(1);
                                tempSourceEndDate = ld.toString();
                                if(!tempSourceEndDate.equals(sourceJobEndDates.get(j-iCount))){
                                	localErrorMsg +=String.format("Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
                                            ,tempSourceEndDate
                                            ,sourceJobEndDates.get(j));
                                    bFound=false;
                                }
                                else {
                                	bFound=true;
                                	break;
                                }
                            }
                            else{
                                if(null!=sourceJobEndDates.get(j)){
                                	localErrorMsg +=String.format("Job End Date is mismatched!!! \n\tTarget Job End Date: %s\r\nSource Job End Date: %s\r\n"
                                            ,null
                                            ,sourceJobEndDates.get(j));
                                    bFound=false;
                                }
                            }
                            bFound=true;
                            break;
                        }
                    }
                    else {
                    	iCount++;
                    }
                }
            }
            if(!bFound){
            	localErrorMsg +=String.format("Future Job in sequence %s is mismatched!!! \r\nTarget Job name:%s  Source Job name:%s"
                                + "\r\nTarget Job number:%s  Source Job number:%s"
                                + "\r\nTarget EffectiveDate:%s  Source EffectiveDate:%s"
                        ,iSequenc
                        ,tempTargetJobName
                        ,tempSourceJobName
                        ,tempTargetJobNumber
                        ,tempSourceJobNumber
                        ,tempTargetDate
                        ,tempSourceDate);
            }
        }
        return bFound;
    }

    /**
     *
     * @param JSONArray target jobs
     * @param JSONArray source jobs
     * This method is specific for compare FIM jobs.
     *
     * @return String error info.
     * @throws ParseException 
     */
    private boolean comparePartnersData(JSONArray targetJobs, JSONArray sourceJobs) throws ParseException{
        String sTargetJobs = targetJobs.toString();
        //String sSourceJobs = sourceJobs.toString();
        net.minidev.json.JSONArray slist = new net.minidev.json.JSONArray();
        net.minidev.json.JSONArray tlist;
        net.minidev.json.JSONArray polist;
        polist = JsonPath.read(sTargetJobs,"$.._id");
        String partnerNumber = polist.get(0).toString();
        JSONObject sourceObject = new JSONObject();
        JSONObject targetObject = new JSONObject(); 
        ArrayList<Object> targetEffectiveDates = new ArrayList<Object>();
        ArrayList<Object> targetEndDates = new ArrayList<Object>();
        ArrayList<Object> sourceEffectiveDates = new ArrayList<Object>();
        ArrayList<Object> sourceJobEndDates = new ArrayList<Object>();
        ArrayList<Object> targetData = new ArrayList<Object>();
        Object tempTargetData="";
        Object tempSourceData="";
        Object tempTargetData1= "";
        boolean bFound=false;
        String localErrorMsg="";
        SimpleDateFormat datetimeLong = new SimpleDateFormat("yyyy-MM-dd'T'hh24:mm:ss'Z'");
   	 	Instant instant = Instant.now();
   	 	long today1 = instant.toEpochMilli();
        
        for(int it=0; it<polist.size(); it++) {
        	partnerNumber = polist.get(it).toString();
        	targetObject = targetJobs.getJSONObject(it);
            for(int i=0; i<sourceJobs.length();i++){
                if(sourceJobs.getJSONObject(i).getString("employeeID").equals(partnerNumber)){
                    sourceObject = sourceJobs.getJSONObject(i);
                    break;
                }
                else {
                	sourceObject = null;
                }
            }
            
            // Now start loop thru and compare all of fields...
            if(sourceObject!=null){
            	localErrorMsg="";
            	/* for China*/
            	// Check if the terminated partner's lastModifiedDataTime is greater than 30 days and current EmpJob.startDate is greater than 180 days,
            	// API should not send this partner to the merge file.
            	// If it does, raise the error and break the validation process.
            	if(((ArrayList)JsonPath.read(targetObject.toString(), "$..lastModifiedDateTime")).get(0)!=null
            		&& TimeUnit.DAYS.convert(today1 - datetimeLong.parse(((ArrayList)JsonPath.read(targetObject.toString(), "$..lastModifiedDateTime")).get(0).toString()).getTime(), TimeUnit.MILLISECONDS) > 30) {
                	tempTargetData = null;
                	targetEffectiveDates = null;
                	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
                	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
                	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
                	for(int i=0; i< targetEffectiveDates.size(); i++) {
                		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
                		String targetEndDate= targetEndDates.get(i).toString();
                   	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
                   	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
                   	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
                			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..startDate")).get(i);
                		}
                	}
                	if (TimeUnit.DAYS.convert(today1 - datetimeLong.parse(tempTargetData.toString()).getTime(), TimeUnit.MILLISECONDS) > 180) {
                		localErrorMsg = String.format("Partner %s should not be published to merge file due to lastMondifiedDateTime is greater than 30 days and EmpJob.startDate is greater than 180 days."
                				, partnerNumber);
                		break;
                	}
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeID")).size()==0) {
            		tempSourceData = null;
            		localErrorMsg +=String.format("employeeID is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeID")).get(0);
            	}
            	tempTargetData = null;
            	tempTargetData = ((ArrayList)JsonPath.read(targetObject.toString(), "$.._id")).get(0);
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("employeeID is mismatched!!! \n\tTarget UserId: %s\r\nSource employeeID: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxLocalMarketEmployeeID")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxLocalMarketEmployeeID is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxLocalMarketEmployeeID")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPerson");
            	if(tlist.isEmpty()) {
            		localErrorMsg +=String.format("Partner has PerPerson missing!!! \n\tTarget personIdExternal: %s\r\nSource personIdExternal: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		errmsg +=String.format("\r\n\r\n\r\nPartner %s has PerPerson missing:\r\n%s", partnerNumber, localErrorMsg);;
            		break;
            	}
            	tempTargetData = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..personIdExternal")).get(0).toString());
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxLocalMarketEmployeeID is mismatched!!! \n\tTarget personIdExternal: %s\r\nSource sbuxLocalMarketEmployeeID: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxGlobalID")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxGlobalID is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxGlobalID")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPerson");
            	if(tlist.isEmpty()) {
            		localErrorMsg +=String.format("Partner has PerPerson missing!!! \n\tTarget personIdExternal: %s\r\nSource personIdExternal: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		errmsg +=String.format("\r\n\r\n\r\nPartner %s has PerPerson missing:\r\n%s", partnerNumber, localErrorMsg);;
            		break;
            	}
            	tempTargetData = String.format("CN%s",JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..personIdExternal")).get(0).toString()).replaceFirst("^0+(?!$)", ""));
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxLocalMarketEmployeeID is mismatched!!! \n\tTarget personIdExternal: %s\r\nSource sbuxLocalMarketEmployeeID: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..samAccountName")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..samAccountName")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPerson");
            	if(tlist.isEmpty()) {
            		localErrorMsg +=String.format("Partner has PerPerson missing!!! \n\tTarget personIdExternal: %s\r\nSource personIdExternal: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		errmsg +=String.format("\r\n\r\n\r\nPartner %s has PerPerson missing:\r\n%s", partnerNumber, localErrorMsg);;
            		break;
            	}
            	if(((ArrayList)JsonPath.read(tlist.toString(), "$..customString1")).get(0) == null || ((ArrayList)JsonPath.read(tlist.toString(), "$..customString1")).size()==0){
            		tempTargetData = null;            		
            	}
            	else {
            		tempTargetData = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..customString1")).get(0).toString());
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("samAccountName is mismatched!!! \n\tTarget customString1: %s\r\nSource samAccountName: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxImmutableID")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxImmutableID")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPerson");
            	if(tlist.isEmpty()) {
            		localErrorMsg +=String.format("Partner has PerPerson missing!!! \n\tTarget personIdExternal: %s\r\nSource personIdExternal: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		errmsg +=String.format("\r\n\r\n\r\nPartner %s has PerPerson missing:\r\n%s", partnerNumber, localErrorMsg);;
            		break;
            	}
            	tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..customString11")).get(0);
            	if(((ArrayList)JsonPath.read(tlist.toString(), "$..customString11")).get(0) == null || ((ArrayList)JsonPath.read(tlist.toString(), "$..customString11")).size()==0){
            		tempTargetData = null;            		
            	}
            	else {
            		tempTargetData = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..customString11")).get(0).toString());
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxImmutableID is mismatched!!! \n\tTarget customString11: %s\r\nSource sbuxImmutableID: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeID")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeID")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..personIdExternal")).get(0).toString());
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("PerPersonal.personIdExternal is mismatched!!! \n\tTarget personIdExternal: %s\r\nSource personIdExternal: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxCompanyCode")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList<Integer>)JsonPath.read(sourceObject.toString(), "$..sbuxCompanyCode")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..company")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("CompanyCode is mismatched!!! \n\tTarget company: %s\r\nSource sbuxCompanyCode: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..company")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("company is missing !!!\n"); 
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..company")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	System.currentTimeMillis();
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..companyName")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("companyName is mismatched!!! \n\tTarget companyName: %s\r\nSource company: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..costCenter")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("costCenter is missing !!!\n"); 

            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..costCenter")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..costCenter")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("costCenter is mismatched!!! \n\tTarget costCenter: %s\r\nSource costCenter: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeStatus")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("employeeStatus is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..employeeStatus")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..event")).get(i);
            			if (tempTargetData.equals("26")) {
            				tempTargetData = "INACTIVE";            				
            			}
            			else {
            				tempTargetData = "ACTIVE"; 
            			}
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("employeeStatus is mismatched!!! \n\tTarget emplStatus: %s\r\nSource employeeStatus: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSapEmployeeGroup")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxSapEmployeeGroup is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSapEmployeeGroup")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..event")).get(i);
               	 	}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("employeeClass is mismatched!!! \n\tTarget employeeClass: %s\r\nSource sbuxSapEmployeeGroup: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..manager")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("manager is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..manager")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = "SFCN" + "-"+ ((ArrayList)JsonPath.read(tlist.toString(), "$..managerId")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("managerId is mismatched!!! \n\tTarget managerId: %s\r\nSource manager: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxManagerNumber")).size()==0) {
            		tempSourceData = null;
                	//localErrorMsg +=String.format("sbuxManagerNumber is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxManagerNumber")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..managerId")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxManagerNumber is mismatched!!! \n\tTarget managerId: %s\r\nsbuxManagerNumber: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..department")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("department is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..department")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
                    	if(((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnitDesc")).get(0) == null || ((ArrayList)JsonPath.read(tlist.toString(), "$..divisionDescription")).size()==0){
                    		tempTargetData= null;
                    	}
                    	else {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnitDesc")).get(i).toString();
                    	}
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("department is mismatched!!! \n\tTarget divisionDescription: %s\r\nSource department: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgShortName")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxOrgShortName is missing !!!\n"); 
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgShortName")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
                    	if(((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnit")).get(0) == null || ((ArrayList)JsonPath.read(tlist.toString(), "$..division")).size()==0){
                    		tempTargetData = null;
                    	}
                    	else {
               	 		tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnit")).get(i);
                    	}
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxOrgShortName is mismatched!!! \n\tTarget division: %s\r\nSource sbuxOrgShortName: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgName")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxOrgName is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgName")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
                    	if(((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnitDesc")).get(0) == null || ((ArrayList)JsonPath.read(tlist.toString(), "$..divisionDescription")).size()==0){
                    		tempTargetData = null;
                    	}
                    	else {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnitDesc")).get(i).toString().toUpperCase();
                    	}
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxOrgName is mismatched!!! \n\tTarget divisionDescription: %s\r\nSource sbuxOrgName: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgUnit")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxOrgUnit is missing !!!\n");  
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxOrgUnit")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..businessUnit")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxOrgUnit is mismatched!!! \n\tTarget businessUnit: %s\r\nSource sbuxOrgUnit: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..location")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..location")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..customString5")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("location is mismatched!!! \n\tTarget customString5: %s\r\nSource location: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxLocalMarket")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxLocalMarket is missing !!!\n");

            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxLocalMarket")).get(0);
                	tempTargetData = null;
                	tempTargetData = "CN";
            	}
            	
            	if(!tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxLocalMarket is mismatched!!! \n\tTarget LocalMarket: %s\r\nSource sbuxLocalMarket: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSourceSystem")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxSourceSystem is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSourceSystem")).get(0);
                	tempTargetData = null;
                	tempTargetData = "SFCN";
            	}
            	
            	if(!tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxSourceSystem is mismatched!!! \n\tTarget SourceSystem: %s\r\nSource sbuxSourceSystem: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..c")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..c")).get(0);
                	tempTargetData = null;
                	tempTargetData = "CN";
            	}
            	
            	if(!tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("Countrycode is mismatched!!! \n\tTarget Countrycode: %s\r\nSource Countrycode: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxRehireDate")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxRehireDate is missing !!!\n");
            		
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxRehireDate")).get(0);
            	}
            	tempTargetData = "";
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
               	 		if (((ArrayList)JsonPath.read(tlist.toString(), "$..employmentStartDate")).size()==0){
               	 		tempTargetData = null;
               	 		}
               	 		else {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..employmentStartDate")).get(i);
               	 		}
            		}
            	}
            	if(null !=tempTargetData) {
	                String msDateString= tempTargetData.toString();
	            	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	            	Matcher dateMSMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString);                 	 	
	           	    if(dateMSMatcher.find())
	                  calendar.setTimeInMillis(Long.parseLong(dateMSMatcher.group(1), 10));
	           	    Date targetEffectdate = calendar.getTime();                
	                DateFormat format = new SimpleDateFormat("YYYY-MM-dd");
	                format.setTimeZone(TimeZone.getTimeZone("UTC"));
	                String targetsbuxRehireDate ="";
	                targetsbuxRehireDate = format.format(targetEffectdate);
	                tempTargetData = targetsbuxRehireDate;
            	}
            	
				if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxRehireDate is mismatched!!! \n\tTarget employmentStartDate: %s\r\nSource sbuxRehireDate: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSeparationDate")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxSeparationDate is missing !!!\n");
            		
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSeparationDate")).get(0);
            	}
            	tempTargetData = "";
            	tlist = JsonPath.read(targetObject.toString(), "$..EmpJob");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
               	 		if (((ArrayList)JsonPath.read(tlist.toString(), "$..startDate")).size()==0){
               	 		tempTargetData = null;
               	 		}
               	 		else {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..startDate")).get(i);
               	 		}
            		}
            	}
            	if(null !=tempTargetData) {
	                String msDateString= tempTargetData.toString();
	            	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	            	Matcher dateMSMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString);                 	 	
	           	    if(dateMSMatcher.find())
	                  calendar.setTimeInMillis(Long.parseLong(dateMSMatcher.group(1), 10));
	           	    Date targetEffectdate = calendar.getTime();                
	                DateFormat format = new SimpleDateFormat("YYYY-MM-dd");
	                format.setTimeZone(TimeZone.getTimeZone("UTC"));
	                String targetsbuxRehireDate ="";
	                targetsbuxRehireDate = format.format(targetEffectdate);
	                tempTargetData = targetsbuxRehireDate;
            	}
            	
				if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxSeparationDate is mismatched!!! \n\tTarget startDate: %s\r\nSource sbuxSeparationDate: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
           	
            	
           	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..givenName")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("givenName is missing !!!\n");
            		
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..givenName")).get(0);
            	}
            	tempTargetData = null;
            	targetEffectiveDates = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..secondLastName")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("givenName is mismatched!!! \n\tTarget preferredName: %s\r\nSource givenName: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxGivenNameDB")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxGivenNameDB is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxGivenNameDB")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxGivenNameDB is mismatched!!! \n\tTarget preferredName: %s\r\nSource sbuxGivenNameDB: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sn")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sn is missing !!!\n");

            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sn")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("lastName is mismatched!!! \n\tTarget thirdName: %s\r\nSource sn: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSnDB")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxSnDB is missing !!!\n");

            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxSnDB")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxSnDB is mismatched!!! \n\tTarget thirdName: %s\r\nSource sbuxSnDB: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxKnownAs")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxKnownAs")).get(0);
            	}
            	tempTargetData = null;
            	tempTargetData1 = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			//tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..birthName")).get(i);
                    	tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);

            		}
            	}
//            	if (null==tempTargetData) {
//            		tempTargetData=tempTargetData1;
//            	}          	            		
            	
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)){
            		if(!tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxKnownAs is mismatched!!! \n\tTarget birthName or preferredName: %s\r\nSource sbuxKnownAs: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		}
            	}
/*---------------------------------------------------------------------------sbuxKnownAsDB-------------------------------*/            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxKnownAsDB")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxKnownAsDB")).get(0);
            	}
            	tempTargetData = null;
            	tempTargetData1 = null;            	
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	//tempTargetData = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..birthName")).get(0).toString());
            	//tempTargetData1 = JmeterUtils.stripDiacritics(((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(0).toString());
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..birthName")).get(i);
                    	tempTargetData1 = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);

            		}
            	}
            	if (null==tempTargetData) {
            		tempTargetData=tempTargetData1;
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)){
            		if(!tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxKnownAsDB is mismatched!!! \n\tTarget birthName or preferredName : %s\r\nSource sbuxKnownAsDB: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            		}
            	}
/* --------------------------------------------Full Name-------------------------------------------------------------------*/            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..fullName")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("fullName is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..fullName")).get(0);
            	}
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);
                    	tempTargetData1 = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);

            		}
            	}
            	tempTargetData += " " + tempTargetData1;
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("fullName is mismatched!!! \n\tTarget fullName: %s\r\nSource fullName: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	
/*---------------------------------------------------sbuxFullNameDB---------------------------------------------*/            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameDB")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxFullNameDB is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameDB")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);
                    	tempTargetData1 = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);

            		}
            	}
            	tempTargetData += " " + tempTargetData1;
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxFullNameDB is mismatched!!! \n\tTarget sbuxFullNameDB: %s\r\nSource sbuxFullNameDB: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
 /*-----------------------------------------------------------------sbuxFullNameAKA-----------------------------------------------*/
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameAKA")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxFullNameAKA is missing !!!\n");
            		
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameAKA")).get(0);
            	}
            	tempTargetData = "";
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..birthName")).get(i);
            			if (tempTargetData == null){
                			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);
            			}
                    	tempTargetData1 = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);

            		}
            	}
            	tempTargetData += " " + tempTargetData1;

            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxFullNameAKA is mismatched!!! \n\tTarget sbuxFullNameAKA: %s\r\nSource sbuxFullNameAKA: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
/*----------------------------------------------------------------sbuxFullNameAKADB-----------------------------------------*/            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameAKADB")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxFullNameAKADB is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxFullNameAKADB")).get(0);
            	}
            	tempTargetData = "";
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){            		              	 		
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..birthName")).get(i);
            			if (tempTargetData == null){
                			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..preferredName")).get(i);
            			}
                    	tempTargetData1 = ((ArrayList)JsonPath.read(tlist.toString(), "$..thirdName")).get(i);

            		}
            	}
            	tempTargetData += " " + tempTargetData1;

            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxFullNameAKA is mismatched!!! \n\tTarget sbuxFullNameAKA: %s\r\nSource sbuxFullNameAKA: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
/*-----------------------------------------------------------------Birth date-----------------------------------------------------*/            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxBirthDate")).size()==0) {
            		tempSourceData = null;
            		//localErrorMsg +=String.format("sbuxBirthDate is missing !!!\n");
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..sbuxBirthDate")).get(0);
            	}
            	tempTargetData = "";
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPersonal");
            	tempTargetData1 = "";
            	targetEffectiveDates = JsonPath.read(tlist.toString(), "$..startDate");
            	targetEndDates = JsonPath.read(tlist.toString(), "$..endDate");
            	for(int i=0; i< targetEffectiveDates.size(); i++) {
            		String targetEffectiveDate= targetEffectiveDates.get(i).toString();
            		String targetEndDate= targetEndDates.get(i).toString();
               	 	long startdates1 = datetimeLong.parse(targetEffectiveDate).getTime();
               	 	long enddates1 = Long.parseLong(targetEndDate.substring(targetEndDate.indexOf("(") + 1, targetEndDate.indexOf(")")));
               	 	if (today1 >= startdates1 && today1<=enddates1){
               	 		if (((ArrayList)JsonPath.read(tlist.toString(), "$..customDate1")).size()==0){
               	 		tempTargetData = null;
               	 		}
               	 		else {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..customDate1")).get(i);
               	 		}
            		}
            	}
            	if(null !=tempTargetData) {
	                String msDateString= tempTargetData.toString();
	            	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	            	Matcher dateMSMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(msDateString);                 	 	
	           	    if(dateMSMatcher.find())
	                  calendar.setTimeInMillis(Long.parseLong(dateMSMatcher.group(1), 10));
	           	    Date targetEffectdate = calendar.getTime();                
	                DateFormat format = new SimpleDateFormat("MM-dd");
	                format.setTimeZone(TimeZone.getTimeZone("UTC"));
	                String targetBirthdate ="";
	                targetBirthdate = format.format(targetEffectdate);
	                tempTargetData = targetBirthdate;
            	}
            	
				if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("sbuxBirthDate is mismatched!!! \n\tTarget customDate1: %s\r\nSource sbuxBirthDate: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	
            	
/*-----------------------------------------------------------------mail-----------------------------------------------------*/            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..mail")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..mail")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerEmail");
            	if(0 == tlist.size()) {
            		tempTargetData = null;
            	}else {
                	targetData = JsonPath.read(tlist.toString(), "$..emailType");
                	for(int i=0; i< targetData.size(); i++) {
                		if(targetData.get(i).toString().equals("Company Email")) {
                			if(0 == ((ArrayList)JsonPath.read(tlist.toString(), "$..emailAddress")).size()) {
                				tempTargetData = null;
                			}
                			else {
                				HashMap list = (HashMap)((ArrayList)tlist.get(0)).get(i);
                				tempTargetData = list.get("emailAddress");
                			}
                			//tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..address")).get(i);
                			break;
                		}
                	}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("mail is mismatched!!! \n\tTarget mail: %s\r\nSource mail: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..mobile")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..mobile")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPhone");
            	targetData = JsonPath.read(tlist.toString(), "$..phoneType");
            	for(int i=0; i< targetData.size(); i++) {
            		if(targetData.get(i).toString().equals("Mobile")) {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..phoneNumber")).get(i);
            			break;
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("mobile is mismatched!!! \n\tTarget mobile: %s\r\nSource mobile: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);
            	}            	
            	
            	if(((ArrayList)JsonPath.read(sourceObject.toString(), "$..telephoneNumber")).size()==0) {
            		tempSourceData = null;
            	}
            	else {
                	tempSourceData = ((ArrayList)JsonPath.read(sourceObject.toString(), "$..telephoneNumber")).get(0);
            	}
            	tempTargetData = null;
            	tlist = JsonPath.read(targetObject.toString(), "$..PerPhone");
            	targetData = JsonPath.read(tlist.toString(), "$..phoneType");
            	for(int i=0; i< targetData.size(); i++) {
            		if(targetData.get(i).toString().equals("Company Telephone")) {
            			tempTargetData = ((ArrayList)JsonPath.read(tlist.toString(), "$..phoneNumber")).get(i);
            			break;
            		}
            	}
            	if(null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) {
                	localErrorMsg +=String.format("Company Telephone is mismatched!!! \n\tTarget Company Telephone: %s\r\nSource telephoneNumber: %s\r\n"
                            ,tempTargetData
                            ,tempSourceData);               	
            	}	
            	
            	
	     	
/* for China*/   
                	
           	
            	if (!localErrorMsg.equals("")) {
            		errmsg += String.format("\r\n\r\n\r\nPartner %s has following mismatched:\r\n%s", partnerNumber, localErrorMsg);
            	}
            }
	   }
       if(errmsg.equals("")) {
    	   bFound=true;
       }
       return bFound;
    }
}


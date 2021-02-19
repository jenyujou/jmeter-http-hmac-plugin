package com.sbux.jmeter.assertions;

import java.io.Serializable;
import java.util.ArrayList;

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

/**
 * Test element to handle Response Data Assertions, @see AssertionGui
 *
 * @author jjou
 *
 */
public class ResponseJsonDataAssertion extends AbstractScopedAssertion implements Serializable, Assertion {

    private static final long serialVersionUID = 260L;

    private static final String TEST_FIELD_JMETER_VARIABLES = "assertion_jmeter_variables"; // $NON-NLS-1$ 

    private static final String TEST_FIELD_TARGET = "assertion_response_json_fields"; // $NON-NLS-1$

    private static final String TEST_REQUIRED_FIELD = "assertion_required_field"; // $NON-NLS-1$

    private static final String TEST_TYPE = "assertion_test_type"; // $NON-NLS-1$

    /*
     * Mask values for TEST_TYPE TODO: remove either MATCH or CONTAINS - they
     * are mutually exclusive
     */
    private static final int IGNORE = 1; // 1 << 0;

    private static final int CONTAINS = 1 << 1;

    private static final int EQUALS = 1 << 2;

    // Mask should contain all types (but not NOT)
    private static final int TYPE_MASK = IGNORE | EQUALS | CONTAINS ;

    public ResponseJsonDataAssertion() {
        setProperty(new CollectionProperty(TEST_FIELD_TARGET, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_FIELD_JMETER_VARIABLES, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_REQUIRED_FIELD, new ArrayList<String>()));
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
    }

    public CollectionProperty[] getTestStrings() {
    	CollectionProperty[] tempCol = new CollectionProperty[3];
    	tempCol[0] = (CollectionProperty) getProperty(TEST_FIELD_TARGET);
    	tempCol[1] = (CollectionProperty) getProperty(TEST_FIELD_JMETER_VARIABLES);
    	tempCol[2] = (CollectionProperty) getProperty(TEST_REQUIRED_FIELD);
        return tempCol;
    }

    public void addTestString(String responseField, String jmeterVariable, String requiredField) {
    	getTestStrings()[0].addProperty(new StringProperty(String.valueOf(responseField.hashCode()), responseField));
    	getTestStrings()[1].addProperty(new StringProperty(String.valueOf(jmeterVariable.hashCode()), jmeterVariable));
    	getTestStrings()[2].addProperty(new StringProperty(String.valueOf(requiredField.hashCode()), requiredField));
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
			result.setFailure(true);
	        result.setError(true);
	        result.setFailureMessage(e.getMessage());
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
    private AssertionResult evaluateResponse(SampleResult response) throws Exception {
        AssertionResult result = new AssertionResult(getName());
        String jmeterVariable = "";
        String responseVariable = "";
        String errmsg = "";
        Boolean ifound = false;
        String targetNodeValue = ""; // The target node value to check
        boolean contains = isContainsType(); // do it once outside loop
        boolean equals = isEqualsType();
        boolean ignorecase = isIgnoreType();
        String jmeterVariableValues = "";
        int totalb = 1;
        String sTotalFromVariable = "";
        JSONObject jsonObject;
        String originalJSON = response.getResponseDataAsString();
        JSONArray jsonTargetArray = null;
        JSONObject jsonTargetObject = null;
        boolean btargetJsonObject = false;
        ArrayList<String> targetValues;
        String targetVariable = "";
        String ssourcedata = "";
	        
        // In the case the JSON data start with array structure, we need to strip out the first array first. 
//        if(originalJSON.substring(0, 1).equals("[") && originalJSON.substring(originalJSON.length()-1).equals("]")){
//        	originalJSON=originalJSON.substring(1,originalJSON.length()-1);
//        }
        
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
		
		// Now get the list of target nodes.
		// This, need to find out the least node and then start iterate thru its children.
		for (int j=0; j<data[0].size(); j++){
   		  	  ifound = false;
    	  	  targetVariable = data[0].get(j).getStringValue();
    	  	  
    	  	  if(!btargetJsonObject) {
    	  		targetValues = JmeterUtils.getTargetData(jsonTargetArray, targetVariable); // The target node value to check....
    	  	  } else {
    	  		targetValues = JmeterUtils.getTargetData(jsonTargetObject, targetVariable);
    	  	  }
    	  	  
   		  	  //jsonObject = new JSONObject(originalJSON); 
   		  	  targetNodeValue = "";
   		  	  jmeterVariableValues = "";
	   		  jmeterVariable = data[1].get(j).getStringValue();
	        	
	   		  // Jmeter has a glitch to handle variables when users define them in the UI loop. So we need to have additional handle for this.
	   		  // Retrive total counts of source Jmeter variable data.
	   		  jmeterVariable = jmeterVariable.replace(",","").trim(); 
        	  sTotalFromVariable = getThreadContext().getVariables().get(jmeterVariable+"_#");
        	  totalb = Integer.parseInt(sTotalFromVariable==null?"0":sTotalFromVariable);

    	  	  responseVariable = data[0].get(j).getStringValue();
    	  	  //ArrayList<String> jsonvalues = JmeterUtils.getTargetData(jsonObject, responseVariable); // Retrieve target Json value. 
			  // Now validate if we can find the matched data...
		  	  // reset ifound value to false...
    	  	  for(int m=0; m<targetValues.size();m++){
	    		  targetNodeValue = targetValues.get(m);
    	  		  for (int l=0; l<totalb; l++){
   	 	    		ssourcedata = getThreadContext().getVariables().get(jmeterVariable+"_"+(l+1));
   	 	    		if(JmeterUtils.getValidDateTime(ssourcedata,"yyyy-MM-dd'T'HH:mm:ss'Z'")!=0) {
   	 	    			ssourcedata = String.format("/Date(%s+0000)",JmeterUtils.getValidDateTime(ssourcedata,"yyyy-MM-dd'T'HH:mm:ss'Z'"));
   	 	    		}
                  	if(targetNodeValue.equals("") && (null==ssourcedata)
                  			|| !targetNodeValue.equals("") && JmeterUtils.compare(targetNodeValue,ssourcedata,contains,equals,ignorecase)){
 		        		  ifound = true;
 		        		  break;
 	 	        		}
                  	else {
                  		jmeterVariableValues += ssourcedata + ",\n\t";
                  	}
 	    		  }
    	  		  if(!ifound){
        			errmsg += "\r\nCan't find matched value between target and source... \r\n target node: " + responseVariable + " target value: " 
        					+ targetNodeValue + " source jmeter variable: " + jmeterVariable + " jmeter variable's values: " + jmeterVariableValues;
	        	 }
 	    	  	}
	   		  }
		  	  
			// In the case if service skips returning node because no value, we need to handle this 
		  	// situation intelligently...
	        if(errmsg != ""){
				result.setFailure(true);
		        result.setError(true);
		        result.setFailureMessage(errmsg);
			    ifound = false;
		        }
		        else{
					result.setFailure(false);
			        result.setError(false);
		        }
      	return result;
    }
}

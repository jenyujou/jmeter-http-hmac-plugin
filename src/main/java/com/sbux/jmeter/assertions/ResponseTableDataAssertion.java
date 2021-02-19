package com.sbux.jmeter.assertions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.IntegerProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.StringProperty;

import com.sbux.jmeter.util.JmeterUtils;

/**
 * Test element to handle Response Data Assertions, @see AssertionGui
 *
 * @author jjou
 *
 */
public class ResponseTableDataAssertion extends AbstractScopedAssertion implements Serializable, Assertion {

    private static final long serialVersionUID = 260L;

    private static final String TEST_FIELD_JMETER_VARIABLES = "Assertion_jmeter_variables"; // $NON-NLS-1$ 

    private static final String TEST_FIELD_TARGET = "Assertion_response_table_fields"; // $NON-NLS-1$

    private static final String TEST_REQUIRED_FIELD = "assertion_required_field"; // $NON-NLS-1$

    private static final String TEST_TYPE = "Assertion_test_type"; // $NON-NLS-1$

    /*
     * Mask values for TEST_TYPE TODO: remove either MATCH or CONTAINS - they
     * are mutually exclusive
     */
    private static final int IGNORE = 1; // 1 << 0;

    private static final int CONTAINS = 1 << 1;

    private static final int EQUALS = 1 << 2;

    // Mask should contain all types (but not NOT)
    private static final int TYPE_MASK = IGNORE | EQUALS | CONTAINS ;

    public ResponseTableDataAssertion() {
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
	        result.setFailureMessage(e.getMessage()+'\n'+e.getStackTrace());
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
    private AssertionResult evaluateResponse(SampleResult response) throws Exception {
        TimeUnit.SECONDS.sleep(1);
        AssertionResult result = new AssertionResult(getName());
        String jmeterVariable = "";
        String responseVariable = "";
        String errmsg = "";
        Boolean ifound = false;
		String targetData = "";
		String sourceData = "";
        boolean contains = isContainsType(); // do it once outside loop
        boolean equals = isEqualsType();
        boolean ignorecase = isIgnoreType();
        String jmeterVariableValues = "";
        int totala = 1;
        int totalb = 1;
        String sTotalFromVariable = "";
        
        // Get baseline and respond data.
        CollectionProperty[] data = getTestStrings();
		
		// Now get the list of target nodes.
		// This, need to find out the least node and then start iterate thru its children.
		for (int j=0; j<data[0].size(); j++){
   		  	  ifound = false;
	   		  jmeterVariable = data[1].get(j).getStringValue();
	   		  jmeterVariableValues = "";
	        	
	   		  //Jmeter has a glitch to handle variables when users define them in the UI loop. So we need to have additional handle for this.
	   		  jmeterVariable = jmeterVariable.replace(",","").trim(); 
	        	  sTotalFromVariable = getThreadContext().getVariables().get(jmeterVariable+"_#");
	        	  totalb = Integer.parseInt(sTotalFromVariable==null?"0":sTotalFromVariable);
	   		  
	   		  responseVariable = data[0].get(j).getStringValue();
	   		  String stotala = getThreadContext().getVariables().get(responseVariable.trim()+"_#");
        	  
	   		  // If not found any targeting data, stop validation and return errors.
	   		  if(stotala==null) {
	   			  errmsg += "Cannot find target data: "+responseVariable+"\r\n";
	   		  }
	   		  else {
		   		  totala = Integer.parseInt(stotala);

		  	  	  // Iterate thru the target field.
				  // Now validate if we can find the matched data...
			  	  // reset ifound value to false...
	 	    	  for (int l=0; l<totala; l++){
	 	    		  for(int k=0; k<totalb; k++) {
	 	 		   		 targetData = JmeterUtils.convertString(getThreadContext().getVariables().get(responseVariable.trim()+"_"+(l+1)),"UTF-8");
	 	 	    		 sourceData = JmeterUtils.convertString(getThreadContext().getVariables().get(jmeterVariable+"_"+(k+1)),"UTF-8");
	 	 	 	        	if(null != targetData && (targetData.equals("") && (null==sourceData)
	 	 		        			|| !targetData.equals("") && JmeterUtils.compare(targetData,sourceData,contains,equals,ignorecase))){
	 	 		        		  ifound = true;
	 	 		        		  break;
	 	 	 	        		}
	 	    		  	}
		 	        	if(!ifound){
		 				  if(data[2].get(j).getStringValue().trim().equals("0")) { // It's an optional field.
		 					  break;
		 				  	}

		 				  	// Get not matched baseline.
		 				  	jmeterVariableValues += getThreadContext().getVariables().get(jmeterVariable+"_"+(l+1)) + ",\n\t";
		 				  
		 				  	String getTargetValues = "";
		    				for(int ipos=0; ipos<data[0].size(); ipos++) {
		    					getTargetValues += data[0].get(ipos).getStringValue() + "=" 
		    								+ JmeterUtils.convertString(getThreadContext().getVariables().get(data[0].get(ipos).getStringValue().trim()+"_"+(l+1)),"UTF-8")
		    								+ ", ";
		    				}
		 				  	
		  	        		errmsg += "\r\nCan't find target node info from jmeter variables... \r\n target data: "
		  	        				+ getTargetValues + responseVariable + "=" 
	  	        					+ targetData + "\n\tjmeter variable: " + jmeterVariable + " jmeter variable's values: " + jmeterVariableValues;
		  	        	}
		 	        	else {
		 	        		ifound = false; // reset.
		 	        	}
	    		  	}
	   		  	}
          }

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

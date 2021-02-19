package com.sbux.jmeter.assertions;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sbux.jmeter.util.JmeterUtils;

/**
 * Test element to handle Response Data Assertions, @see AssertionGui
 *
 * @author jjou
 *
 */
public class ResponseJsonXmlDataAssertion extends AbstractScopedAssertion implements Serializable, Assertion {

    private static final long serialVersionUID = 260L;

    private static final String TEST_FIELD_XML = "assertion_xml_variables"; // $NON-NLS-1$ 

    private static final String TEST_FIELD_TARGET = "assertion_response_json_fields"; // $NON-NLS-1$

    private static final String TEST_REQUIRED_FIELD = "assertion_required_field"; // $NON-NLS-1$

    private static final String TEST_TYPE = "assertion_test_type"; // $NON-NLS-1$

    private static final String TEST_XMLFILE_FIELD = "assertion_xmlfile_field"; // $NON-NLS-1$

    /*
     * Mask values for TEST_TYPE TODO: remove either MATCH or CONTAINS - they
     * are mutually exclusive
     */
    private static final int IGNORE = 1; // 1 << 0;

    private static final int CONTAINS = 1 << 1;

    private static final int EQUALS = 1 << 2;

    // Mask should contain all types (but not NOT)
    private static final int TYPE_MASK = IGNORE | EQUALS | CONTAINS ;

    public AssertionResult result = new AssertionResult(getName());

    public ResponseJsonXmlDataAssertion() {
        setProperty(new CollectionProperty(TEST_FIELD_TARGET, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_FIELD_XML, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_REQUIRED_FIELD, new ArrayList<String>()));
        setProperty(TEST_XMLFILE_FIELD, "");
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
    	getxmlfileStrings().setObjectValue("");
    }

    public CollectionProperty[] getTestStrings() {
    	CollectionProperty[] tempCol = new CollectionProperty[3];
    	tempCol[0] = (CollectionProperty) getProperty(TEST_FIELD_TARGET);
    	tempCol[1] = (CollectionProperty) getProperty(TEST_FIELD_XML);
    	tempCol[2] = (CollectionProperty) getProperty(TEST_REQUIRED_FIELD);
        return tempCol;
    }
    
    public JMeterProperty getxmlfileStrings() {
    	return getProperty(TEST_XMLFILE_FIELD);
    }

    public void addTestString(String responseField, String jmeterVariable, String requiredField) {
    	getTestStrings()[0].addProperty(new StringProperty(String.valueOf(responseField.hashCode()), responseField));
    	getTestStrings()[1].addProperty(new StringProperty(String.valueOf(jmeterVariable.hashCode()), jmeterVariable));
    	getTestStrings()[2].addProperty(new StringProperty(String.valueOf(requiredField.hashCode()), requiredField));
    }
    
    public void addxmlfileStrings(String xmlfileField) {
    	getxmlfileStrings().setObjectValue(String.valueOf(xmlfileField));
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
        String xmlcontent = "";
        String jmeterVariable = "";
        String responseVariable = "";
        String errmsg = "";
        Boolean ifound = false;
        String targetNodeValue = ""; // The target node value to check
        boolean contains = isContainsType(); // do it once outside loop
        boolean equals = isEqualsType();
        boolean ignorecase = isIgnoreType();
        String jmeterVariableValues = "";
        String sxpath = "";
        String ssourcedata = "";
        JSONArray jsonArray = null;
        JSONObject jsonObject = null;
        boolean bJsonObject = false;
        String originalJSON = response.getResponseDataAsString();
        ArrayList<String> jsonvalues;

        // Now determine whether the JSON data type are either JSONObject or JSONArray.
        JsonElement jsonElement = new JsonParser().parse(originalJSON);
        if(jsonElement.isJsonObject()) {
        	jsonObject = new JSONObject(originalJSON);
        	bJsonObject = true;
        } else {
        	jsonArray = new JSONArray(originalJSON);
        }
        
        // Get baseline and respond data.
        CollectionProperty[] data = getTestStrings();
		
		// Now get the list of target nodes.
		// This, need to find out the least node and then start iterate thru its children.
		for (int j=0; j<data[0].size(); j++){
   		  	  ifound = false;
   		  	  targetNodeValue = "";
   		  	  jmeterVariableValues = "";
   		  	  jmeterVariable = data[1].get(j).getStringValue();
   		  	  jmeterVariable = jmeterVariable.substring(0, jmeterVariable.indexOf(".")); // extract Jmeter variable which contains xml content.
	   		  xmlcontent = getThreadContext().getVariables().get(jmeterVariable); // get xml content.
	   		  sxpath = data[1].get(j).getStringValue().substring(data[1].get(j).getStringValue().indexOf(".")+1); // extract xpath info.

		  	  Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		  	            .parse(new InputSource(new StringReader(xmlcontent)));

    	  	  responseVariable = data[0].get(j).getStringValue();
    	  	  
    	  	  ArrayList<String> xmlvalues = JmeterUtils.getTargetData(doc, sxpath);
    	  	  
    	  	  if(!bJsonObject) {
        	  	  jsonvalues = JmeterUtils.getTargetData(jsonArray, responseVariable);
    	  	  } else {
        	  	  jsonvalues = JmeterUtils.getTargetData(jsonObject, responseVariable);
    	  	  }
    	  	  
			  // Now validate if we can find the matched data...
		  	  // reset ifound value to false...
 	    	  for (int l=0; l<jsonvalues.size(); l++){
 	    		  ifound = false;
	    		  targetNodeValue = JmeterUtils.convertString(jsonvalues.get(l),"UTF-8");
 	    		  for(int m=0; m<xmlvalues.size();m++){
 	 	    		ssourcedata = JmeterUtils.convertString(xmlvalues.get(m),"UTF-8");
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
	        			errmsg += "\r\nCan't find matched value between target and source... \r\n target json path: " + responseVariable + " target value: " 
	        					+ targetNodeValue + " source xml xpath: " + sxpath + " source values: " + jmeterVariableValues;
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


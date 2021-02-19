package com.sbux.jmeter.Sampler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.JMeterVariables;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRecordFieldIterator;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sbux.jmeter.util.JmeterUtils;

public class SAPDataActions extends AbstractScopedAssertion implements Serializable, Assertion{

    private static final long serialVersionUID = 241L;

    private static final String TEST_JMETER_VARIABLES = "assertion_jmeter_variables"; // $NON-NLS-1$ 

    private static final String TEST_PARAMETERS = "assertion_parameters"; // $NON-NLS-1$

	private final static String DESTINATION_NAME = "ABAP_AS_WITH_POOL";
	
	private JMeterVariables variables = new JMeterVariables();

	
    public SAPDataActions() {
        setProperty(new CollectionProperty(TEST_PARAMETERS, new ArrayList<String>()));
        setProperty(new CollectionProperty(TEST_JMETER_VARIABLES, new ArrayList<String>()));
    }

    public void clearTestStrings() {
    	CollectionProperty[] temp = getTestStrings();
    	for(CollectionProperty c : temp){
    		c.clear();
    	}
    }

    public CollectionProperty[] getTestStrings() {
    	CollectionProperty[] tempCol = new CollectionProperty[2];
    	tempCol[0] = (CollectionProperty) getProperty(TEST_PARAMETERS);
    	tempCol[1] = (CollectionProperty) getProperty(TEST_JMETER_VARIABLES);
        return tempCol;
    }

    public void addTestString(String parametersField) {
    	getTestStrings()[0].addProperty(new StringProperty(String.valueOf(parametersField.hashCode()), parametersField));
    }
	
	static {
	        Properties connectProperties = new Properties();
	
	        connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, "sap-cert.starbucks.net");
	        connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, "00");
	        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, "700");
	        connectProperties.setProperty(DestinationDataProvider.JCO_USER, "1710517");
	        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, "Jack'1234");
	        connectProperties.setProperty(DestinationDataProvider.JCO_LANG, "en");	
	        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, "3");
	        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, "10");
	}
	
	 
    static void createDestinationDataFile(String destinationName, Properties connectProperties) {
          File destCfg = new File(destinationName + ".jcoDestination");
          try {
                FileOutputStream fos = new FileOutputStream(destCfg, false);
                connectProperties.store(fos, "For testing Jmeter to SAP!");
                fos.close();
          } catch (Exception e) {
                throw new RuntimeException("Unable to create thedestination files", e);
          }
    }
    
	public void setFunctionName(String name) {
		setProperty(new StringProperty("FUNCTION_NAME",name));
	}
	
	public String getFunctionName() {
		return getProperty("FUNCTION_NAME").getStringValue();
	}
	
	public void setJmeterParameters(String name) {
		setProperty("JMETER_VARIABLES",name);
	}
	
	public String getJmeterParameters() {
		return getProperty("JMETER_VARIABLES").getStringValue();
	}
	
	public void setSAPHost(String name) {
		setProperty(new StringProperty("SAP_HOST",name));
	}
	
	public String getSAPHost() {
		return getProperty("SAP_HOST").getStringValue();
	}
	
	public void setSAPLogon(String name) {
		setProperty(new StringProperty("SAP_LOGON",name));
	}
	
	public String getSAPLogon() {
		return getProperty("SAP_LOGON").getStringValue();
	}
	
	public void setSAPPassword(String name) {
		setProperty(new StringProperty("SAP_PWD",name));
	}
	
	public String getSAPPassword() {
		return getProperty("SAP_PWD").getStringValue();
	}

		/**
	    * Clean up existing testing variables.
	    *
	    * @param String[]
	    * @return 
	    */
		private void cleanJmeterVariables(String[] fieldnames){
	        JMeterVariables temp = getThreadContext().getVariables();
	        String st = "";
	        for (int i = 0; i < fieldnames.length; i++) {
	        		st = temp.get(fieldnames[i]+"_#");
	        		if(st != null) {
		        		for(int j=1; j<=Integer.parseInt(st); j++) {
		        			temp.remove(fieldnames[i]+"_"+String.valueOf(j));
		        		}
			            temp.remove(fieldnames[i]+"_#");
	        		}
			    }
          getThreadContext().setVariables(temp);
		}

		/**
	    * Save variables to Jmeter global parameters.
	    *
	    * @param JCoTable, String delimiter, String[] fieldnames
	    * @return 
	    * @throws ParserConfigurationException, TransformerException 
	    */
		private void saveToJmeterParameters(JCoTable act, String delimiter, String[] fieldnames) throws ParserConfigurationException, TransformerException {
	        variables = getThreadContext().getVariables();
	        cleanJmeterVariables(fieldnames);
	        for (int i = 0; i < act.getNumRows(); i++) {
	        	act.setRow(i);
		        Iterator<JCoField> iter = act.iterator();
			    while (iter.hasNext()) {
			        JCoField field = iter.next();
			        String temp = field.getString();
			        String[] values;
			        
			        // Deal with some special characters. 
			        if(delimiter.equals("|") || delimiter.equals("?") || delimiter.equals("*") || delimiter.equals("+") || delimiter.equals("\\") || delimiter.equals(".") || delimiter.equals("^") || delimiter.equals("$") || delimiter.equals("&")) {
		        		values = temp.split("\\"+delimiter);
			        }
			        else {
		        		values = temp.split(delimiter);
			        }
			        
			        for(int j=0; j<fieldnames.length; j++) {
			            variables.put(fieldnames[j]+"_"+String.valueOf(i+1), (j>=fieldnames.length)?"":values[j]);
			            variables.put(fieldnames[j]+"_#", String.format("%d", act.getNumRows()));
			        }
			    }
	        }
            getThreadContext().setVariables(variables);
		}
		 
		/**
	    * Call SAP RFC function.
	    *
	    * @param 		String funcationName
	    * 				HashMap<String,String> params
	    * @return 
	    * @throws JCoException 
	    */
		public void executeSAPFunction(String functionName, Map<String,List<String>> imports, Map<String,List<String>> tables) throws Exception {
          JCoDestination destination = JCoDestinationManager.getDestination(DESTINATION_NAME);
          JCoFunction function = destination.getRepository().getFunction(functionName);
          JCoTable data;
          Date targetdate;
          
          // Process input parameters.
          Iterator<String> ikeys = imports.keySet().iterator();
          List<String> imapValues = new ArrayList<String>();
    	  while(ikeys.hasNext()) {
    		  String ikName = ikeys.next();
    		  imapValues = imports.get(ikName);
    		  
    		  for (String s : imapValues) {
    			  targetdate = JmeterUtils.getValidDate(s,"MM/dd/yyyy");
    			  if(targetdate!=null) {
                      function.getImportParameterList().setValue(ikName,targetdate);
    			  }
    			  else {
                      function.getImportParameterList().setValue(ikName,s);
    			  }
    		  }
    	  }

          // Process Tables parameters.
          Iterator<String> itkeys = tables.keySet().iterator();
          List<String> itmapValues = new ArrayList<String>();
          JCoTable t; 
		  Pattern p = Pattern.compile("[^\\[\\]\\s]+");
    	  while(itkeys.hasNext()) {
    		  String itkName = itkeys.next();
    		  t = function.getTableParameterList().getTable(itkName);
    		  itmapValues = tables.get(itkName);
    		  Matcher m;
    		  
    		  for (int i=0; i<itmapValues.size(); i++) {
    			  t.appendRow();
				  m = p.matcher(itmapValues.get(i));
				  if(m.find()) {
					  String[] values = m.group().split(",");
					  for (int j=0; j<values.length; j++) {
						  JCoRecordFieldIterator jfi = t.getRecordFieldIterator();
		    			  while(jfi.hasNextField()) {
		    				  JCoField f = jfi.nextField();
		    				  if(f.getName().toLowerCase().equals(values[j].split("=")[0].toLowerCase())) {
		        				  t.setValue(f.getName(), values[j].split("=")[1]);
		    				  }
		    			  }
					  }
				  }
    		  }
    	  }

          if (function == null)
                throw new RuntimeException(functionName + " not found in SAP.");

	        // Now execute the function call.
	        try {
	            function.execute(destination);
	        } catch (Exception e) {
	              //System.out.println(e.toString());
	              return;
	        }

            //TimeUnit.SECONDS.sleep(1);
	        if(function.getTableParameterList()!=null) {
		        data = function.getTableParameterList().getTable(0);
                saveToJmeterParameters(data,"|",getJmeterParameters().split(","));
	        }
}

	  
	 /**
     * Get SAP data from read table and set to Jmeter variables.
     *
     * @param 
     * @return 
     * @throws JCoException 
	 * @throws InterruptedException 
     */
	  private void getSAPTableData(String functionName, Map<String,List<String>> imports, Map<String,List<String>> tables) throws JCoException, InterruptedException {
          JCoDestination destination = JCoDestinationManager.getDestination(DESTINATION_NAME);
          JCoFunction function = destination.getRepository().getFunction(functionName);
          JCoTable data;
          String delimiter = "";
		  Pattern p = Pattern.compile("\\$\\{(.*?)}");
		  Matcher m;
          
          // Process input parameters.
          Iterator<String> ikeys = imports.keySet().iterator();
          List<String> imapValues = new ArrayList<String>();
    	  while(ikeys.hasNext()) {
    		  String ikName = ikeys.next();
    		  imapValues = imports.get(ikName);
    		  
    		  // Need to remember the delimiter value.
    		  if(ikName.toLowerCase().equals("delimiter")) {
    			  delimiter = imapValues.get(0);
    		  }
    		  
    		  for (String s : imapValues) {
                  function.getImportParameterList().setValue(ikName,s);
    		  }
    	  }

          // Process Tables parameters.
          Iterator<String> itkeys = tables.keySet().iterator();
          List<String> itmapValues = new ArrayList<String>();
    	  while(itkeys.hasNext()) {
    		  String itkName = itkeys.next();
    		  itmapValues = tables.get(itkName);
    		  
              JCoTable t = function.getTableParameterList().getTable(itkName);
    		  for (String s : itmapValues) {
    			  if(itkName.toLowerCase().equals("options")) {
     				  m = p.matcher(s);
     				  if(!m.find()) { // Not found matched regular expression...
     					  String rows[] = s.split("\\|"); // Using "|" to split out the columns and values.
      					  for(int iloops=0; iloops<rows.length; iloops++) {
     						  	t.appendRow();
           	  	        		t.setValue("TEXT", rows[iloops]);
     	  	        		}
     				  }
     				  else {
         				  while(m.find()) {
         	    			t.appendRow();
       				        String tokenKey = m.group(1);
       				        Integer it = getThreadContext().getThreadNum()+1;
       				        String replacementValue = getThreadContext().getVariables().get(tokenKey+"_"+String.valueOf(it));
       				        s = s.replaceAll("\\$\\{(.*?)}", replacementValue);
       	        			t.setValue("TEXT", s);
         				  }
     				  }
    			  }
    			  else if(itkName.toLowerCase().equals("fields")) {
   	    			t.appendRow();
        			t.setValue("FIELDNAME", s);
    			  }
    		  }
    		  
              TimeUnit.SECONDS.sleep(1);
    		  function.getTableParameterList().setValue(itkName, t);
              TimeUnit.SECONDS.sleep(1);
    	  }

          if (function == null)
                throw new RuntimeException(functionName + " not found in SAP.");

          try {
                function.execute(destination);
                data = function.getTableParameterList().getTable("DATA");
                //printXMLToOutput(data);
                saveToJmeterParameters(data,delimiter,getJmeterParameters().split(","));
          } catch (Exception e) {
                System.out.println(e.toString());
                return;
          }
  	 }	

	@Override
	public AssertionResult getResult(SampleResult response) {
        AssertionResult result = null;
		try {
			result = evaluateResponse(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
	        StringWriter sw = new StringWriter();
	        e.printStackTrace(new PrintWriter(sw));
	        result.setFailureMessage(sw.toString());
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
        AssertionResult result = new AssertionResult(getName());
        String pVariable = "";
        String errmsg = "";
        Map<String,List<String>> inputParams = new HashMap<String,List<String>>();
        Map<String,List<String>> tableParams = new HashMap<String,List<String>>();
        String param[] = null;
        
        CollectionProperty[] data = getTestStrings();
        String values[] = null;
        String icurrentKey = "";
        String tcurrentKey = "";
        
        // First to check if the credential is valid.
        if(getSAPHost().equals("SAP_HOST_NAME") || getSAPHost().equals("")) {
        	errmsg += "SAP Host Name " + getSAPHost() + " is invalid.\n";
        }
        if(getSAPLogon().equals("SAP_LOGON_NAME") || getSAPLogon().equals("")) {
        	errmsg += "SAP Logon Name " + getSAPLogon() + " is invalid.\n";
        }
        if(getSAPPassword().equals("SAP_PASSWORD") || getSAPPassword().equals("")) {
        	errmsg += "SAP Logon Password " + getSAPPassword() + " is invalid.\n";
        }
        
        Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, getSAPHost());
        connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, "00");
        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, "700");
        connectProperties.setProperty(DestinationDataProvider.JCO_USER, getSAPLogon());
        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, getSAPPassword());
        connectProperties.setProperty(DestinationDataProvider.JCO_LANG, "en");	
        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, "3");
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, "10");
        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, "3");
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, "10");
        createDestinationDataFile(DESTINATION_NAME, connectProperties);
        List<String> tlist = new ArrayList<String>();;
        
        if(errmsg=="") {
            for (int i=0; i<data[0].size(); i++){
          	  pVariable = data[0].get(i).getStringValue().trim();
      		  param = pVariable.split(";");
      		  for(int j=0; j<param.length; j++) {
      			  
      			  // Filter out the typed of parameters.
      			  // Need to include the possible file path string which has drive letter followed by ":"
      			  values = param[j].split(":");
      				  if(values[0].toLowerCase().contains("import")) {
      					String stvalue = (values.length>3) ? values[2]+":"+values[3] : values[2];
      			        List<String> ilist = new ArrayList<String>();
      					  if(j==param.length-1) {
      						  ilist.add(stvalue);
      						  inputParams.put(values[1], ilist);
      						  icurrentKey = values[1];
      					  }
      					  else if(icurrentKey!="" && !values[1].toLowerCase().equals(icurrentKey)) {
      						  inputParams.put(values[1], ilist);
      						  icurrentKey = values[1];
      					  }
      					  else {
      						  ilist.add(stvalue);
      					  }
      				  }
      				  else if (values[0].toLowerCase().contains("table")) {
    					  if(j==param.length-1) {
      						  tlist.add(values[2]);
      						  tableParams.put(values[1], tlist);
      						  tcurrentKey = "";
      						  tlist = new ArrayList<String>();
      					  }
      					  else if(tcurrentKey!="" && !values[1].toLowerCase().equals(tcurrentKey)) {
      						  tlist.add(values[2]);
      						  tableParams.put(values[1], tlist);
      						  tcurrentKey = values[1];
      						  tlist = new ArrayList<String>();
      					  }
      					  else {
      						tlist.add(values[2]);
      					  }
      				  }
/*      			  else if(values.length==2) {
      				  fParams.put(values[0], values[1]);
      			  }
*/      		  }
          }
          
          try {
            // Now call SAP function.
            if(getFunctionName().toLowerCase().equals("rfc_read_table")) {
            	getSAPTableData(getFunctionName(),inputParams,tableParams);
            }
            else {
            	executeSAPFunction(getFunctionName(),inputParams,tableParams);
            }
          }
          catch(Exception ex) {
        	  errmsg = ex.toString();
          }
        }
	  	  
		  // Something wrong before get into the comparing logic...
	  	  // In the case if service skips returning node because no value, we need to handle this 
	  	  // situation intelligently...
	      if(errmsg != ""){
		     result.setFailure(true);
	         result.setError(true);
	         result.setFailureMessage(errmsg);
	      }
	      else {
	  		result.setFailure(false);
	        result.setError(false);
	      }
        return result;
    }
}

package com.sbux.jmeter.util;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.jmeter.util.JMeterUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.jayway.jsonpath.JsonPath;

public final class JmeterUtils extends JMeterUtils{
    
	private static SimpleDateFormat dateFormat;
	private static Date sourcedate;
	private static Date targetdate;
   /**
     * Convert String to certain encoding of String.
     *
     * @param s source
     * @param f encoding
     * @throws UnsupportedEncodingException 
     */
    public static String convertString(String s, String encoding) throws UnsupportedEncodingException{
		if(null != s){
	    	byte[] ptext = s.getBytes("ISO-8859-1"); 
			return (new String(ptext, encoding)); 
		}
		return null;
    }
    
    /**
     * Compare source and target strings with certain way.
     *
     * @param s source
     * @param t target
     * @param contains whether t is part of s
     * @param equals whether s is equal to t
     * @param ignorecase whether s is equal to t in case insensitive 
     * @return boolean whether comparison is met or not
     * @throws ParseException 
     * @throws Exception 
     */
    public static boolean compare(String s, String t, boolean contains, boolean equals, boolean ignorecase) throws ParseException{
    	// One of comparer is null and so comparison is mismatched.
    	if(null==s || null==t){
    		return false;
    	}
    	
    	if(contains){
    		return s.contains(t);
    	}
    	else if(equals){
    		// Now let's try compare source and target values...
    		if((sourcedate=getValidDate(s))!=null && (targetdate=getValidDate(t))!=null) {
        		return sourcedate.equals(targetdate);
    		}
    		else if(isFloat(s) && isFloat(t)) {
        		if(Float.compare(Float.parseFloat(s), Float.parseFloat(t))==0){
        			return true;
        		}
        		else {
        			return false;
        		}
    		}
    		else if(isInteger(s) && isInteger(t)) {
        		if(Integer.compare(Integer.parseInt(s), Integer.parseInt(t))==0){
        			return true;
        		}
        		else {
        			return false;
        		}
    		}
    		else {
        		return s.equals(t);
    		}
    	}
    	else{
    		return s.toLowerCase().equals(t.toLowerCase());
    	}
    }

    
   	/**
   	 * 
   	 * @param String Integer
   	 * @return Boolean indicating whether it's a valid Integer string.
   	 */
   	public static boolean isInteger(String s){
   		   boolean flag = true;

   		   try{
   		      Integer.parseInt(s); 
   		      return flag;
   		   }catch(Exception e){
   		      flag = false;
   		   }
   		 return flag;
   	}
    
   	/**
   	 * 
   	 * @param String float
   	 * @return Boolean indicating whether it's a valid float or double string.
   	 */
   	public static boolean isFloat(String s){
   		   boolean flag = true;

   		   try{
   		      Float.parseFloat(s); 
   		      return flag;
   		   }catch(Exception e){
   		      flag = false;
   		   }
   		 return flag;
   	}

   	/**
   	 * 
   	 * @param String date
   	 * @return Date indicating whether it's a valid date string.
   	 */
   	public static Date getValidDate(String date){
 
   		   Date tempdate;
   		   dateFormat = new SimpleDateFormat("MM/dd/yyyy");
   		   try{
   			   tempdate = dateFormat.parse(date); 
   			   return tempdate;
   		   }catch(ParseException e){
   			   tempdate = null;
   		   }

   		   dateFormat = new SimpleDateFormat("yyyy-MM-dd");
   		   try{
   			   tempdate = dateFormat.parse(date); 
   			   return tempdate;
   		   }catch(ParseException e){
   			   tempdate = null;
   		   }

   		   dateFormat = new SimpleDateFormat("yyyy/MM/dd");
   		   try{
   			   tempdate = dateFormat.parse(date); 
   			   return tempdate;
   		   }catch(ParseException e){
   			   tempdate = null;
   		   }

   		   dateFormat = new SimpleDateFormat("yyyyMMdd");
   		   try{
   			   tempdate = dateFormat.parse(date); 
   			   return tempdate;
   		   }catch(ParseException e){
   			   tempdate = null;
   		   }

   		   dateFormat = new SimpleDateFormat("dd-MM-yyyy");
   		   try{
   			   tempdate = dateFormat.parse(date); 
   			   return tempdate;
   		   }catch(ParseException e){
   			   tempdate = null;
   		   }
   		 return tempdate;
   	}

   	/**
   	 * @param String date
   	 * @param String date format
   	 * @return Boolean whether it's a valid date string.
   	 */
   	public static Date getValidDate(String date, String dateformat){
 
   		   SimpleDateFormat tempdate;
   		   Date datetransformed;
   		   tempdate = new SimpleDateFormat(dateformat);
   		   try{
   			   datetransformed = tempdate.parse(date); 
   			   return datetransformed;
   		   }catch(ParseException e){
   			   return null;
   		   }
   	}

   	/**
   	 * @param String datetime
   	 * @param String date format
   	 * @return Boolean whether it's a valid date string.
   	 */
   	public static long getValidDateTime(String datetime, String dateformat){
 
   		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);
   		try{
   	        long positionCreationTime = Instant.from(dateTimeFormatter.parse(datetime)).toEpochMilli();
   			   return positionCreationTime;
   		}catch(Exception e){
   			   return 0;
   		}
   	}
   		
   	/**
   	 * 
   	 * @param String target XML element
   	 * @param String target XML xpath
   	 * 
   	 * @return String target XML nodes' values.
   	 */
   	public static ArrayList<String> getTargetData(Document doc, String path){
   		NodeList targetNodes = null;
		ArrayList<String> stargetvalues = new ArrayList<String>();
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
   		   try{
   			XPathExpression expr = xpath.compile(path);
   			targetNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODE);
   			
   			for(int i=0; i<targetNodes.getLength(); i++) {
   				stargetvalues.add(targetNodes.item(i).getTextContent());
   			}
   			
   		   }catch(Exception ex){
   			   stargetvalues.add(ex.toString());
   		   }
   		   
   		   if(stargetvalues.size()==0) {
   			   stargetvalues.add(null);
   		   }
   		return stargetvalues;
   	}

   	/**
   	 * 
   	 * @param JSONObject target element
   	 * @param String target path
   	 * If path string contains ? meant to get the latest child node's info.
   	 * If path string contains * meant to get all of sub-nodes' info.
   	 * @return Collection of target String data.
   	 */
   	public static ArrayList<String> getTargetData(JSONObject o, String path){
		ArrayList<String> stargetnodesvalue = new ArrayList<String>();
		String sjsonObject = "";
		String text="";
		String[] listQueries = path.split(";"); // This is for concatenate multiple query results. 
		ArrayList<Object> nodes = new ArrayList();
		net.minidev.json.JSONArray list;
		int iCount=0;
		for(String query : listQueries) {
			if(!query.contains("$")) {
				text += query;
				iCount++;
			}
			else {
		        try {
			   		sjsonObject = o.toString();
			   		if(query.contains("[?]")) { 
			   			String tempPath = query.substring(0, query.indexOf("[?]"));
			   			list = JsonPath.read(sjsonObject,tempPath);
			   			tempPath = tempPath+"["+String.valueOf(list.size()-1)+"]."+query.substring(query.indexOf("[?]")+4);
			   			nodes = JsonPath.read(list, tempPath);
			   			if (nodes.size()>0) {
					   		stargetnodesvalue.add(text+nodes.get(nodes.size()-1));
			   			}
			   		}else if(query.contains("[*]")) {
			   			String tempPath = query.substring(0, query.indexOf("[*]"));
			   			list = JsonPath.read(sjsonObject,tempPath);
			   			tempPath = "$.."+query.substring(query.indexOf("[*]")+4);
			   			nodes = JsonPath.read(list, tempPath);
			        	for(int i=0; i<nodes.size(); i++) {
					   		stargetnodesvalue.add(text+nodes.get(i));
			        	}
			   		}else if(query.contains("[??]")) { // This is for conditional JSONArray logic to get corresponding target field's value.
			   			String tempPath = query.substring(0, query.indexOf("[??]"));
			   			list = JsonPath.read(sjsonObject,tempPath);
			   			tempPath = "$.."+query.substring(query.indexOf("[??]")+4, query.indexOf("="));
			   			nodes = JsonPath.read(list, tempPath);
			   			for(int i=0; i<nodes.size(); i++) {
			   				if(tempPath.contains("effectiveDate")) {
			   					// Need to have logic to find targeting date record.
			   					// Currently just look up current date record...
			   					if(query.substring(query.indexOf("=")+1, query.lastIndexOf(",")).toLowerCase().equals("current")) {
			   						Date today = new Date();
			   						if(today.after(getValidDate(nodes.get(i).toString(), "yyyy-MM-dd"))) {
					   					for(int ip=0; ip<stargetnodesvalue.size(); ip++) {
					   						stargetnodesvalue.remove(ip);
					   					}
					   					ArrayList<Object> alist = JsonPath.read(list, "$"+query.substring(query.lastIndexOf(",")+1));
					   					if(alist.size()==nodes.size()) {
						   					stargetnodesvalue.add(text+alist.get(i));
					   					}
					   					else if(alist.size()>0 && alist.size()==nodes.size()*2) { // Heck, this is to deal with multiple matched field name values...
									   		stargetnodesvalue.add(text+alist.get(i*2));
					   					}
					   					else {
					   						stargetnodesvalue.add(text+null);
					   					}
			   						}
			   					}
			   				} 
			   				else if(nodes.get(i).toString().equals(query.substring(query.indexOf("=")+1, query.lastIndexOf(".")))) {
			   					nodes = JsonPath.read(list, "$.."+query.substring(query.lastIndexOf(".")+1));
						   		stargetnodesvalue.add(text+nodes.get(i).toString());
					        	break;
			   				}
			   			}
			   		}
			   		else if(query.contains("[???]")){ // Hack, this is to deal with jobs logic for FIM generated JSON file...

					}
			   		else {
			   			if(query.contains("[d]")) {
			   				String tempPath = query.substring(0, query.indexOf("[d]"));
					   		list = JsonPath.read(sjsonObject,tempPath);
			   			}
			   			else {
					   		list = JsonPath.read(sjsonObject,query);
			   			}
			   			
				        for(int i=0; i<list.size(); i++) {
			   				if(query.contains("[d]")) {
			   					if(iCount==listQueries.length-1) {
						   			stargetnodesvalue.add(text+stripDiacritics(list.get(i).toString()));
			   					}
			   					else {
			   						text += stripDiacritics(list.get(i).toString());
			   					}
			   				}
			   				else {
			   					if(iCount==listQueries.length-1) {
						   			stargetnodesvalue.add(text+list.get(i).toString());
			   					}
			   					else {
			   						text += list.get(i).toString();
			   					}
			   				}
				        }
			   		}
		   		}catch(Exception e){
		   			String value = JsonPath.read(sjsonObject,query);
		   			stargetnodesvalue.add(value);
		   		}
				
			}
		}
		return stargetnodesvalue;
   	}

   	/**
   	 * 
   	 * @param JSONArray target element
   	 * @param String target path
   	 * If path string contains ? meant to get the latest child node's info.
   	 * If path string contains * meant to get all of sub-nodes' info.
   	 * @return Collection of target String data.
   	 */
   	public static ArrayList<String> getTargetData(JSONArray o, String path){
		ArrayList<String> stargetnodesvalue = new ArrayList<String>();
		String text="";
		String[] listQueries = path.split(";"); // This is for concatenate multiple query results. 
		ArrayList<Object> nodes = new ArrayList();
		net.minidev.json.JSONArray list;
		String sjsonArray = "";
		int iCount=0;
		for(String query : listQueries) {
			if(!query.contains("$")) {
				text += query;
				iCount++;
			}
			else {
		        try {
		        	sjsonArray = o.toString();
			   		if(query.contains("[?]")) { 
			   			String tempPath = query.substring(0, query.indexOf("[?]"));
			   			list = JsonPath.read(sjsonArray,tempPath);
			   			tempPath = tempPath+"["+String.valueOf(list.size()-1)+"]."+query.substring(query.indexOf("[?]")+4);
			   			nodes = JsonPath.read(list, tempPath);
			   			if (nodes.size()>0) {
			        		if(null == nodes.get(nodes.size()-1)) {
			        			stargetnodesvalue.add(text+null);
			        		}
			        		else {
						   		stargetnodesvalue.add(text+nodes.get(nodes.size()-1));
			        		}
			   			}
			   		}else if(query.contains("[*]")) {
			   			String tempPath = query.substring(0, query.indexOf("[*]"));
			   			list = JsonPath.read(sjsonArray,tempPath);
			   			tempPath = "$.."+query.substring(query.indexOf("[*]")+4);
			   			nodes = JsonPath.read(list, tempPath);
			        	for(int i=0; i<nodes.size(); i++) {
			        		if(null == nodes.get(i)) {
			        			stargetnodesvalue.add(text+null);
			        		}
			        		else {
						   		stargetnodesvalue.add(text+nodes.get(i));
			        		}
			        	}
			   		}else if(query.contains("[??]")) { // This is for conditional JSONArray logic to get corresponding target field's value.
			   			String tempPath = query.substring(0, query.indexOf("[??]"));
			   			list = JsonPath.read(sjsonArray,tempPath);
			   			tempPath = "$.."+query.substring(query.indexOf("[??]")+4, query.indexOf("="));
			   			nodes = JsonPath.read(list, tempPath);
			   			for(int i=0; i<nodes.size(); i++) {
			   				if(tempPath.contains("effectiveDate")) {
			   					// Need to have logic to find targeting date record.
			   					// Currently just look up current date record...
			   					if(query.substring(query.indexOf("=")+1, query.lastIndexOf(",")).toLowerCase().equals("current")) {
			   						Date today = new Date();
			   						if(today.after(getValidDate(nodes.get(i).toString(), "yyyy-MM-dd"))) {
					   					for(int ip=0; ip<stargetnodesvalue.size(); ip++) {
					   						stargetnodesvalue.remove(ip);
					   					}
					   					ArrayList<Object> alist = JsonPath.read(list, "$"+query.substring(query.lastIndexOf(",")+1));
					   					if(alist.size()==nodes.size()) {
						   					stargetnodesvalue.add(text+alist.get(i));
					   					}
					   					else if(alist.size()>0 && alist.size()==nodes.size()*2) { // Heck, this is to deal with multiple matched field name values...
									   		stargetnodesvalue.add(text+alist.get(i*2));
					   					}
					   					else {
					   						stargetnodesvalue.add(text+null);
					   					}
			   						}
			   					}
			   				} 
			   				else if(null != nodes.get(i) && nodes.get(i).toString().equals(query.substring(query.indexOf("=")+1, query.lastIndexOf(".")))) {
			   					nodes = JsonPath.read(list, "$.."+query.substring(query.lastIndexOf(".")+1));
						   		stargetnodesvalue.add(text+nodes.get(i));
					        	break;
			   				}
			   			}
			   		}
			   		else {
			   			if(query.contains("[d]")) {
			   				String tempPath = query.substring(0, query.indexOf("[d]"));
					   		list = JsonPath.read(sjsonArray,tempPath);
			   			}
			   			else {
					   		list = JsonPath.read(sjsonArray,query);
			   			}
			   			
				        for(int i=0; i<list.size(); i++) {
			   				if(query.contains("[d]")) {
			   					if(iCount==listQueries.length-1) {
						   			stargetnodesvalue.add(text+stripDiacritics(list.get(i).toString()));
			   					}
			   					else {
			   						text += stripDiacritics(list.get(i).toString());
			   					}
			   				}
			   				else {
			   					if(iCount==listQueries.length-1) {
			   						if(null == list.get(i)) {
			   							stargetnodesvalue.add(text+null);
			   						}
			   						else {
							   			stargetnodesvalue.add(text+list.get(i).toString());
			   						}
			   					}
			   					else {
			   						if(null == list.get(i)) {
			   							text += null;
			   						}
			   						else {
				   						text += list.get(i).toString();
			   						}
			   					}
			   				}
				        }
			   		}
		   		}catch(Exception e){
		   			String value = JsonPath.read(sjsonArray,query);
		   			stargetnodesvalue.add(value);
		   		}
			}
		}
        return stargetnodesvalue;
   	}

    /**
     * Function to remove diacritics
     * @param text
     * @return
     */
    public static String stripDiacritics(String text){
        return text == null ? null :
                Normalizer.normalize(replaceUmlaut(text), Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Replacing German Characters Umlauts
     * @param input
     * @return
     */
    private static String replaceUmlaut(String input) {
        //replace all lower Umlauts
        String o_strResult =
                input
                        .replaceAll("ü", "ue")
                        .replaceAll("ö", "oe")
                        .replaceAll("ä", "ae")
                        .replaceAll("ß", "ss");

        //first replace all capital umlaute in a non-capitalized context (e.g. Übung)
        o_strResult =
                o_strResult
                        .replaceAll("Ü(?=[a-zäöüß ])", "Ue")
                        .replaceAll("Ö(?=[a-zäöüß ])", "Oe")
                        .replaceAll("Ä(?=[a-zäöüß ])", "Ae");

        //now replace all the other capital umlaute
        o_strResult =
                o_strResult
                        .replaceAll("Ü", "UE")
                        .replaceAll("Ö", "OE")
                        .replaceAll("Ä", "AE");

        return o_strResult;
    }

    /**
     * Method to determin if string has diacritics
     * @param text
     * @return boolen
     */
    public static boolean hasDiacritics(String s) {
        // Decompose any á into a and combining-'.
        String s2 = Normalizer.normalize(s, Normalizer.Form.NFD);
        return s2.matches("(?s).*\\p{InCombiningDiacriticalMarks}.*");
    }

    /**
     * Method to compare 2 objects
     * @param Oject, Object
     * @return boolen
     */
    public static boolean equals(Object tempTargetData, Object tempSourceData) {
    	if((null != tempTargetData && null != tempSourceData && !tempTargetData.equals(tempSourceData)) 
    			|| (null != tempTargetData && null == tempSourceData)
    			|| (null == tempTargetData && null != tempSourceData)) {
        	return false;
    	}
    	return true;
    }
}

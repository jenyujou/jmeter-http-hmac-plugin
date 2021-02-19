package com.sbux.jmeter.protocol.java.sampler;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.FileWriter;
import java.io.Serializable;
import java.net.URI;

/**
 * Sampler element to retrieve blob file.
 *
 * @author jjou
 *
 */
public class BlobStorage extends AbstractJavaSamplerClient implements Serializable {
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("container", "");
        defaultParameters.addArgument("account_name", "");
        defaultParameters.addArgument("account_key", "");
        defaultParameters.addArgument("file_name", "");
        return defaultParameters;
    }

    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {

        String containerName = javaSamplerContext.getParameter("container").trim();
        String accountName = javaSamplerContext.getParameter("account_name").trim();
        String accountKey = javaSamplerContext.getParameter("account_key").trim();
        String fileName = javaSamplerContext.getParameter("file_name").trim();
        JMeterVariables variables = JMeterContextService.getContext().getVariables();
        
        CloudStorageAccount storageAccount;
        CloudBlobClient blobClient;
        CloudBlobContainer container;

        SampleResult result = new SampleResult();
        result.sampleStart(); // start stopwatch
        try {
            storageAccount = CloudStorageAccount.parse("DefaultEndpointsProtocol=https;" +
                    "AccountName="+accountName+";" +
                    "AccountKey="+accountKey
            );
            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
            if(fileName.contains("${")) {
            	fileName = variables.get(fileName.substring(2,fileName.lastIndexOf("}")-2));
            }
            CloudBlockBlob tempBlob = (fileName.isEmpty() || fileName==null) ? null : container.getBlockBlobReference(fileName);
            
            // Now iterate thru all blobs and find the most recent blob file, if user didn't specify the blob filename.
            if(tempBlob == null) {
                for(ListBlobItem blobItem : container.listBlobs()){
                    URI uri = blobItem.getUri();
                    String blobPath = uri.getPath().substring(1 + containerName.length() + 1);
                    long fileTimestamp = Long.valueOf(blobPath.substring(0, blobPath.indexOf("__")));
                    //System.out.println(blobPath);
                    CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(blobPath);
                    if(tempBlob==null || fileTimestamp > Long.valueOf(tempBlob.getName().substring(0, blobPath.indexOf("__")))){
                        tempBlob = cloudBlockBlob;
                    }
                }
            }

            String blobContents = tempBlob.downloadText("UTF-16",null,null,null);
            FileWriter fileWriter = new FileWriter(tempBlob.getName());
            fileWriter.write(blobContents);
            fileWriter.close();
	        
            // For now, add blob filename and content to global variables.
            variables.put("blob", tempBlob.getName());
            variables.put("blobContent", blobContents);
            
            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseData(blobContents, "UTF-16");
            result.setResponseMessageOK();
        } catch (Exception e) {
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Exception:" + e);

            java.io.StringWriter stringWriter = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString(), "UTF-16");
            result.setDataType(org.apache.jmeter.samplers.SampleResult.TEXT);
            result.setResponseCode("500");
        }
        return result;
    }
}

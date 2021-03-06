package com.supernet.api.instantdex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.apache.http.conn.ConnectTimeoutException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.google.common.base.Preconditions;
import com.supernet.api.BaseTestClass;
import com.supernet.api.client.instantdex.Bids;
import com.supernet.api.client.instantdex.orderBookBean;
import com.supernet.api.utility.HTTPUtil;
import com.supernet.api.utility.HelperUtil;
import com.supernet.api.utility.ReadExcel;
import com.supernet.api.utility.ReporterUtil;

public class OrderBookTest extends BaseTestClass {

	public ReporterUtil reporter;
	private static final Logger logger = LoggerFactory.getLogger(OrderBookTest.class);

	String className = this.getClass().getName();
	private String appURL;

	String uniqueValue = HelperUtil.getUniqueValue();
	
	SoftAssert softAssert=new SoftAssert();

	/**
	 * Setting up the precondition
	 */

	@BeforeClass
	public void setPreconditions() {
		logger.info("In the class ---", className);
		reporter = getReporter(" Instadex API - Orderbook method ", "Tests OrderBook method of Instadex API ");
	}

	@DataProvider(name = "orderBookTestData")
	public static Object[][] testData() throws Exception {
		logger.info("In the method ");
		ReadExcel re = new ReadExcel(
				System.getProperty("user.dir") + BaseTestClass.globalConfig.getString("DATASHEET_CONFIG"),
				"sheetOrderbookTest");
		return re.getTableToHashMapDoubleArray();
	}

	/**
	 * This method is used to test Response code of api
	 * 
	 * @param hm
	 */
	@Test(priority = 1, dataProvider = "orderBookTestData")
	public void testResponseCode(HashMap<String, String> hm) throws ConnectTimeoutException {

		// Checking execution flag
		if (hm.get("ExecuteFlag").trim().equalsIgnoreCase("No"))
			throw new SkipException("Skipping the test -->> As per excel entry");

		appURL = globalConfig.getString("ORDERBOOK_API_CONFIG") + "?" + "exchange=" + hm.get("I_Exchange") + "&"
				+ "base=" + hm.get("I_Base") + "&" + "rel=" + hm.get("I_Rel") + "&" + "allfields="
				+ hm.get("I_Allfields") + "&" + "ignore=" + hm.get("I_Ignore");

		int responseCode = 0;
		
		logger.debug("request URI is  sent as --" + appURL.toString());
		reporter.writeLog("Request" , "Is being sent as", appURL.toString());

		try {
			
			responseCode = HTTPUtil.sendGet(appURL);

			System.out.println("Response Code is :" + responseCode);

			if (responseCode == 200) {
				
				try {

					String filePathOfJsonResponse = HelperUtil.createOutputDir(this.getClass().getSimpleName())
							+ File.separator + this.getClass().getSimpleName() + hm.get("SerialNo") + ".json";

					HTTPUtil.writeResponseToFile(filePathOfJsonResponse);

				} catch (UnsupportedOperationException e) {
					e.printStackTrace();
					reporter.writeLog("FAIL", "", "Caught Exception :" + e.getMessage());
					Assert.fail("Caught Exception ...");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			reporter.writeLog("FAIL", "", "Caught Exception :" + e.getMessage());
			Assert.fail("Caught Exception ..." + e.getMessage());
		}

		logger.debug("The response code is " + responseCode);
		Preconditions.checkArgument(!hm.get("httpstatus").equals(null), "String httpstatus in excel must not be null");

		// Verify response code
		
		System.out.println("A"+Integer.parseInt(hm.get("httpstatus")));
		System.out.println("B is"+responseCode);

		try {
			if (responseCode == Integer.parseInt(hm.get("httpstatus"))) {
				reporter.writeLog("PASS", "Status should be " + (hm.get("httpstatus")), "Status is " + responseCode);
				System.out.println("Yes its here");
				Assert.assertEquals(responseCode, (Integer.parseInt(hm.get("httpstatus"))));
			} else {
				reporter.writeLog("FAIL", "Status should be " + (hm.get("httpstatus")), "Status is " + responseCode);
				Assert.assertEquals(responseCode, (Integer.parseInt(hm.get("httpstatus"))));
			}
		} catch (Exception e) {
		}

		System.out.println("-------------------------------------------");
	}
	

	/**
	 * This method is used to test content of response
	 * 
	 * @param hm
	 */
	@Test(priority = 2, dataProvider = "orderBookTestData")
	public void testContentJSONFileResponses(HashMap<String, String> hm) {

		// Checking execution flag
		if (hm.get("ExecuteFlag").trim().equalsIgnoreCase("No"))
			throw new SkipException("Skipping the test ---->> As per excel entry");

		Preconditions.checkArgument(hm != null, "The hash map parameter must not be null");				

		String filePathOfJsonResponse = HelperUtil.createOutputDir(this.getClass().getSimpleName()) + File.separator
				+ this.getClass().getSimpleName() + hm.get("SerialNo") + ".json";

		switch ((hm.get("httpstatus"))) {
		case "200":
			try {
				
				orderBookBean getFieldsResponseBean = testAPIAttribute200Response(filePathOfJsonResponse);	
				
				reporter.writeLog("verifying", "Json response", "By parsing");
				// If error response, Verify error response is same
				
				if(getFieldsResponseBean.getError()!=null){
				Assert.assertEquals(getFieldsResponseBean.getError(),hm.get("Error"),
						"The Error response is differnt : The Error should be"+hm.get("Error")+
						"But the Error is "+getFieldsResponseBean.getError());	
				break;
				}									
			
				// Exchange
				softAssert.assertTrue(getFieldsResponseBean.getExchange().equals(hm.get("I_Exchange")), 
						"Exchange shuld be "+hm.get("I_Exchange")+"But the Exchange is "+getFieldsResponseBean.getExchange());
				
				// Base
				softAssert.assertTrue(getFieldsResponseBean.getBase().equals(hm.get("I_Base")), 
						"Base shuld be "+hm.get("I_Base")+"But the Base is "+getFieldsResponseBean.getBase());
				
				//Rel
				softAssert.assertTrue(getFieldsResponseBean.getRel().equals(hm.get("I_Rel")), 
						"Rel shuld be "+hm.get("I_Rel")+"But the Rel is "+getFieldsResponseBean.getRel());
				
								
				//Depth
				softAssert.assertTrue(getFieldsResponseBean.getMaxdepth().equals(hm.get("I_Depth")), 
						"Depth shuld be "+hm.get("I_Depth")+"But the Depth is "+getFieldsResponseBean.getMaxdepth());				
				
				//Bids array should be of length Depth				
				softAssert.assertTrue(getFieldsResponseBean.getBids().length==Integer.parseInt(hm.get("I_Depth")), 
						"Bids Length shuld be "+hm.get("I_Depth")+"But the Bids length is "+getFieldsResponseBean.getBids().length);
				
				//Asks  array should be of length Depth
				softAssert.assertTrue(getFieldsResponseBean.getAsks().length==Integer.parseInt(hm.get("I_Depth")), 
						"Asks shuld be "+hm.get("I_Depth")+"But the Depth is "+getFieldsResponseBean.getAsks().length);
				
				//Nubids
				softAssert.assertTrue(Integer.parseInt(getFieldsResponseBean.getNumbids())==Integer.parseInt(hm.get("I_Depth")), 
						"numbids shuld be "+hm.get("I_Depth")+"But the numbids  is "+getFieldsResponseBean.getNumbids());
								
				//numasks				
				softAssert.assertTrue(Integer.parseInt(getFieldsResponseBean.getNumasks())==Integer.parseInt(hm.get("I_Depth")), 
						"numAsks shuld be "+hm.get("I_Depth")+"But the numasks is "+getFieldsResponseBean.getNumasks());
				
				//All fields
				if (Integer.parseInt(hm.get("I_Allfields"))==1){
					
				Bids[] bids = getFieldsResponseBean.getBids();
				
				//Average price
				softAssert.assertTrue(bids[0].getAveprice()!=null, "Average price is not displayed");
				
				//Cumulative
				softAssert.assertTrue(bids[0].getCumulative()!=null, "Cumulative is not displayed");
				
				//Offerer
				softAssert.assertTrue(bids[0].getOfferer()!=null, "Offerer is not displayed");
				
				//Price
				softAssert.assertTrue(bids[0].getPrice()!=null, "price is not displayed");
				
				//Volume
				softAssert.assertTrue(bids[0].getVolume()!=null, "Volume is not displayed");
				
				}
			

			     } catch (FileNotFoundException e) {
				e.printStackTrace();
				reporter.writeLog("FAIL", "", "Caught Exception : Response not stored in File");
				Assert.fail("Caught Exception : Response not stored in File ...");
			} catch (IOException e) {
				e.printStackTrace();
				reporter.writeLog("FAIL", "", "Caught Exception :" + e.getMessage());
				Assert.fail("Caught Exception ..." + e.getMessage());
		}
			break;

		default:
			logger.debug("Other than 200, 404 response --> {}", hm.get("httpstatus"));
			break;
		}

	}
	/**
	 * This method will return the response body for 200 status code.
	 * 
	 * @param jsonFileAbsPath
	 * @return response
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	
	private static orderBookBean testAPIAttribute200Response(String jsonFileAbsPath)
			throws JsonParseException, JsonMappingException, IOException {

		ObjectMapper objectMapper = new ObjectMapper();
		orderBookBean response200 = objectMapper.readValue(new File(jsonFileAbsPath), orderBookBean.class);
		return response200;

	}

	@AfterClass
	public void tearDown() {
		if (reporter != null) {
			logger.debug("closing local reporter file");
			reporter.flush();
			reporter.closeReport();
		}
	}
}
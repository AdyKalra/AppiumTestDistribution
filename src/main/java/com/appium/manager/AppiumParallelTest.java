package com.appium.manager;

import com.appium.utils.CommandPrompt;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;
import com.report.factory.ExtentManager;
import com.report.factory.ExtentTestManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.XML;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class AppiumParallelTest extends TestListenerAdapter {
	protected String port;
	public AppiumDriver<MobileElement> driver;
	CommandPrompt cp = new CommandPrompt();
	public AppiumManager appiumMan = new AppiumManager();
	AndroidDeviceConfiguration androidDevice = new AndroidDeviceConfiguration();
	public static Properties prop = new Properties();
	public InputStream input = null;
	public String device_udid;
	public ExtentTest testReporter;
	public AppiumDriverLocalService appiumDriverLocalService;
	int thread_device_count;
	public List<LogEntry> logEntries;
	public File logFile;
	public PrintWriter log_file_writer;
	public DesiredCapabilities capabilities = new DesiredCapabilities();

    public AppiumServiceBuilder startAppiumServer(String methodName) throws Exception {
        input = new FileInputStream("config.properties");
        prop.load(input);
        ArrayList<String> devices = androidDevice.getDeviceSerail();

        if (prop.getProperty("RUNNER").equalsIgnoreCase("distribute")) {
            System.out.println("*************" + Thread.currentThread().getName());
            System.out.println(
                    "******Running Test in Distributed Way******" + Thread.currentThread().getName().split("-")[3]);
            thread_device_count = Integer.valueOf(Thread.currentThread().getName().split("-")[3]) - 1;
        } else if (prop.getProperty("RUNNER").equalsIgnoreCase("parallel")) {
            System.out
                    .println("******Running Test in Parallel *******" + Thread.currentThread().getName().split("-")[1]);
            thread_device_count = Integer.valueOf(Thread.currentThread().getName().split("-")[1]);
        }

        // When tests are running in parallel then we get
        // Thread.currentThread().getName().split("-")[1] to get the device
        // array position
        device_udid = devices.get(thread_device_count);
        // appiumMan.appiumServerParallelMethods(device_udid, methodName);
        String category = androidDevice.deviceModel(device_udid);
        ExtentTestManager.startTest(methodName, "Mobile Appium Test",
                category + device_udid.replace(".", "_").replace(":", "_"));
        ExtentTestManager.getTest().log(LogStatus.INFO, "AppiumServerLogs", "<a href=" + System.getProperty("user.dir")
                + "/target/appiumlogs/" + device_udid + "__" + methodName + ".txt" + ">Logs</a>");

        return appiumMan.appiumServer(device_udid, methodName);

    }

    // @BeforeMethod
    public AppiumDriver<MobileElement> startDriverInstance() throws Exception {

        if (prop.getProperty("APP_TYPE").equalsIgnoreCase("web")) {
            androidWeb();
        } else if (prop.getProperty("APP_TYPE").equalsIgnoreCase("native")) {
            androidNative();
        }
        Thread.sleep(5000);
        driver = new AndroidDriver<MobileElement>(appiumMan.getAppiumUrl(), capabilities);
        return driver;
    }

    public void startLogResults(String methodName) throws FileNotFoundException {
        if (driver.toString().contains("Android on LINUX")) {
            logEntries = driver.manage().logs().get("logcat").filter(Level.ALL);
            logFile = new File(
                    System.getProperty("user.dir") + "/target/adblogs/" + device_udid + "__" + methodName + ".txt");
            log_file_writer = new PrintWriter(logFile);
        }
        System.out.println(device_udid);
        /*
		 * String category = androidDevice.deviceModel(device_udid);
		 * ExtentTestManager.startTest(methodName, "Mobile Appium Test",
		 * category + device_udid.replace(".", "_").replace(":", "_"));
		 */
	}

	// @AfterMethod
	public void endLogTestResults(ITestResult result) {
		if (result.isSuccess()) {
			ExtentTestManager.getTest().log(LogStatus.PASS, result.getMethod().getMethodName(), "Pass");
			/*
			 * ExtentTestManager.getTest().log(LogStatus.INFO, "Logs:: <a href="
			 * + System.getProperty("user.dir") + "/target/appiumlogs/" +
			 * device_udid + "__" + result.getMethod().getMethodName() + ".txt"
			 * + ">AppiumServerLogs</a>");
			 */
            if (driver.toString().contains("Android on LINUX")) {
                log_file_writer.println(logEntries);
                log_file_writer.flush();
                ExtentTestManager.getTest().log(LogStatus.INFO, result.getMethod().getMethodName(),
                        "ADBLogs:: <a href=" + System.getProperty("user.dir") + "/target/adblogs/" + device_udid + "__"
                                + result.getMethod().getMethodName() + ".txt" + ">AdbLogs</a>");
                System.out.println(driver.getSessionId() + ": Saving device log - Done.");
            }

        }
        if (result.getStatus() == ITestResult.FAILURE) {
            ExtentTestManager.getTest().log(LogStatus.FAIL, result.getMethod().getMethodName(), result.getThrowable());
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                FileUtils.copyFile(scrFile, new File(System.getProperty("user.dir") + "/target/" + device_udid
                        + result.getMethod().getMethodName() + ".png"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ExtentTestManager.getTest().log(LogStatus.INFO, result.getMethod().getMethodName(),
                    "Snapshot below: " + ExtentTestManager.getTest().addScreenCapture(System.getProperty("user.dir")
                            + "/target/" + device_udid + result.getMethod().getMethodName() + ".png"));
            if (driver.toString().contains("Android on LINUX")) {
                log_file_writer.println(logEntries);
                log_file_writer.flush();
                ExtentTestManager.getTest().log(LogStatus.INFO, result.getMethod().getMethodName(),
                        "ADBLogs:: <a href=" + System.getProperty("user.dir") + "/target/adblogs/" + device_udid + "__"
                                + result.getMethod().getMethodName() + ".txt" + ">AdbLogs</a>");
                System.out.println(driver.getSessionId() + ": Saving device log - Done.");
            }

		}
		if (result.getStatus() == ITestResult.SKIP) {
			ExtentTestManager.getTest().log(LogStatus.SKIP, "Test skipped");
		}

	}

    public void killAppiumServer() throws InterruptedException, IOException {
        System.out.println("**************ClosingAppiumSession****************");
        ExtentTestManager.endTest();
        ExtentManager.getInstance().flush();
        if (driver.toString().contains("Android on LINUX")) {
            System.out.println("Closing Session::" + driver.getSessionId());
            driver.closeApp();
        } else if (prop.getProperty("APP_TYPE").equalsIgnoreCase("web")) {
            driver.quit();
        }
        appiumMan.destroyAppiumNode();
    }

	protected String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	public void waitForElement(By id, int time) {
		WebDriverWait wait = new WebDriverWait(driver, 20);
		wait.until(ExpectedConditions.elementToBeClickable((id)));
	}

	public void resetAppData() throws InterruptedException, IOException {
		androidDevice.clearAppData(device_udid, prop.getProperty("APP_PACKAGE"));
	}

	public void closeOpenApp() throws InterruptedException, IOException {
		androidDevice.closeRunningApp(device_udid, prop.getProperty("APP_PACKAGE"));
	}

	public void androidNative() {
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android");
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
		capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "5.X");
		if (Integer.parseInt(androidDevice.deviceOS(device_udid)) <= 16) {
			capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "Selendroid");
		}
		capabilities.setCapability(MobileCapabilityType.APP, prop.getProperty("APP_PATH"));
		capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, prop.getProperty("APP_PACKAGE"));
		capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, prop.getProperty("APP_ACTIVITY"));
		if (prop.getProperty("APP_WAIT_ACTIVITY") != null) {
			capabilities.setCapability(MobileCapabilityType.APP_WAIT_ACTIVITY, prop.getProperty("APP_WAIT_ACTIVITY"));
		}
	}

	public void androidWeb() {
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android");
		capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "5.0.X");
		// If you want the tests on real device, make sure chrome browser is
		// installed
		capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, prop.getProperty("BROWSER_TYPE"));
		capabilities.setCapability(MobileCapabilityType.SUPPORTS_ALERTS, true);
		capabilities.setCapability(MobileCapabilityType.TAKES_SCREENSHOT, true);
	}

	public void captureAndroidScreenShot(String screenShotName) {
		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		try {
			String androidModel = androidDevice.deviceModel(device_udid);
			FileUtils.copyFile(scrFile, new File(System.getProperty("user.dir") + "/target/screenshot/" + device_udid + "/"
					+ androidModel + "/" + screenShotName + ".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public void convertXmlToJSon() throws IOException {
		String fileName = "report.json";
		BufferedWriter bufferedWriter = null;
		try {
			int i = 1;
			FileWriter fileWriter;
			int dir_1 = new File(System.getProperty("user.dir") + "/test-output/junitreports").listFiles().length;

			List textFiles = new ArrayList();
			File dir = new File(System.getProperty("user.dir") + "/test-output/junitreports");

			for (File file : dir.listFiles()) {
				if (file.getName().contains(("Test"))) {
					System.out.println(file);

					fileWriter = new FileWriter(fileName, true);
					InputStream inputStream = new FileInputStream(file);
					StringBuilder builder = new StringBuilder();
					int ptr = 0;
					while ((ptr = inputStream.read()) != -1) {
						builder.append((char) ptr);
					}

					String xml = builder.toString();
					JSONObject jsonObj = XML.toJSONObject(xml);

					// Always wrap FileWriter in BufferedWriter.
					bufferedWriter = new BufferedWriter(fileWriter);

					// Always close files.
					String jsonPrettyPrintString = jsonObj.toString(4);
					// bufferedWriter.write(jsonPrettyPrintString);
					if (i == 1) {
						bufferedWriter.append("[");
					}
					bufferedWriter.append(jsonPrettyPrintString);
					if (i != dir_1) {
						bufferedWriter.append(",");
					}

					if (i == dir_1) {
						bufferedWriter.append("]");
					}

					bufferedWriter.newLine();
					bufferedWriter.close();
					i++;
				}
			}
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

package online.qastudy.okd.demo.deployment;


import online.qastudy.okd.demo.utils.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;


public class DemoDeploymetnTest {
    private DemoDeployment demoDeployment;
        private WebDriver driver;

        @Before
        public void deploy() {
            System.setProperty("webdriver.chrome.driver","/home/ymaks/sources/IdeaProjects/CODE/XPDays-2018/src/test/resources/chromedriver");
            driver = new ChromeDriver();
            driver.manage().timeouts().pageLoadTimeout(50, TimeUnit.SECONDS);
            driver.manage().window().maximize();

            demoDeployment = new DemoDeployment("xpdays-2018");
        }

        @After
        public void cleanup() {
            demoDeployment.close();
        }

        @Test
        public void testAppDeployment() {

            demoDeployment.login()
                    .createNewProject("xpdays-2018", "Demo for XPDays 2018", "Demo of Fabric8")
                    .deployPod()
                    .deployService()
                    .createRout();

            driver.navigate().to(demoDeployment.getApplicationURL());
            Util.WaitUntilAppWillBeReady();
            driver.navigate().refresh();

            assertThat(driver.findElement(By.className("center")).getText()).contains("Hello from POD!!");
        }
}

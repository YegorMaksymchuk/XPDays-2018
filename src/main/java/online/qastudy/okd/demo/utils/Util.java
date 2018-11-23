package online.qastudy.okd.demo.utils;

import cz.xtf.wait.Waiters;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public final class Util {

    public static final String HTTP = "http://";
    public static final String APP_URL = "pod-demo-xpdays-2018.127.0.0.1.nip.io";

    private Util() {
    }

    public static void WaitUntilAppWillBeReady() {
        try {
            Waiters.doesUrlReturnOK(HTTP + APP_URL).timeout(TimeUnit.SECONDS, 30L).execute();
            log.info("Wait 5 seconds.");
            Waiters.sleep(TimeUnit.SECONDS, 5);
        } catch (TimeoutException e) {
            log.error(e.getMessage());
        }
    }
}

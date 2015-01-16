package kfs.springutils;

import java.io.IOException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author pavedrim
 */
public class RunApp {

    public static String defaultContextFile = "appContext.xml";
    public static String defaultPidFilePropertyName = "pidfile";
    
    public static void run(String profileName) {
        run(profileName, defaultPidFilePropertyName, defaultContextFile);
    }
    
    public static void run(String profileName, String pidPropertyFilename, String contextFile) {
        if (pidPropertyFilename != null) {
            String pidf = System.getProperty(pidPropertyFilename);
            if ((pidf != null) && !pidf.isEmpty()) {
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "echo $PPID > " + pidf);
                try {
                    Process p = pb.start();
                } catch (IOException e) {
                }
            }
        }
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext();
        ctx.setConfigLocation(contextFile);
        ctx.getEnvironment().setActiveProfiles(profileName);
        ctx.refresh();
    }
}

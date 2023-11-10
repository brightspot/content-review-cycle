package brightspot.reviewcycle.task;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultTaskHost {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskHost.class);

    private DefaultTaskHost() {
    }

    public static boolean isRunningOnTaskHost() {
        String defaultTaskHost = Singleton.getInstance(CmsTool.class).as(DefaultTaskHostSetting.class).getDefaultTaskHost();
        if (!StringUtils.isEmpty(defaultTaskHost)) {
            try {
                InetAddress localAddress = InetAddress.getLocalHost();
                InetAddress allowedAddress = InetAddress.getByName(defaultTaskHost);
                if (localAddress.getHostAddress().equals(allowedAddress.getHostAddress())) {
                    return true;
                }
            } catch (UnknownHostException e) {
                LOGGER.warn("Exception finding host name; message: " + e.getMessage());
            }
        }
        return false;
    }
}
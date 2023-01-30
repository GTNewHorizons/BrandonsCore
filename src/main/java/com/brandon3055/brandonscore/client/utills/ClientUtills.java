package com.brandon3055.brandonscore.client.utills;

import java.net.URI;

import com.brandon3055.brandonscore.common.utills.LogHelper;

/**
 * Created by brandon3055 on 11/11/2015.
 */
public class ClientUtills {

    public static void openLink(String url) {
        try {
            URI uri = new URI(url);
            Class oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            oclass.getMethod("browse", new Class[] { URI.class }).invoke(object, new Object[] { uri });
        } catch (Throwable throwable) {
            LogHelper.error("Couldn\'t open link");
            throwable.printStackTrace();
        }
    }
}

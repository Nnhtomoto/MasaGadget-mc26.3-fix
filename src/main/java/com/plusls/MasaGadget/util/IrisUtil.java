package com.plusls.MasaGadget.util;

import java.lang.reflect.Method;

public final class IrisUtil {
    private static final String IRIS_API_CLASS_NAME = "net.irisshaders.iris.api.v0.IrisApi";
    private static boolean resolved = false;
    private static Method getInstanceMethod = null;
    private static Method isShaderPackInUseMethod = null;

    private IrisUtil() {
    }

    public static boolean isShaderPackInUse() {
        if (!IrisUtil.resolved) {
            IrisUtil.resolved = true;

            try {
                Class<?> clazz = Class.forName(IrisUtil.IRIS_API_CLASS_NAME);
                IrisUtil.getInstanceMethod = clazz.getMethod("getInstance");
                IrisUtil.isShaderPackInUseMethod = clazz.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
                IrisUtil.getInstanceMethod = null;
                IrisUtil.isShaderPackInUseMethod = null;
            }
        }

        if (IrisUtil.getInstanceMethod == null || IrisUtil.isShaderPackInUseMethod == null) {
            return false;
        }

        try {
            Object api = IrisUtil.getInstanceMethod.invoke(null);
            return api != null && (boolean) IrisUtil.isShaderPackInUseMethod.invoke(api);
        } catch (Throwable ignored) {
            return false;
        }
    }
}

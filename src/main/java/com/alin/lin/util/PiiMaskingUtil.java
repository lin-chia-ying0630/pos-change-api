package com.alin.lin.util;

/**
 * 應用程式 log 使用的個資遮蔽工具，避免保單號碼直接寫入 console 或集中式日誌。
 */
public final class PiiMaskingUtil {
    private PiiMaskingUtil() {
    }

    public static String maskPolicyNo(String policyNo) {
        if (policyNo == null || policyNo.isBlank()) {
            return "";
        }
        int length = policyNo.length();
        if (length <= 2) {
            return "*".repeat(length);
        }
        if (length <= 6) {
            return policyNo.charAt(0) + "***" + policyNo.charAt(length - 1);
        }
        return policyNo.substring(0, 3) + "***" + policyNo.substring(length - 3);
    }
}

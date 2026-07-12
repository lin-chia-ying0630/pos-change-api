package com.alin.lin.interceptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskedSqlLogInterceptorTest {
    @Test
    void maskValueShouldMaskEmail() {
        assertEquals("p***@example.com", MaskedSqlLogInterceptor.maskValue("policyholder@example.com"));
    }

    @Test
    void maskValueShouldMaskPhone() {
        assertEquals("091****678", MaskedSqlLogInterceptor.maskValue("0912345678"));
    }

    @Test
    void maskValueShouldMaskPolicyNo() {
        assertEquals("P00***001", MaskedSqlLogInterceptor.maskValue("P000000001"));
    }

    @Test
    void maskValueShouldMaskPolicyNoByFieldName() {
        assertEquals("P00***001", MaskedSqlLogInterceptor.maskValue("policyNo", "P000000001"));
    }

    @Test
    void maskValueShouldMaskAddressByFieldName() {
        assertEquals("臺北市中正區***", MaskedSqlLogInterceptor.maskValue("fullWidthAddress", "臺北市中正區重慶南路一段100號"));
    }

    @Test
    void maskValueShouldMaskTelByFieldName() {
        assertEquals("022-****789", MaskedSqlLogInterceptor.maskValue("telNo", "022-3456789"));
    }

    @Test
    void maskValueShouldKeepNumber() {
        assertEquals("1", MaskedSqlLogInterceptor.maskValue(1));
    }
}

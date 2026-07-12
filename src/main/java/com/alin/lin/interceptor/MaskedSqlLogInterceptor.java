package com.alin.lin.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
})
public class MaskedSqlLogInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(MaskedSqlLogInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (log.isDebugEnabled()) {
            logSql(invocation);
        }
        return invocation.proceed();
    }

    private void logSql(Invocation invocation) {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameterObject = args.length > 1 ? args[1] : null;
        BoundSql boundSql = args.length >= 6 ? (BoundSql) args[5] : mappedStatement.getBoundSql(parameterObject);

        log.debug("SQL id: {}", mappedStatement.getId());
        log.debug("SQL: {}", normalizeSql(boundSql.getSql()));
        log.debug("Masked parameters: {}", maskedParameters(mappedStatement, boundSql, parameterObject));
    }

    private String maskedParameters(MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            return "[]";
        }

        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        MetaObject metaObject = parameterObject == null ? null : mappedStatement.getConfiguration().newMetaObject(parameterObject);
        for (ParameterMapping parameterMapping : parameterMappings) {
            String propertyName = parameterMapping.getProperty();
            Object value = parameterValue(boundSql, metaObject, parameterObject, propertyName);
            joiner.add(propertyName + "=" + maskValue(propertyName, value));
        }
        return joiner.toString();
    }

    private Object parameterValue(BoundSql boundSql, MetaObject metaObject, Object parameterObject, String propertyName) {
        if (boundSql.hasAdditionalParameter(propertyName)) {
            return boundSql.getAdditionalParameter(propertyName);
        }
        if (metaObject != null && metaObject.hasGetter(propertyName)) {
            return metaObject.getValue(propertyName);
        }
        return isSimpleValue(parameterObject) ? parameterObject : null;
    }

    private boolean isSimpleValue(Object value) {
        return value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    static String maskValue(String propertyName, Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }

        String source = String.valueOf(value).trim();
        if (source.isEmpty()) {
            return "";
        }
        String normalizedPropertyName = normalizePropertyName(propertyName);
        if (isPolicyNoField(normalizedPropertyName)) {
            return maskPolicyNo(source);
        }
        if (isAddressField(normalizedPropertyName)) {
            return maskAddress(source);
        }
        if (isTelField(normalizedPropertyName)) {
            return maskPhone(source);
        }
        if (source.contains("@")) {
            return maskEmail(source);
        }
        String digits = source.replaceAll("\\D", "");
        if (digits.length() >= 7 && source.matches("[\\d\\s()+\\-]+")) {
            return maskPhone(source);
        }
        return maskText(source);
    }

    static String maskValue(Object value) {
        return maskValue(null, value);
    }

    private static String normalizePropertyName(String propertyName) {
        return propertyName == null ? "" : propertyName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private static boolean isPolicyNoField(String propertyName) {
        return propertyName.endsWith("policyno");
    }

    private static boolean isAddressField(String propertyName) {
        return propertyName.endsWith("address")
                || propertyName.equals("add")
                || propertyName.equals("addr")
                || propertyName.contains("fullwidthaddress")
                || propertyName.contains("halfwidthaddress");
    }

    private static boolean isTelField(String propertyName) {
        return propertyName.contains("tel")
                || propertyName.contains("phone")
                || propertyName.contains("mobile")
                || propertyName.contains("cell");
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskText(email);
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + domain;
    }

    private static String maskPolicyNo(String policyNo) {
        return maskText(policyNo);
    }

    private static String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 6) {
            return "*".repeat(phone.length());
        }
        String maskedDigits = digits.substring(0, 3) + "****" + digits.substring(digits.length() - 3);
        StringBuilder result = new StringBuilder();
        int digitIndex = 0;
        for (int i = 0; i < phone.length(); i++) {
            char current = phone.charAt(i);
            if (Character.isDigit(current)) {
                result.append(digitIndex < maskedDigits.length() ? maskedDigits.charAt(digitIndex) : '*');
                digitIndex++;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static String maskAddress(String address) {
        int length = address.length();
        if (length <= 6) {
            return "*".repeat(length);
        }
        return address.substring(0, 6) + "***";
    }

    private static String maskText(String text) {
        int length = text.length();
        if (length <= 2) {
            return "*".repeat(length);
        }
        if (length <= 6) {
            return text.charAt(0) + "***" + text.charAt(length - 1);
        }
        return text.substring(0, 3) + "***" + text.substring(length - 3);
    }
}

package org.lyi.cha;

import java.util.regex.Pattern;

/**
 * 隧道主机名校验，规则与 Cloudflared-Helper 的 cf-helper.sh 保持一致：
 * RFC 1123（标签 1-63 字符、总长不超过 253、不含下划线）、至少两段，
 * 自动剥离 scheme、路径/查询/片段、:port 与末尾点号。
 * cloudflared 不支持 IP 地址，IPv4/IPv6 及其数字变体一律拒绝。
 */
final class HostnameUtil {
    private static final Pattern HOSTNAME = Pattern.compile(
            "[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+");
    private static final Pattern SCHEME = Pattern.compile("[A-Za-z][A-Za-z0-9+.-]*");
    private static final Pattern IPV6_CHARS = Pattern.compile("[0-9A-Fa-f:.]+");
    private static final Pattern PORT_SUFFIX = Pattern.compile("^(.+):[0-9]+$");
    private static final Pattern NUMERIC_LABEL = Pattern.compile("[0-9]+|0[xX][0-9A-Fa-f]*");

    private HostnameUtil() {
    }

    /** 返回规范化后的主机名；无效（含 IP 地址）时返回 null。 */
    static String normalize(String raw) {
        String s = strip(raw);
        if (s.isEmpty() || s.length() > 253 || isIp(s)) return null;
        return HOSTNAME.matcher(s).matches() ? s : null;
    }

    /** 输入（剥离 URL 外壳后）是否是 IP 地址写法，用于给出更明确的错误提示。 */
    static boolean isIpAddress(String raw) {
        return isIp(strip(raw));
    }

    private static String strip(String raw) {
        String v = raw == null ? "" : raw.trim();
        int i = v.indexOf("://");
        if (i > 0 && SCHEME.matcher(v.substring(0, i)).matches()) v = v.substring(i + 3);
        for (int k = 0; k < v.length(); k++) {
            char c = v.charAt(k);
            if (c == '/' || c == '?' || c == '#') {
                v = v.substring(0, k);
                break;
            }
        }
        if (isIpv6(v)) return v;
        java.util.regex.Matcher m = PORT_SUFFIX.matcher(v);
        if (m.matches()) v = m.group(1);
        if (v.endsWith(".")) v = v.substring(0, v.length() - 1);
        return v;
    }

    private static boolean isIpv6(String v) {
        if (v.startsWith("[")) return true;
        int colons = 0;
        for (int k = 0; k < v.length(); k++) if (v.charAt(k) == ':') colons++;
        return colons >= 2 && IPV6_CHARS.matcher(v).matches();
    }

    private static boolean isIp(String v) {
        if (isIpv6(v)) return true;
        String last = v.substring(v.lastIndexOf('.') + 1);
        return NUMERIC_LABEL.matcher(last).matches();
    }
}

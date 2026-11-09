package io.github.omarmokhtarsaad.apitest.framework.logging;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Masks sensitive values in captured HTTP text before it is written anywhere (report, console,
 * assertion message).
 *
 * <p>Applied, in order:
 *
 * <ol>
 *   <li>whole header lines whose name is sensitive ({@code Authorization}, {@code Cookie}, ...)
 *   <li>{@code Bearer <token>} anywhere in the text
 *   <li>JSON members whose key is sensitive (string, number and boolean values)
 *   <li>{@code key=value} pairs in query strings and form bodies
 * </ol>
 *
 * <p>Key matching is case-insensitive. Extra keys (domain-specific identifiers such as {@code
 * nationalId} or {@code accountNumber}) come from {@code redact.extraKeys} and apply to all four
 * rules. This is a safety net, not a licence to log secrets.
 */
public final class Redactor {

    public static final String MASK = "[REDACTED]";

    /** Header names masked by default. */
    static final Set<String> DEFAULT_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "x-auth-token",
            "x-csrf-token");

    /** Body and parameter keys masked by default. */
    static final Set<String> DEFAULT_KEYS = Set.of(
            "password",
            "pass",
            "pwd",
            "token",
            "accesstoken",
            "access_token",
            "refreshtoken",
            "refresh_token",
            "idtoken",
            "id_token",
            "otp",
            "pin",
            "secret",
            "clientsecret",
            "client_secret",
            "apikey",
            "api_key",
            "authorization",
            "phone",
            "phonenumber",
            "phone_number",
            "mobile",
            "mobilenumber",
            "email",
            "emailaddress");

    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\b(bearer)\\s+[A-Za-z0-9._~+/=-]+");

    private final Pattern headerLine;
    private final Pattern jsonMember;
    private final Pattern keyValuePair;

    /** A redactor with the default keys only. */
    public Redactor() {
        this(Set.of());
    }

    /** A redactor with the default keys plus {@code extraKeys} (header names or body keys). */
    public Redactor(Collection<String> extraKeys) {
        Set<String> extras = Objects.requireNonNull(extraKeys, "extraKeys").stream()
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> headers = union(DEFAULT_HEADERS, extras);
        Set<String> keys = union(DEFAULT_KEYS, extras);

        headerLine = Pattern.compile("(?im)^(\\s*(?:" + alternation(headers) + ")\\s*:).*$");
        jsonMember = Pattern.compile("(?i)(\"(?:" + alternation(keys) + ")\"\\s*:\\s*)"
                + "(\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|true|false)");
        keyValuePair = Pattern.compile("(?im)(^|[?&\\s])((?:" + alternation(keys) + ")=)[^&\\s]*");
    }

    /** {@code text} with every known sensitive value replaced by {@link #MASK}. Null-safe. */
    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String mask = Matcher.quoteReplacement(MASK);
        String result = headerLine.matcher(text).replaceAll("$1 " + mask);
        result = BEARER_TOKEN.matcher(result).replaceAll("$1 " + mask);
        result = jsonMember.matcher(result).replaceAll("$1\"" + mask + "\"");
        result = keyValuePair.matcher(result).replaceAll("$1$2" + mask);
        return result;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> all = new LinkedHashSet<>(a);
        all.addAll(b);
        return all;
    }

    private static String alternation(Set<String> keys) {
        return keys.stream().map(Pattern::quote).collect(Collectors.joining("|"));
    }
}

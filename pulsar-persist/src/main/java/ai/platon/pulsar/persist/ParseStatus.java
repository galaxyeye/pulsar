/**
 * Autogenerated by Avro
 * <p>
 * DO NOT EDIT DIRECTLY
 */
package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.gora.generated.GParseStatus;
import ai.platon.pulsar.persist.metadata.ParseStatusCodes;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>ParseStatus class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ParseStatus implements ParseStatusCodes {
    /** Constant <code>REFRESH_HREF="refreshHref"</code> */
    public static final String REFRESH_HREF = "refreshHref";
    /** Constant <code>REFRESH_TIME="refreshTime"</code> */
    public static final String REFRESH_TIME = "refreshTime";

    /** Constant <code>majorCodes</code> */
    public static final HashMap<Short, String> majorCodes = new HashMap<>();
    /** Constant <code>minorCodes</code> */
    public static final HashMap<Integer, String> minorCodes = new HashMap<>();

    static {
        majorCodes.put(NOTPARSED, "notparsed");
        majorCodes.put(SUCCESS, "success");
        majorCodes.put(FAILED, "failed");

        minorCodes.put(SUCCESS_OK, "ok");
        minorCodes.put(SUCCESS_REDIRECT, "redirect");

        minorCodes.put(FAILED_EXCEPTION, "exception");
        minorCodes.put(FAILED_NOT_SPECIFIED, "not_specified");
        minorCodes.put(FAILED_TRUNCATED, "truncated");
        minorCodes.put(FAILED_INVALID_FORMAT, "invalid_format");
        minorCodes.put(FAILED_MISSING_PARTS, "missing_parts");
        minorCodes.put(FAILED_MISSING_CONTENT, "missing_content");
        minorCodes.put(FAILED_NO_PARSER, "no_parser");
        minorCodes.put(FAILED_MALFORMED_URL, "malformed_url");
        minorCodes.put(FAILED_UNKNOWN_ENCODING, "unknown_encoding");
    }

    private final GParseStatus parseStatus;

    /**
     * <p>Constructor for ParseStatus.</p>
     *
     * @param majorCode a short.
     * @param minorCode a int.
     */
    public ParseStatus(short majorCode, int minorCode) {
        this.parseStatus = GParseStatus.newBuilder().build();
        setMajorCode(majorCode);
        setMinorCode(minorCode);
    }

    /**
     * <p>Constructor for ParseStatus.</p>
     *
     * @param majorCode a short.
     * @param minorCode a int.
     * @param message a {@link java.lang.String} object.
     */
    public ParseStatus(short majorCode, int minorCode, String message) {
        this.parseStatus = GParseStatus.newBuilder().build();
        setMajorCode(majorCode);
        setMinorCode(minorCode);
        getArgs().put(getMinorName(minorCode), message == null ? "(unknown)" : message);
    }

    private ParseStatus(GParseStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    /**
     * <p>box.</p>
     *
     * @param parseStatus a {@link ai.platon.pulsar.persist.gora.generated.GParseStatus} object.
     * @return a {@link ai.platon.pulsar.persist.ParseStatus} object.
     */
    @Nonnull
    public static ParseStatus box(GParseStatus parseStatus) {
        Objects.requireNonNull(parseStatus);
        return new ParseStatus(parseStatus);
    }

    /**
     * <p>getMajorName.</p>
     *
     * @param code a short.
     * @return a {@link java.lang.String} object.
     */
    public static String getMajorName(short code) {
        return majorCodes.getOrDefault(code, "unknown");
    }

    /**
     * <p>getMinorName.</p>
     *
     * @param code a int.
     * @return a {@link java.lang.String} object.
     */
    public static String getMinorName(int code) {
        return minorCodes.getOrDefault(code, "unknown");
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link ai.platon.pulsar.persist.gora.generated.GParseStatus} object.
     */
    public GParseStatus unbox() {
        return parseStatus;
    }

    /**
     * <p>setCode.</p>
     *
     * @param majorCode a short.
     * @param minorCode a int.
     */
    public void setCode(short majorCode, int minorCode) {
        setMajorCode(majorCode);
        setMinorCode(minorCode);
    }

    /**
     * <p>getMajorName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMajorName() {
        return getMajorName(getMajorCode());
    }

    /**
     * <p>getMajorCode.</p>
     *
     * @return a short.
     */
    public short getMajorCode() {
        return parseStatus.getMajorCode().shortValue();
    }

    /**
     * <p>setMajorCode.</p>
     *
     * @param majorCode a short.
     */
    public void setMajorCode(short majorCode) {
        parseStatus.setMajorCode((int) majorCode);
    }

    /**
     * <p>getMinorName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMinorName() {
        return getMinorName(getMinorCode());
    }

    /**
     * <p>getMinorCode.</p>
     *
     * @return a int.
     */
    public int getMinorCode() {
        return parseStatus.getMinorCode();
    }

    /**
     * <p>setMinorCode.</p>
     *
     * @param minorCode a int.
     */
    public void setMinorCode(int minorCode) {
        parseStatus.setMinorCode(minorCode);
    }

    /**
     * <p>setMinorCode.</p>
     *
     * @param minorCode a int.
     * @param message a {@link java.lang.String} object.
     */
    public void setMinorCode(int minorCode, String message) {
        setMinorCode(minorCode);
        getArgs().put(getMinorName(), message);
    }

    /**
     * <p>getArgs.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<CharSequence, CharSequence> getArgs() {
        return parseStatus.getArgs();
    }

    /**
     * <p>setArgs.</p>
     *
     * @param args a {@link java.util.Map} object.
     */
    public void setArgs(Map<CharSequence, CharSequence> args) {
        parseStatus.setArgs(args);
    }

    /**
     * <p>setSuccessOK.</p>
     */
    public void setSuccessOK() {
        setCode(ParseStatus.SUCCESS, ParseStatus.SUCCESS_OK);
    }

    /**
     * <p>setFailed.</p>
     *
     * @param minorCode a int.
     * @param message a {@link java.lang.String} object.
     */
    public void setFailed(int minorCode, String message) {
        setMajorCode(FAILED);
        setMinorCode(minorCode, message);
    }

    /**
     * <p>isParsed.</p>
     *
     * @return a boolean.
     */
    public boolean isParsed() {
        return getMajorCode() != NOTPARSED;
    }

    /**
     * <p>isSuccess.</p>
     *
     * @return a boolean.
     */
    public boolean isSuccess() {
        return getMajorCode() == SUCCESS;
    }

    /**
     * <p>isFailed.</p>
     *
     * @return a boolean.
     */
    public boolean isFailed() {
        return getMajorCode() == FAILED;
    }

    /**
     * <p>isRedirect.</p>
     *
     * @return a boolean.
     */
    public boolean isRedirect() {
        return isSuccess() && getMinorCode() == SUCCESS_REDIRECT;
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return majorCodes.getOrDefault(getMajorCode(), "unknown") + "/"
                + minorCodes.getOrDefault(getMinorCode(), "unknown");
    }

    /**
     * <p>getArgOrDefault.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getArgOrDefault(String name, String defaultValue) {
        return getArgs().getOrDefault(name, defaultValue).toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String args = getArgs().entrySet().stream()
                .map(e -> Pair.of(e.getKey().toString(), e.getValue() == null ? "(null)" : e.getValue().toString()))
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        return getName() +
                " (" + getMajorCode() + "/" + getMinorCode() + ")" +
                ", args=[" + args + "]";
    }
}

package org.schoolkernel.contract;

public final class KernelMetadata {
    private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile(
            "[0-9]+\\.[0-9]+\\.[0-9]+(?:[-.][0-9A-Za-z][0-9A-Za-z.-]*)?");

    private KernelMetadata() {}

    public static String version() {
        String packaged = KernelMetadata.class.getPackage().getImplementationVersion();
        if (valid(packaged)) {
            return packaged;
        }
        String testOverride = System.getProperty("school.kernel.version");
        if (valid(testOverride)) {
            return testOverride;
        }
        throw new IllegalStateException("Packaged kernel version metadata is missing or malformed");
    }

    private static boolean valid(String value) {
        return value != null && VERSION.matcher(value).matches();
    }
}

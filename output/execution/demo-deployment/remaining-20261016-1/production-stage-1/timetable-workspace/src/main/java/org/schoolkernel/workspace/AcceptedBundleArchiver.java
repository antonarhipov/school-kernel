package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.Map;

public interface AcceptedBundleArchiver {
    byte[] create(Map<String, byte[]> entries) throws IOException;
}

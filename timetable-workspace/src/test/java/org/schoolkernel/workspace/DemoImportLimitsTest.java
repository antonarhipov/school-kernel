package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DemoImportLimitsTest {
    final SafeImportReader demo=new SafeImportReader(true);
    byte[] archive(Map<String,byte[]> entries) throws Exception {
        var output=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(output)){for(var entry:entries.entrySet()){zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();}}return output.toByteArray();
    }
    @Test void demoRejectsCompressedExpandedPerDocumentAndMalformedUtf8BeforeVerification() throws Exception {
        byte[] atLimit=("{}"+" ".repeat(2*1024*1024-2)).getBytes(StandardCharsets.UTF_8);
        assertNotNull(demo.read(new MockMultipartFile("definition",atLimit),null,null));
        byte[] over=Arrays.copyOf(atLimit,atLimit.length+1);
        assertEquals("IMPORT_TOO_LARGE",assertThrows(WorkspaceProblem.class,()->demo.read(new MockMultipartFile("definition",over),null,null)).code());
        assertEquals("IMPORT_TOO_LARGE",assertThrows(WorkspaceProblem.class,()->demo.readArchive(new byte[5*1024*1024+1])).code());
        assertEquals("MALFORMED_IMPORT",assertThrows(WorkspaceProblem.class,()->demo.read(new MockMultipartFile("definition",new byte[]{(byte)0xc3,0x28}),null,null)).code());
        assertEquals("IMPORT_TOO_LARGE",assertThrows(WorkspaceProblem.class,()->demo.readArchive(archive(Map.of("school-definition.json",over,"timetable-result.json","{}".getBytes(),"workspace-manifest.json","{}".getBytes())))).code());
        for(String content:List.of("{}{}","{\"x\":1,\"x\":2}","[]","null"))assertThrows(WorkspaceProblem.class,()->demo.read(new MockMultipartFile("definition",content.getBytes(StandardCharsets.UTF_8)),null,null));
        // Original local profile keeps its larger 10 MiB JSON ceiling.
        assertNotNull(new SafeImportReader().read(new MockMultipartFile("definition",("{}"+" ".repeat(2*1024*1024)).getBytes()),null,null));
    }
}

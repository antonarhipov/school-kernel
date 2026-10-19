package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;

class AudienceRepairContractTest {
    final ObjectMapper json=JsonMapper.builder().build();
    @Test void mixedPersistentAndAttemptLocksPreserveTheirLifetimeAndExactSource() throws Exception {
        var pair=json.readTree(Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json"))).path("nested");
        ObjectNode definition=(ObjectNode)pair.path("definition").deepCopy();
        ArrayNode locks=definition.putArray("sessionLocks");locks.addObject().put("sessionId","e1").put("periodId","1-2");locks.addObject().put("sessionId","e2").put("roomId","ER");
        ObjectNode result=json.createObjectNode().put("schemaVersion",2).put("inputRevision","baseline-definition").put("timetableRevision","baseline-timetable");result.putObject("timetable").set("assignments",pair.path("assignments"));
        var manifests=new ManifestService();var manifest=(ObjectNode)manifests.validatedOrGenerated(new ImportDocuments(definition,result,null,ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        ((ObjectNode)manifest.path("locks").get(1)).put("roomLockOrigin","ATTEMPT_SCOPED");
        ObjectNode document=json.createObjectNode();var accepted=document.putObject("acceptedBaseline");accepted.set("definition",definition);accepted.set("result",result);accepted.set("manifest",manifest);
        var draft=document.putObject("repairDraft");var intent=draft.putObject("intent");intent.putArray("changes");intent.putArray("bulkActions");var pin=intent.putArray("pins").addObject().put("lessonId","h1");pin.putArray("periodSources").add("INDIVIDUAL");pin.putArray("roomSources").add("INDIVIDUAL");
        JsonNode source=document.deepCopy();ObjectNode compiled=new RepairDraftService(null,null,json).compiledDefinition(document);
        assertEquals(source,document);assertEquals("baseline-definition",compiled.path("basedOnRevision").asString());
        assertEquals(json.readTree("[{\"sessionId\":\"e1\",\"periodId\":\"1-2\"},{\"sessionId\":\"h1\",\"periodId\":\"3-4\",\"roomId\":\"HR1\"}]"),compiled.path("sessionLocks"));
        JsonNode next=manifests.forAcceptedRepair(compiled,result,manifest,draft);
        assertEquals("PERSISTENT_POLICY",next.path("locks").get(0).path("periodLockOrigin").asString());assertEquals("ATTEMPT_SCOPED",next.path("locks").get(1).path("periodLockOrigin").asString());assertEquals("ATTEMPT_SCOPED",next.path("locks").get(1).path("roomLockOrigin").asString());
        assertEquals(next,manifests.validatedOrGenerated(new ImportDocuments(compiled,result,next,ImportDocuments.ImportMode.ACCEPTED_BASELINE)));
    }
}

package org.schoolkernel.workspace;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

/** Read-only descriptors for both public contracts. Never serialize these over the source definition. */
final class WorkspaceSessions {
    private WorkspaceSessions() {}
    static ArrayNode lessons(JsonNode definition) {
        if (definition.path("schemaVersion").asInt() != 2) return (ArrayNode) definition.path("lessons");
        var mapper=JsonMapper.builder().build();
        ArrayNode result=definition.path("lessons").isArray()?(ArrayNode)definition.path("lessons").deepCopy():mapper.createArrayNode();
        for(JsonNode plan:definition.path("audiencePlans")) for(JsonNode portion:plan.path("portions"))
            for(JsonNode id:portion.path("sessionIds")) {
                ObjectNode lesson=(ObjectNode)plan.deepCopy();
                portion.propertyStream().forEach(e->lesson.set(e.getKey(),e.getValue().deepCopy()));
                lesson.put("id",id.asString());lesson.put("cohortId",plan.path("classId").asString());
                lesson.put("portionId",portion.path("id").asString());
                if(!lesson.has("displayName"))lesson.put("displayName",id.asString());
                if("fixed".equals(portion.path("teacherPolicy").path("mode").asString()))
                    lesson.set("teacherId",portion.path("teacherPolicy").path("fixedTeacherId"));
                result.add(lesson);
            }
        for(JsonNode lesson:result)for(JsonNode lock:definition.path("sessionLocks"))if(lesson.path("id").equals(lock.path("sessionId"))) {
            if(lock.has("periodId"))((ObjectNode)lesson).set("periodLock",lock.path("periodId"));
            if(lock.has("roomId"))((ObjectNode)lesson).set("roomLock",lock.path("roomId"));
        }
        return result;
    }
    static void lock(ObjectNode definition,String id,String dimension,String value) {
        if(definition.path("schemaVersion").asInt()!=2) {
            for(JsonNode lesson:definition.path("lessons"))if(id.equals(lesson.path("id").asString()))
                ((ObjectNode)lesson).put(dimension.equals("PERIOD")?"periodLock":"roomLock",value);
            return;
        }
        ArrayNode locks=definition.has("sessionLocks")?(ArrayNode)definition.path("sessionLocks"):definition.putArray("sessionLocks");
        ObjectNode target=null;
        for(JsonNode lock:locks)if(id.equals(lock.path("sessionId").asString()))target=(ObjectNode)lock;
        if(target==null)target=locks.addObject().put("sessionId",id);
        target.put(dimension.equals("PERIOD")?"periodId":"roomId",value);
    }
    static void clearAttemptLocks(ObjectNode definition,JsonNode manifest) {
        for(JsonNode lock:manifest.path("locks")) {
            String id=lock.path("lessonId").asString();
            if(definition.path("schemaVersion").asInt()==2) {
                if(!definition.has("sessionLocks"))continue;
                ArrayNode locks=(ArrayNode)definition.path("sessionLocks");
                for(int i=locks.size()-1;i>=0;i--)if(id.equals(locks.get(i).path("sessionId").asString())) {
                    ObjectNode target=(ObjectNode)locks.get(i);
                    if("ATTEMPT_SCOPED".equals(lock.path("periodLockOrigin").asString()))target.remove("periodId");
                    if("ATTEMPT_SCOPED".equals(lock.path("roomLockOrigin").asString()))target.remove("roomId");
                    if(target.size()==1)locks.remove(i);
                }
            } else for(JsonNode lesson:definition.path("lessons"))if(id.equals(lesson.path("id").asString())) {
                if("ATTEMPT_SCOPED".equals(lock.path("periodLockOrigin").asString()))((ObjectNode)lesson).remove("periodLock");
                if("ATTEMPT_SCOPED".equals(lock.path("roomLockOrigin").asString()))((ObjectNode)lesson).remove("roomLock");
            }
        }
    }
    static boolean automatic(JsonNode lesson) {return lesson!=null && "automatic".equals(lesson.path("teacherPolicy").path("mode").asString());}
}

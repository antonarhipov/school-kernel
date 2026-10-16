package org.schoolkernel.workspace;

import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Presence-aware native definition comparison, shared by saved authoring and verified proposal review. */
final class DefinitionComparison {
    private DefinitionComparison() {}
    static ArrayNode compare(ObjectMapper json,JsonNode before,JsonNode after){
        ArrayNode changes=json.createArrayNode();Set<String> collections=new HashSet<>(DefinitionRemovalService.COLLECTIONS.values());collections.add("softConstraintOverrides");
        Set<String> fields=new TreeSet<>(before.propertyNames());fields.addAll(after.propertyNames());
        for(String field:fields){
            if(!collections.contains(field)){add(changes,"School",after.path("schoolId").asString(""),field,before.path(field),after.path(field));continue;}
            String type=field.equals("softConstraintOverrides")?"Preference":DefinitionRemovalService.COLLECTIONS.entrySet().stream().filter(e->e.getValue().equals(field)).findFirst().orElseThrow().getKey(),key=type.equals("Preference")?"constraintId":"id";
            if(before.has(field)!=after.has(field))add(changes,"School",after.path("schoolId").asString(""),field+" presence",before.has(field)?json.getNodeFactory().booleanNode(true):json.missingNode(),after.has(field)?json.getNodeFactory().booleanNode(true):json.missingNode());
            Map<String,JsonNode> old=map(before.path(field),key),next=map(after.path(field),key);Set<String> ids=new TreeSet<>(old.keySet());ids.addAll(next.keySet());
            for(String id:ids){JsonNode a=old.get(id),b=next.get(id);if(a==null||b==null)add(changes,type,id,"",a==null?json.missingNode():a,b==null?json.missingNode():b);else{Set<String> attributes=new TreeSet<>(a.propertyNames());attributes.addAll(b.propertyNames());for(String attribute:attributes)if(!attribute.equals(key))add(changes,type,id,attribute,a.path(attribute),b.path(attribute));}}
        }
        return changes;
    }
    private static void add(ArrayNode changes,String type,String id,String field,JsonNode before,JsonNode after){if(before.equals(after))return;ObjectNode row=changes.addObject().put("ownerType",type).put("ownerId",id).put("field",field).put("beforePresent",!before.isMissingNode()).put("afterPresent",!after.isMissingNode()).put("category",before.isMissingNode()?"Added":after.isMissingNode()?type.equals("Lesson")?"Cancelled":"Removed":"Changed");if(!before.isMissingNode())row.set("before",before.deepCopy());if(!after.isMissingNode())row.set("after",after.deepCopy());}
    private static Map<String,JsonNode> map(JsonNode rows,String key){Map<String,JsonNode> result=new LinkedHashMap<>();for(JsonNode row:rows)result.put(row.path(key).asString(""),row);return result;}
}

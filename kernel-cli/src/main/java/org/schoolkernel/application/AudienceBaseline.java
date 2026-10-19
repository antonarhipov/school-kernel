package org.schoolkernel.application;

import java.util.*;
import org.schoolkernel.contract.*;
import org.schoolkernel.domain.*;
import org.schoolkernel.solver.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

/** A bounded, self-contained comparison snapshot; never recursively trusts historic scores. */
public final class AudienceBaseline {
    public record Outcome(AudienceSchedule model,List<ValidationError> errors) {}
    public static JsonNode context(JsonNode definition,String revision,JsonNode result){
        var context=JsonSupport.mapper().createObjectNode();context.set("definition",definition.deepCopy());context.put("definitionRevision",revision);context.put("timetableRevision",result.path("timetableRevision").asString());context.set("assignments",result.path("timetable").path("assignments").deepCopy());context.set("coordinationMatches",result.path("coordinationMatches").deepCopy());return context;
    }
    public static Outcome load(JsonNode context,AudienceSchedule current,DefinitionLoader loader){
        if(context==null)return new Outcome(null,List.of());
        var loaded=loader.load(context.path("definition"),DefinitionLoader.Mode.BASELINE);
        if(loaded instanceof DefinitionLoader.Rejected rejected)return new Outcome(null,rejected.report().errors());
        if(!(loaded instanceof DefinitionLoader.Accepted accepted)||!AudienceDefinition.enabled(accepted.document()))return failure("Comparison requires a supported exact audience baseline; use explicit initial generation for an incompatible source");
        if(!current.resources.schoolId().equals(accepted.dto().schoolId()))return failure("Comparison baseline names another school");
        if(!context.path("definitionRevision").asString().equals(accepted.revision()))return failure("Comparison definition revision differs from its exact snapshot");
        String revision=new RevisionService().timetableRevision(2,accepted.dto().schoolId(),accepted.revision(),(ArrayNode)context.path("assignments"));
        if(!revision.equals(context.path("timetableRevision").asString()))return failure("Comparison timetable revision differs from its exact baseline assignments");
        if(current.document.has("basedOnRevision")&&!current.document.path("basedOnRevision").asString().equals(accepted.revision()))return failure("Successor ancestry does not identify the comparison baseline definition");
        var model=new AudienceSchedule(accepted.document(),accepted.definition());model.assign(context.path("assignments"));if(!model.errors.isEmpty())return new Outcome(null,List.copyOf(model.errors));
        var evaluated=new AudienceReview().evaluate(model,context.path("coordinationMatches"));
        if(!evaluated.valid())return new Outcome(null,evaluated.conflicts().stream().map(c->new ValidationError("/repairContext",c.lessonIds(),"Baseline "+c.constraintId()+": "+c.examples())).toList());
        return new Outcome(model,List.of());
    }
    private static Outcome failure(String message){return new Outcome(null,List.of(new ValidationError("/repairContext",List.of(),message)));}
}

package org.schoolkernel.application;

import java.util.*;
import org.schoolkernel.contract.*;
import org.schoolkernel.domain.*;
import org.schoolkernel.solver.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Application boundary for exact initial audience generation and explicit-baseline repair. */
final class AudiencePlanning {
    private final DefinitionLoader loader;
    AudiencePlanning(DefinitionLoader loader){this.loader=loader;}
    CommandOutcome initial(DefinitionLoader.Accepted accepted,SolverAdapter.ExecutionControls controls,String correlation,long started){
        return search(accepted,controls,correlation,started,null,null);
    }
    CommandOutcome repair(DefinitionLoader.Accepted previous,JsonNode current,DefinitionLoader.Accepted updated,
            SolverAdapter.ExecutionControls controls,String correlation,long started){
        if(!AudienceDefinition.enabled(previous.document())||!AudienceDefinition.enabled(updated.document()))return invalid(correlation,started,List.of(new ValidationError("/schemaVersion",List.of(),"Repair needs faithfully supported audience versions on both sides; use explicit adoption/initial generation for an incompatible legacy pair")));
        var validator=new AudiencePairValidator();var errors=validator.envelope(previous.document(),previous.revision(),current);if(!errors.isEmpty())return invalid(correlation,started,errors);
        if(!updated.document().path("basedOnRevision").asString().equals(previous.revision()))return invalid(correlation,started,List.of(new ValidationError("/basedOnRevision",List.of(),"Successor must identify the exact accepted baseline definition revision")));
        var baseline=new AudienceSchedule(previous.document(),previous.definition());baseline.assign(current.path("timetable").path("assignments"));if(!baseline.errors.isEmpty())return invalid(correlation,started,baseline.errors);
        try{
            if(current.has("repairContext")){var historic=AudienceBaseline.load(current.path("repairContext"),baseline,loader);if(!historic.errors().isEmpty())return invalid(correlation,started,historic.errors());}
            var evaluation=new AudienceReview().evaluate(baseline,current.path("coordinationMatches"));
            if(!evaluation.valid())return invalid(correlation,started,evaluation.conflicts().stream().map(c->new ValidationError("/current",c.lessonIds(),"Baseline "+c.constraintId()+": "+c.examples())).toList());
            var computed=new AudienceResultFactory().feasible(baseline,evaluation,previous.revision(),correlation,elapsed(started),"MANUAL_REVIEW");errors=validator.allocations(current,computed);if(!errors.isEmpty())return invalid(correlation,started,errors);
            JsonNode context=AudienceBaseline.context(previous.document(),previous.revision(),current);
            return search(updated,controls,correlation,started,context,baseline);
        }catch(AudienceCoordination.ReviewLimit|ArithmeticException limit){return unestablished(correlation,started,limit);}
    }
    private CommandOutcome search(DefinitionLoader.Accepted accepted,SolverAdapter.ExecutionControls controls,String correlation,long started,JsonNode context,AudienceSchedule baseline){
        var model=new AudienceSchedule(accepted.document(),accepted.definition());if(!model.errors.isEmpty())return invalid(correlation,started,model.errors);
        if(context!=null){var loaded=AudienceBaseline.load(context,model,loader);if(!loaded.errors().isEmpty())return invalid(correlation,started,loaded.errors());}
        try{
            var solved=new AudienceSolver(model,baseline,controls).solve();
            if(Thread.currentThread().isInterrupted())return new CommandOutcome.Interrupted();
            if(solved.assignments()==null){
                var diagnostics=solved.diagnostics().stream().map(c->new ConstraintDiagnostic(c.constraintId(),c.matchCount(),c.examples())).toList();
                var result=new ResultFactory().noFeasibleSolution(correlation,elapsed(started),accepted.definition(),accepted.revision(),controls,solved.terminationReason(),diagnostics);result.put("schemaVersion",2).put("catalogVersion",11);
                result.put("safeMessage",solved.searchExhausted()?"Complete finite search established that these hard requirements have no valid timetable.":"No independently valid complete timetable was found within the available search/review budget. Infeasibility is not established.");
                metadata(result,controls,solved);return new CommandOutcome.NoFeasibleSolution(result);
            }
            // Build a fresh model and independently review the exact selected candidate again.
            var checked=new AudienceSchedule(accepted.document(),accepted.definition());checked.assign(solved.assignments());
            if(!checked.errors.isEmpty())return invalid(correlation,started,checked.errors);
            var evaluation=new AudienceReview().evaluate(checked);
            if(Thread.currentThread().isInterrupted())return new CommandOutcome.Interrupted();
            if(!evaluation.valid())return invalid(correlation,started,evaluation.conflicts().stream().map(c->new ValidationError("/timetable",c.lessonIds(),"Independent post-search review: "+c.constraintId()+": "+c.examples())).toList());
            var comparison=baseline==null?null:new AudienceComparison().compare(baseline,checked);
            var result=new AudienceResultFactory().feasible(checked,evaluation,accepted.revision(),correlation,elapsed(started),solved.terminationReason(),context,comparison);metadata(result,controls,solved);
            var schemaErrors=new AudienceResultSchemaValidator().validate(result);if(!schemaErrors.isEmpty())throw new IllegalStateException("Audience search output violated its public contract: "+schemaErrors);
            return new CommandOutcome.Succeeded(result);
        }catch(java.util.concurrent.CancellationException cancelled){return new CommandOutcome.Interrupted();}
        catch(AudienceCoordination.ReviewLimit|ArithmeticException limit){return unestablished(correlation,started,limit);}
    }
    private static void metadata(ObjectNode result,SolverAdapter.ExecutionControls controls,AudienceSolver.Outcome solved){
        result.put("seed",controls.seed());var limit=result.putObject("limit");if(controls.timeLimit()!=null)limit.put("type","TIME").put("duration",controls.timeLimit().toString());else limit.put("type","STEP").put("steps",controls.stepLimit());
        var weights=(ObjectNode)result.path("effectiveSoftWeights");weights.put("soft.parallel-coverage",1).put("soft.cohort-gap",1);
        result.putObject("searchOutcome").put("visitedNodes",solved.visitedNodes()).put("completeCandidates",solved.completeCandidates()).put("reviewedCandidates",solved.reviewedCandidates()).put("unestablishedReviews",solved.unestablishedReviews()).put("searchExhausted",solved.searchExhausted()).put("globalOptimalityClaimed",false);
    }
    private static CommandOutcome invalid(String correlation,long started,List<ValidationError> errors){return new CommandOutcome.InvalidInput(new VerificationResultFactory().invalid(correlation,elapsed(started),ValidationReport.from(errors)));}
    private static CommandOutcome unestablished(String correlation,long started,RuntimeException limit){return invalid(correlation,started,List.of(new ValidationError("/timetable",List.of(),"Validity is unestablished: "+limit.getMessage())));}
    private static long elapsed(long started){return Math.max(0,(System.nanoTime()-started)/1_000_000);}
}

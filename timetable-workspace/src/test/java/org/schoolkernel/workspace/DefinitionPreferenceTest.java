package org.schoolkernel.workspace;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
class DefinitionPreferenceTest {
    @Test void kernelPopulationCoefficientsAndExactSignedLongBoundary(){
        var weights=new java.util.HashMap<String,Long>();DefinitionPreferenceService.SINCE.keySet().forEach(id->weights.put(id,1L));
        assertEquals(2*8+3*8+27*5,DefinitionPreferenceService.maximumPenalty(2,3,8,5,weights));
        assertEquals(Long.MAX_VALUE,DefinitionPreferenceService.maximumPenalty(Long.MAX_VALUE,0,1,0,Map.of("soft.teacher-gap",1L)));
        assertThrows(ArithmeticException.class,()->DefinitionPreferenceService.maximumPenalty(Long.MAX_VALUE,0,1,1,Map.of("soft.teacher-gap",1L,"soft.series-same-day",1L)));
        assertThrows(ArithmeticException.class,()->DefinitionPreferenceService.maximumPenalty(2147483647L,0,2147483647L,0,Map.of("soft.teacher-gap",1000000L)));
    }
}

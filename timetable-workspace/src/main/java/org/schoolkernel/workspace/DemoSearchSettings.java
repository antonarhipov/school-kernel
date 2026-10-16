package org.schoolkernel.workspace;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
record DemoSearchSettings(int quickSeconds, int thoroughSeconds, int lessonThreshold,
        int maxWaiting, Duration estimatedStartLimit, Duration abandonment, Duration processOverhead) {
    DemoSearchSettings(@Value("${demo.search.quick-seconds:5}") int quickSeconds,
            @Value("${demo.search.thorough-seconds:60}") int thoroughSeconds,
            @Value("${demo.search.lesson-threshold:200}") int lessonThreshold,
            @Value("${demo.queue.max-waiting:8}") int maxWaiting,
            @Value("${demo.queue.estimated-start-limit:2m}") Duration estimatedStartLimit,
            @Value("${demo.queue.abandonment:2m}") Duration abandonment,
            @Value("${demo.kernel.solve-overhead:10s}") Duration processOverhead) {
        if (quickSeconds < 1 || quickSeconds > thoroughSeconds || thoroughSeconds > 120 || lessonThreshold < 0
                || maxWaiting < 0 || maxWaiting > 8 || estimatedStartLimit.isNegative() || estimatedStartLimit.isZero()
                || abandonment.isNegative() || abandonment.isZero() || processOverhead.compareTo(Duration.ofSeconds(1)) < 0)
            throw new IllegalArgumentException("Invalid demo search bounds");
        this.quickSeconds = quickSeconds; this.thoroughSeconds = thoroughSeconds; this.lessonThreshold = lessonThreshold;
        this.maxWaiting = maxWaiting; this.estimatedStartLimit = estimatedStartLimit; this.abandonment = abandonment;
        this.processOverhead = processOverhead;
    }
    Duration effort(String preset) { return Duration.ofSeconds("THOROUGH".equals(preset) ? thoroughSeconds : quickSeconds); }
}

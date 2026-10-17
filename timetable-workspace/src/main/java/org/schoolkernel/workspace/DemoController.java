package org.schoolkernel.workspace;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
class DemoController {
    private final DemoWorkspaces workspaces;
    private final DemoSettings settings;
    @org.springframework.beans.factory.annotation.Value("${demo.search.lesson-threshold:200}") private int lessonThreshold;
    @org.springframework.beans.factory.annotation.Value("${demo.search.quick-seconds:5}") private int quickSeconds;
    @org.springframework.beans.factory.annotation.Value("${demo.search.thorough-seconds:60}") private int thoroughSeconds;
    @org.springframework.beans.factory.annotation.Value("${demo.reuse.enabled:true}") private boolean reuseEnabled;
    @org.springframework.beans.factory.annotation.Value("${demo.complexity.lesson-max:2500}") private int lessonMax;
    @org.springframework.beans.factory.annotation.Value("${demo.complexity.work-max:11000000}") private long workMax;
    DemoController(DemoWorkspaces workspaces, DemoSettings settings) {
        this.workspaces = workspaces;
        this.settings = settings;
    }
    @GetMapping("/healthz") Map<String, String> health() { return Map.of("status", "UP"); }
    @GetMapping("/api/demo/config") ResponseEntity<?> config() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "demo", true, "idleMinutes", settings.idleTimeout().toMinutes(),
                "absoluteHours", settings.absoluteTimeout().toHours(), "lessonThreshold", lessonThreshold,
                "quickSeconds", quickSeconds, "thoroughSeconds", thoroughSeconds, "reuseEnabled", reuseEnabled,
                "lessonMax", lessonMax, "workMax", workMax));
    }
    @PostMapping("/api/demo/session") ResponseEntity<?> restart(HttpServletRequest request) {
        var session = request.getSession(true);
        workspaces.restart(session);
        session.removeAttribute("demo.ended");
        request.changeSessionId();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("started", true));
    }
}

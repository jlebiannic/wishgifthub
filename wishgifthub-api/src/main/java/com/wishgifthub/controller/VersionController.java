package com.wishgifthub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur pour exposer les informations de version et de build de l'application
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final Optional<BuildProperties> buildProperties;
    private final Optional<GitProperties> gitProperties;

    private static final DateTimeFormatter READABLE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Autowired
    public VersionController(
            Optional<BuildProperties> buildProperties,
            Optional<GitProperties> gitProperties) {
        this.buildProperties = buildProperties;
        this.gitProperties = gitProperties;
    }

    /**
     * Retourne les informations de version et de build
     */
    @GetMapping
    public Map<String, Object> getVersion() {
        Map<String, Object> versionInfo = new HashMap<>();

        // Informations de build
        if (buildProperties.isPresent()) {
            BuildProperties build = buildProperties.get();
            versionInfo.put("version", build.getVersion());
            versionInfo.put("group", build.getGroup());
            versionInfo.put("artifact", build.getArtifact());
            versionInfo.put("name", build.getName());

            Instant buildTime = build.getTime();
            versionInfo.put("buildTimestamp", buildTime.toString());
            versionInfo.put("buildDate", READABLE_FORMATTER.format(buildTime));
        } else {
            versionInfo.put("version", "unknown");
            versionInfo.put("buildTimestamp", "unknown");
            versionInfo.put("buildDate", "unknown");
        }

        // Informations Git
        if (gitProperties.isPresent()) {
            GitProperties git = gitProperties.get();
            Map<String, String> gitInfo = new HashMap<>();
            gitInfo.put("branch", git.getBranch());
            gitInfo.put("commitId", git.getShortCommitId());
            gitInfo.put("commitIdFull", git.getCommitId());
            gitInfo.put("commitTime", git.getCommitTime() != null ? git.getCommitTime().toString() : "unknown");
            gitInfo.put("commitMessage", git.get("commit.message.short"));

            versionInfo.put("git", gitInfo);
        }

        return versionInfo;
    }
}


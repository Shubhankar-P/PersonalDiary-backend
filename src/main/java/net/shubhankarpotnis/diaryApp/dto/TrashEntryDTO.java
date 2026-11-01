package net.shubhankarpotnis.diaryApp.dto;

import java.time.OffsetDateTime;

public class TrashEntryDTO {

    private Long id;
    private String title;
    private String content;
    private OffsetDateTime deletedAt;
    private long daysRemaining;

    public TrashEntryDTO(Long id, String title, String content, OffsetDateTime deletedAt, long daysRemaining) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.deletedAt = deletedAt;
        this.daysRemaining = daysRemaining;
    }

    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public String getContent()           { return content; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public long getDaysRemaining()       { return daysRemaining; }
}
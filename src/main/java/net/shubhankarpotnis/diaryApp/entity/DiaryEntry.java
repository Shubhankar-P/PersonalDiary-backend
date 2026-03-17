package net.shubhankarpotnis.diaryApp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Document(collection = "diary_entries")
@Data
@NoArgsConstructor
public class DiaryEntry {

    @Id
    private ObjectId id;

    @NonNull
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 50000, message = "Content is too long")
    private String content;

    private LocalDateTime date;
}
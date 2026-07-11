package net.shubhankarpotnis.diaryApp.dto;

public class UserResponseDto {

    private Long id;
    private String userName;
    private String role;
    private int entryCount;

    public UserResponseDto(Long id, String userName, String role, int entryCount) {
        this.id = id;
        this.userName = userName;
        this.role = role;
        this.entryCount = entryCount;
    }

    public Long getId()       { return id; }
    public String getUserName() { return userName; }
    public String getRole()     { return role; }
    public int getEntryCount()  { return entryCount; }
}
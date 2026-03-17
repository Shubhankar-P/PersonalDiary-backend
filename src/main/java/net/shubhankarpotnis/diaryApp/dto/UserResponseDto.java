package net.shubhankarpotnis.diaryApp.dto;

import java.util.List;

public class UserResponseDto {

    private String id;
    private String userName;
    private List<String> roles;
    private int entryCount;

    public UserResponseDto(String id, String userName, List<String> roles, int entryCount) {
        this.id = id;
        this.userName = userName;
        this.roles = roles;
        this.entryCount = entryCount;
    }

    public String getId()          { return id; }
    public String getUserName()    { return userName; }
    public List<String> getRoles() { return roles; }
    public int getEntryCount()     { return entryCount; }
}
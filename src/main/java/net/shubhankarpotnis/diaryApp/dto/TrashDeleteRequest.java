package net.shubhankarpotnis.diaryApp.dto;

import java.util.List;

public class TrashDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
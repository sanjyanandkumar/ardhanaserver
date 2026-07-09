package com.ardhana.dto;

import java.util.List;

public class MyCourseResponse {
    private String courseId;
    private String title;
    private String name;
    private String description;
    private String level;
    private Integer sessions;
    private int completedLessons;
    private int totalLessons;
    private List<LessonProgressDetail> lessons;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getSessions() { return sessions; }
    public void setSessions(Integer sessions) { this.sessions = sessions; }
    public int getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(int completedLessons) { this.completedLessons = completedLessons; }
    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
    public List<LessonProgressDetail> getLessons() { return lessons; }
    public void setLessons(List<LessonProgressDetail> lessons) { this.lessons = lessons; }

    public static class LessonProgressDetail {
        private String lessonId;
        private String title;
        private int position;
        private boolean completed;
        private String note;

        public String getLessonId() { return lessonId; }
        public void setLessonId(String lessonId) { this.lessonId = lessonId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
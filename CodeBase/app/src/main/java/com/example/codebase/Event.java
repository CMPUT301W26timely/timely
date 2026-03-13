package com.example.codebase;

import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class Event implements Serializable {
    private String id;
    private EventPoster poster;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private Date registrationOpen;
    private Date registrationDeadline;
    private int waitlistCap;
    private float price;
    private String organizerDeviceId;
    private String location;
    private Date drawDate;
    private String posterUrl;
    private Long maxCapacity;
    private Long winnersCount;
    private ArrayList<String> waitingList;
    private ArrayList<String> selectedEntrants;
    private ArrayList<String> enrolledEntrants;
    private String status;
    private boolean geoEnabled;
    private ArrayList<String>  cancelledEntrants;

    public boolean isGeoEnabled() {
        return geoEnabled;
    }

    public void setGeoEnabled(boolean geoEnabled) {
        this.geoEnabled = geoEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizerDeviceId() {
        return organizerDeviceId;
    }

    public void setOrganizerDeviceId(String organizerDeviceId) {
        this.organizerDeviceId = organizerDeviceId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(Date drawDate) {
        this.drawDate = drawDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Long getWinnersCount() {
        return winnersCount;
    }

    public void setWinnersCount(Long winnersCount) {
        this.winnersCount = winnersCount;
    }

    public ArrayList<String> getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(ArrayList<String> waitingList) {
        this.waitingList = waitingList;
    }

    public ArrayList<String> getSelectedEntrants() {
        return selectedEntrants;
    }

    public void setSelectedEntrants(ArrayList<String> selectedEntrants) {
        this.selectedEntrants = selectedEntrants;
    }

    public ArrayList<String> getEnrolledEntrants() {
        return enrolledEntrants;
    }

    public void setEnrolledEntrants(ArrayList<String> enrolledEntrants) {
        this.enrolledEntrants = enrolledEntrants;
    }

    public ArrayList<String> getCancelledEntrants() {
        return cancelledEntrants;
    }

    public void setCancelledEntrants(ArrayList<String>  cancelledEntrants) {
        this.cancelledEntrants = cancelledEntrants;
    }

    public EventPoster getPoster() {
        return poster;
    }

    public void setPoster(EventPoster poster) {
        this.poster = poster;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(Date registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(Date registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public int getWaitlistCap() {
        return waitlistCap;
    }

    public void setWaitlistCap(int waitlistCap) {
        this.waitlistCap = waitlistCap;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}

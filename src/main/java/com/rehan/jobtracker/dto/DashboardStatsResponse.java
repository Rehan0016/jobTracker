package com.rehan.jobtracker.dto;

public class DashboardStatsResponse {

    private long totalApplications;
    private long applied;
    private long interviews;
    private long rejected;
    private long selected;
    private long offers;
    private long assessment;
    private long withdrawn;
    private double successRate;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalApplications, long applied, long interviews,
                                  long rejected, long selected, long offers) {
        this(totalApplications, applied, interviews, rejected, selected, offers, 0, 0,
                totalApplications > 0 ? Math.round(((double) (offers + selected) / totalApplications * 100.0) * 10.0) / 10.0 : 0.0);
    }

    public DashboardStatsResponse(long totalApplications, long applied, long interviews,
                                  long rejected, long selected, long offers,
                                  long assessment, long withdrawn, double successRate) {
        this.totalApplications = totalApplications;
        this.applied = applied;
        this.interviews = interviews;
        this.rejected = rejected;
        this.selected = selected;
        this.offers = offers;
        this.assessment = assessment;
        this.withdrawn = withdrawn;
        this.successRate = successRate;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getApplied() {
        return applied;
    }

    public void setApplied(long applied) {
        this.applied = applied;
    }

    public long getInterviews() {
        return interviews;
    }

    public void setInterviews(long interviews) {
        this.interviews = interviews;
    }

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }

    public long getSelected() {
        return selected;
    }

    public void setSelected(long selected) {
        this.selected = selected;
    }

    public long getOffers() {
        return offers;
    }

    public void setOffers(long offers) {
        this.offers = offers;
    }

    public long getAssessment() {
        return assessment;
    }

    public void setAssessment(long assessment) {
        this.assessment = assessment;
    }

    public long getWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(long withdrawn) {
        this.withdrawn = withdrawn;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}

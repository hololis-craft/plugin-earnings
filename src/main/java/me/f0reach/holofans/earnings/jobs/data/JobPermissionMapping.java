package me.f0reach.holofans.earnings.jobs.data;

public class JobPermissionMapping {
    private final String jobName;
    private final String permission;

    public JobPermissionMapping(String jobName, String permission) {
        this.jobName = jobName;
        this.permission = permission;
    }

    public String getJobName() {
        return jobName;
    }

    public String getPermission() {
        return permission;
    }
}
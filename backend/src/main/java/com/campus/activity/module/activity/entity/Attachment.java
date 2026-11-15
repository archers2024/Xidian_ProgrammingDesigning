package com.campus.activity.module.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 活动附件 t_attachment（Owner：B岗）
 */

@TableName("t_attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long attId;

    private Long activityId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private LocalDateTime uploadTime;

    @TableLogic
    private Integer deleted;

    public Long getAttId() {
        return attId;
    }

    public void setAttId(Long attId) {
        this.attId = attId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}

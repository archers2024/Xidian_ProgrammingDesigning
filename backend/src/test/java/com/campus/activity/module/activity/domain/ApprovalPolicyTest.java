package com.campus.activity.module.activity.domain;

import com.campus.activity.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalPolicyTest {
    @ParameterizedTest
    @CsvSource({"NONE,true,false", "PENDING,true,false", "APPROVED,true,true",
            "REJECTED,true,false", "NONE,false,true", "PENDING,false,false", "APPROVED,false,true"})
    void publishRespectsApprovalEvenWhenPolicyAllowsDirectPublish(String approval, boolean required, boolean allowed) {
        Runnable action = () -> ApprovalPolicy.requirePublishable(approval, required);
        if (allowed) assertDoesNotThrow(action::run);
        else assertThrows(BusinessException.class, action::run);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,NONE,true", "DRAFT,REJECTED,true", "DRAFT,PENDING,false",
            "DRAFT,APPROVED,false", "OPEN,NONE,false", "CANCELED,REJECTED,false"})
    void onlyUnreviewedOrRejectedDraftsCanBeSubmitted(String status, String approval, boolean allowed) {
        Runnable action = () -> ApprovalPolicy.requireSubmittable(status, approval);
        if (allowed) assertDoesNotThrow(action::run);
        else assertThrows(BusinessException.class, action::run);
    }

    @Test
    void submittingAdministratorCannotApproveOwnRequest() {
        assertThrows(BusinessException.class, () -> ApprovalPolicy.requireDecidable("DRAFT", "PENDING", "PENDING", 1L, 1L));
        assertDoesNotThrow(() -> ApprovalPolicy.requireDecidable("DRAFT", "PENDING", "PENDING", 1L, 2L));
    }

    @Test
    void staleTicketCannotApproveResubmittedActivity() {
        assertThrows(BusinessException.class, () -> ApprovalPolicy.requireDecidable("DRAFT", "PENDING", "REJECTED", 1L, 2L));
    }
}

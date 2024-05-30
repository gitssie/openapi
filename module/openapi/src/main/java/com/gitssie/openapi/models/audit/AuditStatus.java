package com.gitssie.openapi.models.audit;

import com.gitssie.openapi.data.Code;
import io.vavr.control.Either;

public enum AuditStatus {
    PENDING, PROCESSING, APPROVED, REJECTED, REVIEW; //待提交、审核中、已审核、已驳回、重新审核

    public static Either<Code, AuditStatus> of(int code) {
        if (code < 0 | code >= AuditStatus.values().length) {
            return Either.left(Code.NOT_FOUND.withMessage("审核状态不存在"));
        }
        return Either.right(AuditStatus.values()[code]);
    }

    public boolean canBeProcessing() {
        switch (this) {
            case PENDING:
            case REJECTED:
            case REVIEW:
                return true;
            default:
                return false;
        }
    }

    public boolean canBeApproved() {
        switch (this) {
            case PROCESSING:
                return true;
            default:
                return false;
        }
    }

    public boolean canBeReview(){
        switch (this){
            case APPROVED:
                return true;
            default:
                return false;
        }
    }
}
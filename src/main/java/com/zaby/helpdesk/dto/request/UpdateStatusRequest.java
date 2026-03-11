package com.zaby.helpdesk.dto.request;

import com.zaby.helpdesk.enumeration.Status;

public record UpdateStatusRequest(
        Status status,
        String remark
) {
}

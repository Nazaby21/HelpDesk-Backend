package com.zaby.helpdesk.dto.response;

import java.util.List;

public record MonthlyChartResponse(
        List<Long> received,
        List<Long> due
) {}

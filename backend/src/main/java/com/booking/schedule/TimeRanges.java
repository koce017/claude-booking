package com.booking.schedule;

import java.util.List;

import com.booking.common.error.BadRequestException;

final class TimeRanges {

    private TimeRanges() {
    }

    static void requireNoOverlap(List<TimeRange> periods) {
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                if (periods.get(i).overlaps(periods.get(j))) {
                    throw new BadRequestException("OVERLAPPING_PERIODS", "Working periods must not overlap");
                }
            }
        }
    }
}

package com.exam.system.exception;

public final class ReservationErrorCode {

    private ReservationErrorCode() {}

    public static final String RSV_NO_RESERVATION = "RSV_NO_RESERVATION";
    public static final String RSV_SESSION_NOT_STARTED = "RSV_SESSION_NOT_STARTED";
    public static final String RSV_SESSION_ENDED = "RSV_SESSION_ENDED";
    public static final String RSV_RESERVATION_CANCELLED = "RSV_RESERVATION_CANCELLED";
    public static final String RSV_LATE_GRACE_EXCEEDED = "RSV_LATE_GRACE_EXCEEDED";
    public static final String RSV_SESSION_MISMATCH = "RSV_SESSION_MISMATCH";
    public static final String RSV_EXAM_MISMATCH = "RSV_EXAM_MISMATCH";

    public static final String RSV_CAPACITY_FULL = "RSV_CAPACITY_FULL";
    public static final String RSV_DUPLICATE_BOOKING = "RSV_DUPLICATE_BOOKING";
    public static final String RSV_BELOW_LEAD_TIME = "RSV_BELOW_LEAD_TIME";
    public static final String RSV_SESSION_NOT_OPEN = "RSV_SESSION_NOT_OPEN";
    public static final String RSV_SESSION_NOT_FOUND = "RSV_SESSION_NOT_FOUND";
    public static final String RSV_EXAM_NOT_FOUND = "RSV_EXAM_NOT_FOUND";
    public static final String RSV_SESSION_HAS_BOOKINGS = "RSV_SESSION_HAS_BOOKINGS";
    public static final String RSV_INVALID_CAPACITY = "RSV_INVALID_CAPACITY";
    public static final String RSV_SUBMISSION_TIMEOUT = "RSV_SUBMISSION_TIMEOUT";
    public static final String RSV_SCREEN_SWITCH_FORCED = "RSV_SCREEN_SWITCH_FORCED";
}

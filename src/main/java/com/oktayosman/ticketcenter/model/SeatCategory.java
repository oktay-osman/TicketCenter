package com.oktayosman.ticketcenter.model;

public enum SeatCategory {
    VIP("VIP Seats"),
    NORMAL("Normal Seats"),
    EARLY_BIRD("Early Bird");

    private final String displayName;

    SeatCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


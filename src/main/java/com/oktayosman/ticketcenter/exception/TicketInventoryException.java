package com.oktayosman.ticketcenter.exception;

/**
 * Thrown when a ticket sale cannot be completed because it would exceed the
 * available inventory (per seat type or for the event as a whole).
 * The message is written for the end user and is shown as-is in the UI.
 */
public class TicketInventoryException extends RuntimeException {

    public TicketInventoryException(String message) {
        super(message);
    }
}

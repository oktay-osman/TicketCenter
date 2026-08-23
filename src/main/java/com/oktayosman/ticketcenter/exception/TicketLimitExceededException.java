package com.oktayosman.ticketcenter.exception;

/**
 * Thrown when a buyer would exceed the per-person ticket limit configured on the event.
 * The message is written for the end user and is shown as-is in the UI.
 */
public class TicketLimitExceededException extends TicketInventoryException {

    public TicketLimitExceededException(String message) {
        super(message);
    }
}

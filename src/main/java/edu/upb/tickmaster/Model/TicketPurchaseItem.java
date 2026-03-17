package edu.upb.tickmaster.Model;

public class PurchaseTicket {
    private int ticket_type_id;
    private int quantity;

    public int getTicket_type_id() {
        return ticket_type_id;
    }

    public void setTicket_type_id(int ticket_type_id) {
        this.ticket_type_id = ticket_type_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
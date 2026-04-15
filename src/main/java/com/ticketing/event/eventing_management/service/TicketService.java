package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;

public interface TicketService {
    String processTicketPurchase(WooCommerceOrderDTO order) throws Exception;
}

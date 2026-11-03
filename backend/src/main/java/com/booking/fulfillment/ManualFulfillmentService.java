package com.booking.fulfillment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.booking.appointment.Appointment;

/**
 * Administrators hand approved appointments over to the company themselves, so
 * nothing is sent automatically. Active with booking.fulfillment.mode=manual.
 */
@Service
@ConditionalOnProperty(name = "booking.fulfillment.mode", havingValue = "manual")
public class ManualFulfillmentService implements AppointmentFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(ManualFulfillmentService.class);

    @Override
    public boolean fulfill(Appointment appointment) {
        log.info("Appointment {} for company {} approved; awaiting manual fulfillment by an administrator",
                appointment.getId(), appointment.getCompany().getId());
        return false;
    }
}

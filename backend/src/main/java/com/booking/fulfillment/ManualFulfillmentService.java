package com.booking.fulfillment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.booking.appointment.Appointment;

/**
 * Initial implementation: administrators hand approved appointments over to the
 * company themselves, so nothing is sent automatically. Replace this bean with an
 * email/webhook/API implementation when that is decided.
 */
@Service
public class ManualFulfillmentService implements AppointmentFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(ManualFulfillmentService.class);

    @Override
    public void fulfill(Appointment appointment) {
        log.info("Appointment {} for company {} approved; awaiting manual fulfillment by an administrator",
                appointment.getId(), appointment.getCompany().getId());
    }
}

package com.booking.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.booking.mail.MailGateway;
import com.booking.mail.OutgoingMail;

/** Records emails instead of sending them. */
public class CapturingMailGateway implements MailGateway {

    private final List<OutgoingMail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(OutgoingMail mail) {
        sent.add(mail);
    }

    public List<OutgoingMail> sent() {
        return sent;
    }

    public List<OutgoingMail> sentTo(String address) {
        return sent.stream().filter(m -> m.to().equals(address)).toList();
    }

    public void clear() {
        sent.clear();
    }
}

package com.booking.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.booking.notification.MagicLinkSender;

public class CapturingMagicLinkSender implements MagicLinkSender {

    public record SentLink(String email, String link) {

        public String token() {
            return link.substring(link.indexOf("token=") + "token=".length());
        }
    }

    private final List<SentLink> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String email, String link) {
        sent.add(new SentLink(email, link));
    }

    public List<SentLink> sent() {
        return sent;
    }

    public SentLink last() {
        return sent.get(sent.size() - 1);
    }

    public void clear() {
        sent.clear();
    }
}

package com.example.notification.router;

import com.example.notification.sender.NotificationSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class NotificationRouterTest {

    @Test
    void notifyAll_callsAllSenders() {
        NotificationSender sender1 = mock(NotificationSender.class);
        NotificationSender sender2 = mock(NotificationSender.class);
        when(sender1.getType()).thenReturn("log");
        when(sender2.getType()).thenReturn("telegram");

        NotificationRouter router = new NotificationRouter(List.of(sender1, sender2));
        router.notifyAll("test message");

        verify(sender1).send("test message");
        verify(sender2).send("test message");
    }

    @Test
    void notifyAll_oneSenderFails_otherStillCalled() {
        NotificationSender failing = mock(NotificationSender.class);
        NotificationSender working = mock(NotificationSender.class);
        when(failing.getType()).thenReturn("failing");
        when(working.getType()).thenReturn("working");
        doThrow(new RuntimeException("boom")).when(failing).send(any());

        NotificationRouter router = new NotificationRouter(List.of(failing, working));
        router.notifyAll("test message");

        verify(failing).send("test message");
        verify(working).send("test message");
    }

    @Test
    void notifyAll_noSenders_doesNotThrow() {
        NotificationRouter router = new NotificationRouter(List.of());
        router.notifyAll("test message");
        // no exception = pass
    }
}
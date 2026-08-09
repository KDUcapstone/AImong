self.addEventListener("push", event => {
    let payload = {};
    try {
        payload = event.data ? event.data.json() : {};
    } catch {
        payload = {};
    }

    const notification = payload.notification || {};
    const data = payload.data || {};
    const title = notification.title || data.title || "AImong";
    const body = notification.body || data.body || "";

    event.waitUntil(self.registration.showNotification(title, {
        body,
        data
    }));
});

self.addEventListener("notificationclick", event => {
    event.notification.close();
    event.waitUntil(self.clients.matchAll({ type: "window", includeUncontrolled: true })
        .then(clients => {
            if (clients.length > 0) {
                return clients[0].focus();
            }
            return self.clients.openWindow("./test.html");
        }));
});

/* ───────────────────────────────────────────────────────────────────────────
 * VegGo Fresh — Notification client (Web)
 *
 * Reference implementation for the Notification module (backend items 1–5).
 * Uses SockJS (auto-fallback) + the @stomp/stompjs client, connecting to the
 * SAME /ws endpoint the Flutter raw-STOMP client uses.
 *
 *   • subscribe to /user/queue/notifications for real-time delivery
 *   • REST fallback (GET /api/notifications …) to hydrate + reset badge
 *   • socket auth via ?token=<jwt> query param (SockJS can't set headers)
 *
 * Install:  npm i @stomp/stompjs sockjs-client
 * ─────────────────────────────────────────────────────────────────────────── */

/** Single notification, mirroring backend NotificationDto. */
class NotificationItem {
  constructor({ id, recipientId, recipientRole, type, title, body, data, read, createdAt }) {
    this.id = id;
    this.recipientId = recipientId;
    this.recipientRole = recipientRole;
    this.type = type;
    this.title = title;
    this.body = body;
    this.data = data;
    this.read = read;
    this.createdAt = createdAt;
  }

  static fromJson(json) {
    return new NotificationItem({
      id: json.id,
      recipientId: json.recipientId,
      recipientRole: json.recipientRole,
      type: json.type,
      title: json.title,
      body: json.body,
      data: json.data,
      read: json.read,
      createdAt: json.createdAt,
    });
  }
}

export class NotificationService {
  /**
   * @param {string} backendBaseUrl  e.g. 'https://api.veggofresh.in' (no slash)
   * @param {() => Promise<string>} authTokenProvider returns the access token
   * @param {(n: NotificationItem) => void} [onNotification] real-time callback
   */
  constructor({ backendBaseUrl, authTokenProvider, onNotification }) {
    this.baseUrl = backendBaseUrl.replace(/\/$/, '');
    this.authTokenProvider = authTokenProvider;
    this.onNotification = onNotification;
    this.stompClient = null;
    this.connected = false;
    this.unreadCount = 0;
    this.notifications = [];
    this._page = 0;
    this._hasMore = true;
  }

  /* ── REST hydrate (source of truth, socket-independent) ─────────────── */

  async _authedFetch(path, options = {}) {
    const token = await this.authTokenProvider();
    const headers = { ...(options.headers || {}), Authorization: `Bearer ${token}` };
    const res = await fetch(`${this.baseUrl}${path}`, { ...options, headers });
    if (!res.ok) throw new Error(`Notification API ${res.status}`);
    return res.json();
  }

  async fetchNotifications({ refresh = true } = {}) {
    if (refresh) {
      this._page = 0;
      this._hasMore = true;
      this.notifications = [];
    }
    if (!this._hasMore) return [];
    const body = await this._authedFetch(
      `/api/notifications?page=${this._page}&size=20`
    );
    const page = body.data;
    const items = (page.content || []).map(NotificationItem.fromJson);
    this.notifications.push(...items);
    this._page += 1;
    this._hasMore = this._page < page.totalPages;
    return items;
  }

  async fetchUnreadCount() {
    const body = await this._authedFetch('/api/notifications/unread-count');
    this.unreadCount = body.data.unreadCount;
    return this.unreadCount;
  }

  async markRead(id) {
    await this._authedFetch(`/api/notifications/${id}/read`, { method: 'PUT' });
    const n = this.notifications.find((x) => x.id === id);
    if (n && !n.read) {
      n.read = true;
      if (this.unreadCount > 0) this.unreadCount -= 1;
    }
  }

  async markAllRead() {
    await this._authedFetch('/api/notifications/read-all', { method: 'PUT' });
    this.unreadCount = 0;
    this.notifications.forEach((n) => { n.read = true; });
  }

  /* ── Real-time (SockJS → /user/queue/notifications) ─────────────────── */

  async connect() {
    if (this.stompClient && this.connected) return;
    const [{ Client }, SockJS] = await Promise.all([
      import('@stomp/stompjs'),
      import('sockjs-client'),
    ]);

    const token = await this.authTokenProvider();
    const socket = new SockJS(`${this.baseUrl}/ws?token=${encodeURIComponent(token)}`);

    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        this.stompClient.subscribe('/user/queue/notifications', (frame) => {
          try {
            const notification = NotificationItem.fromJson(JSON.parse(frame.body));
            this.notifications.unshift(notification);
            if (!notification.read) this.unreadCount += 1;
            if (this.onNotification) this.onNotification(notification);
          } catch (e) {
            console.warn('Bad notification frame', e);
          }
        });
      },
      onDisconnect: () => { this.connected = false; },
      onStompError: (frame) => console.error('STOMP error', frame.headers.message),
    });
    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected = false;
    }
  }

  /** Re-hydrate list + badge from REST (app resume / post-push refresh). */
  async sync() {
    await this.fetchNotifications({ refresh: true });
    await this.fetchUnreadCount();
  }
}
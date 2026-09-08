// ============================================================================
// VegGo Fresh — Notification client (Flutter)
//
// Reference implementation for the Notification module (backend items 1–5).
// Uses:
//   • REST (dio) for initial hydrate + badge count (source of truth if the
//     socket was disconnected / app was killed).
//   • stomp_dart_client for real-time STOMP-over-WebSocket delivery from
//     /user/queue/notifications (raw WS, same /ws endpoint as SockJS uses).
//
// pubspec deps:
//   dio: ^5.x
//   stomp_dart_client: ^1.x
//
// Connection auth uses the same Bearer JWT the REST api uses, passed as a
// query param (?token=...) because raw STOMP-over-WS clients can't always set
// a handshake Authorization header. The backend's WebSocketAuthInterceptor
// accepts both forms.
//
// IMPORTANT (item 8): this covers ONLY in-app / foreground delivery. A
// background-or-killed-app "buzz" needs an OS push channel (Firebase Cloud
// Messaging is the standard, free option for Flutter). It is deliberately NOT
// implemented here — asked before adding.
// ============================================================================

import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

/// Wire DTO mirroring the backend `NotificationDto`.
class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.recipientId,
    required this.recipientRole,
    required this.type,
    required this.title,
    this.body,
    this.data,
    this.read = false,
    this.createdAt,
  });

  final String id;
  final String recipientId;
  final String recipientRole;
  final String type;
  final String title;
  final String? body;
  final String? data;
  final bool read;
  final DateTime? createdAt;

  factory NotificationItem.fromJson(Map<String, dynamic> json) => NotificationItem(
        id: json['id'] as String,
        recipientId: json['recipientId'] as String,
        recipientRole: json['recipientRole'] as String,
        type: json['type'] as String,
        title: json['title'] as String,
        body: json['body'] as String?,
        data: json['data'] as String?,
        read: json['read'] as bool? ?? false,
        createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String) : null,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'recipientId': recipientId,
        'recipientRole': recipientRole,
        'type': type,
        'title': title,
        'body': body,
        'data': data,
        'read': read,
        'createdAt': createdAt?.toIso8601String(),
      };
}

typedef OnNotification = void Function(NotificationItem notification);

class NotificationService extends ChangeNotifier {
  NotificationService({
    required this.baseUrl,
    required this.authTokenProvider,
    this.onNotification,
    this.connectOnInit = true,
  });

  /// e.g. `https://api.veggofresh.in` (backend base URL, no trailing slash).
  final String baseUrl;

  /// Returns the current valid access token when called.
  final FutureOr<String> Function() authTokenProvider;

  final OnNotification? onNotification;

  final Dio _dio = Dio();
  StompClient? _stompClient;
  bool _connected = false;

  final List<NotificationItem> _notifications = [];
  int _unreadCount = 0;
  int _page = 0;
  bool _hasMore = true;
  bool _loading = false;

  bool get connected => _connected;
  int get unreadCount => _unreadCount;
  List<NotificationItem> get notifications => List.unmodifiable(_notifications);

  // ── REST: hydrate + badge + mark-read (source of truth) ────────────────

  Future<List<NotificationItem>> fetchNotifications({bool refresh = false}) async {
    if (_loading) return const [];
    _loading = true;
    if (refresh) {
      _page = 0;
      _hasMore = true;
      _notifications.clear();
    }
    if (!_hasMore) {
      _loading = false;
      return const [];
    }

    final token = await authTokenProvider();
    final res = await _dio.get(
      '$baseUrl/api/notifications',
      queryParameters: {'page': _page, 'size': 20},
      options: Options(headers: {'Authorization': 'Bearer $token'}),
    );

    final data = (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>;
    final content = (data['content'] as List<dynamic>?)
            ?.map((e) => NotificationItem.fromJson(e as Map<String, dynamic>))
            .toList() ??
        const <NotificationItem>[];

    _notifications.addAll(content);
    _page += 1;
    _hasMore = _page < (data['totalPages'] as int? ?? 0);
    _loading = false;
    notifyListeners();
    return content;
  }

  Future<int> fetchUnreadCount() async {
    final token = await authTokenProvider();
    final res = await _dio.get(
      '$baseUrl/api/notifications/unread-count',
      options: Options(headers: {'Authorization': 'Bearer $token'}),
    );
    final data = (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>;
    _unreadCount = (data['unreadCount'] as num?)?.toInt() ?? 0;
    notifyListeners();
    return _unreadCount;
  }

  Future<void> markRead(String notificationId) async {
    final token = await authTokenProvider();
    await _dio.put(
      '$baseUrl/api/notifications/$notificationId/read',
      options: Options(headers: {'Authorization': 'Bearer $token'}),
    );
    final idx = _notifications.indexWhere((n) => n.id == notificationId);
    if (idx >= 0 && !_notifications[idx].read) {
      _notifications[idx] = NotificationItem(
        id: _notifications[idx].id,
        recipientId: _notifications[idx].recipientId,
        recipientRole: _notifications[idx].recipientRole,
        type: _notifications[idx].type,
        title: _notifications[idx].title,
        body: _notifications[idx].body,
        data: _notifications[idx].data,
        read: true,
        createdAt: _notifications[idx].createdAt,
      );
      if (_unreadCount > 0) _unreadCount -= 1;
      notifyListeners();
    }
  }

  Future<int> markAllRead() async {
    final token = await authTokenProvider();
    final res = await _dio.put(
      '$baseUrl/api/notifications/read-all',
      options: Options(headers: {'Authorization': 'Bearer $token'}),
    );
    final data = (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>;
    _unreadCount = 0;
    notifyListeners();
    return (data['updated'] as num?)?.toInt() ?? 0;
  }

  // ── Real-time: STOMP-over-WebSocket (+/queue/notifications) ────────────

  Future<void> connect() async {
    if (_stompClient != null && _connected) return;
    final token = await authTokenProvider();
    final wsUrl = _wsUrl(token);

    _stompClient = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (frame) {
          _connected = true;
          _stompClient?.subscribe(
            destination: '/user/queue/notifications',
            callback: (frame) {
              if (frame.body == null) return;
              try {
                final notification = NotificationItem.fromJson(
                    jsonDecode(frame.body!) as Map<String, dynamic>);
                _handlePush(notification);
              } catch (e) {
                debugPrint('NotificationService: failed to parse push: $e');
              }
            },
          );
          notifyListeners();
        },
        onDisconnect: (frame) {
          _connected = false;
          notifyListeners();
        },
        // Will automatically reconnect when the framing heartbeat opens.
        onDebugMessage: (msg) => debugPrint('[stomp] $msg'),
      ),
    );
    _stompClient?.activate();
  }

  void disconnect() {
    _stompClient?.deactivate();
    _stompClient = null;
    _connected = false;
    notifyListeners();
  }

  /// Turn `https://host/base` → `wss://host/base/ws?token=...` (raw STOMP).
  String _wsUrl(String token) {
    final uri = Uri.parse(baseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
    return Uri(
      scheme: scheme,
      host: uri.host,
      port: uri.hasPort ? uri.port : null,
      path: '${uri.path.isEmpty ? '' : uri.path}/ws',
      queryParameters: {'token': token},
    ).toString();
  }

  void _handlePush(NotificationItem notification) {
    _notifications.insert(0, notification);
    if (!notification.read) _unreadCount += 1;
    onNotification?.call(notification);
    notifyListeners();
  }

  /// Refresh the inbox + badge from REST (call on app resume or when any
  /// screen depends on order/delivery/stock state and just got a push).
  Future<void> sync() async {
    await fetchNotifications(refresh: true);
    await fetchUnreadCount();
  }

  notifyRevertRead(int index) {
    final n = _notifications[index];
    _notifications[index] = NotificationItem(
      id: n.id, recipientId: n.recipientId, recipientRole: n.recipientRole,
      type: n.type, title: n.title, body: n.body, data: n.data, read: n.read,
      createdAt: n.createdAt,
    );
    notifyListeners();
  }

  @override
  void dispose() {
    disconnect();
    super.dispose();
  }
}
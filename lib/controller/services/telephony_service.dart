import 'dart:async';
import 'package:flutter_tele/flutter_tele.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/router/router.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'telephony_service.g.dart';

enum CallState {
  incoming,
  outgoing,
  connected,
  disconnected,
  muted,
  hold,
  unknown,
}

@Riverpod(keepAlive: true)
class TelephonyService extends _$TelephonyService {
  final TeleEndpoint _endpoint = TeleEndpoint();
  TeleCall? _call = null;

  bool _initialized = false;
  bool _isDefaultDialer = false;

  @override
  CallState build() {
    _initTelephony();
    return CallState.unknown;
  }

  Future<void> _initTelephony() async {
    if (_initialized) return;

    _isDefaultDialer = await TeleDialer.isDefaultDialer();

    if (!_isDefaultDialer) return;

    await _endpoint.start({'ReplaceDialer': true, 'Permissions': true});
    _registerCallEvents();
    _initialized = true;
  }

  void _registerCallEvents() {
    _endpoint.on('call_received').listen((event) {
      _call = TeleCall.fromMap(normalizeCallEvent(event));
      state = CallState.incoming;
      router.goNamed(AppRoutes.callScreenRoute);
    });

    _endpoint.on('call_changed').listen((event) {
      _call = TeleCall.fromMap(normalizeCallEvent(event));
      if (_call != null && _call!.state == 'ACTIVE') {}
    });

    _endpoint.on('call_terminated').listen((event) {
      _call = null;
      state = CallState.disconnected;
      router.goNamed(AppRoutes.homeRoute);
    });
  }

  bool isDefaultDialer() => _isDefaultDialer;
  TeleCall? getCall() => _call;
  CallState getCallState() => state;

  void acceptCall() {
    if (_call != null) _endpoint.answerCall(_call!);
  }

  void declineCall() {
    if (_call != null) _endpoint.declineCall(_call!);
  }
}

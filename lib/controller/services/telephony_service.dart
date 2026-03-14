import 'dart:async';
import 'package:flutter_tele/flutter_tele.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/controller/services/notification_service.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/main.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'telephony_service.g.dart';

@Riverpod(keepAlive: true)
class TelephonyService extends _$TelephonyService {
  final TeleEndpoint _endpoint = TeleEndpoint();
  TeleCall? _call = null;
  TeleCall? _lastCall = null;

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

    if (!_isDefaultDialer) {
      await TeleDialer.requestDefaultDialer();
    }

    await _endpoint.start({'ReplaceDialer': false, 'Permissions': false});
    _registerCallEvents();
    _initialized = true;
  }

  void _registerCallEvents() {
    _endpoint.on('call_received').listen((event) {
      _call = TeleCall.fromMap(event);
      _lastCall = _call;
      if (_call != null) {
        state = _call!.state;
      }
    });

    _endpoint.on('call_changed').listen((event) {
      _call = TeleCall.fromMap(event);
      _lastCall = _call;

      if (_call != null) {
        state = _call!.state;
      }
    });

    _endpoint.on('call_terminated').listen((event) {
      _call = null;
      state = CallState.disconnected;
    });
  }

  bool isDefaultDialer() => _isDefaultDialer;
  TeleCall? getCall() => _call;
  TeleCall? getLastCall() => _lastCall;
  CallState getCallState() => state;

  String getDuration() {
    final connectTime = _call?.connectTimeMillis;

    if (connectTime == null || connectTime <= 0) {
      return "00:00";
    }

    final startTime = DateTime.fromMillisecondsSinceEpoch(connectTime);
    final duration = DateTime.now().difference(startTime);

    return convertSecondsToHMS(duration.inSeconds);
  }

  void acceptCall() {
    if (_call != null) _endpoint.answer(_call!);
  }

  void useSpeaker() {
    if (_call != null) _endpoint.speaker(_call!);
  }

  void useEarpiece() {
    if (_call != null) _endpoint.earpiece(_call!);
  }

  void hold() {
    if (_call != null) _endpoint.hold(_call!);
  }

  void unhold() {
    if (_call != null) _endpoint.unhold(_call!);
  }

  void mute() {
    if (_call != null) _endpoint.mute(_call!);
  }

  void unmute() {
    if (_call != null) _endpoint.unmute(_call!);
  }

  void declineCall() {
    if (_call != null) {
      _endpoint.hangup(_call!);
      _endpoint.decline(_call!);
    }
  }

  Future<void> makeCall(int sim, String dest) async {
    if (_call != null) return;
    _call = await _endpoint.makeCall(sim, dest, null, null);
  }
}

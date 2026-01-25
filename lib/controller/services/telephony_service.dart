import 'dart:async';
import 'package:flutter_tele/flutter_tele.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/call_state.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'telephony_service.g.dart';

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

    await _endpoint.start({'ReplaceDialer': false, 'Permissions': false});
    _registerCallEvents();
    _initialized = true;
  }

  void _registerCallEvents() {
    _endpoint.on('call_received').listen((event) {
      _call = TeleCall.fromMap(normalizeCallEvent(event));
      if (_call != null) {
        state = CallStateParser.fromString(_call?.state ?? "INCOMING");
      }
    });

    _endpoint.on('call_changed').listen((event) {
      _call = TeleCall.fromMap(normalizeCallEvent(event));

      if (_call != null) {
        state = CallStateParser.fromString(_call?.state ?? "CONNECTED");
      }
    });

    _endpoint.on('call_terminated').listen((event) {
      _call = null;
      state = CallState.disconnected;
    });
  }

  bool isDefaultDialer() => _isDefaultDialer;
  TeleCall? getCall() => _call;
  CallState getCallState() => state;

  String getDuration() {
    return convertSecondsToHMS((DateTime.now().difference(
            DateTime.fromMillisecondsSinceEpoch(_call?.connectTimeMillis ?? 0)))
        .inMilliseconds);
  }

  void acceptCall() {
    if (_call != null) _endpoint.answerCall(_call!);
  }

  void declineCall() {
    if (_call != null) _endpoint.declineCall(_call!);
  }

  Future<void> makeCall(int sim, String dest) async {
    if (_call != null) return;
    _call = await _endpoint.makeCall(sim + 1, dest, null, null);
  }
}

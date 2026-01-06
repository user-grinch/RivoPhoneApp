// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'telephony_service.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(TelephonyService)
final telephonyServiceProvider = TelephonyServiceProvider._();

final class TelephonyServiceProvider
    extends $NotifierProvider<TelephonyService, CallState> {
  TelephonyServiceProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'telephonyServiceProvider',
          isAutoDispose: false,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$telephonyServiceHash();

  @$internal
  @override
  TelephonyService create() => TelephonyService();

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(CallState value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<CallState>(value),
    );
  }
}

String _$telephonyServiceHash() => r'012eba0e38f2f9de72ea6e997de80c141e9f805d';

abstract class _$TelephonyService extends $Notifier<CallState> {
  CallState build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<CallState, CallState>;
    final element = ref.element as $ClassProviderElement<
        AnyNotifier<CallState, CallState>, CallState, Object?, Object?>;
    element.handleCreate(ref, build);
  }
}

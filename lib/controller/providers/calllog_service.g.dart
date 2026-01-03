// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'calllog_service.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(CallLogService)
final callLogServiceProvider = CallLogServiceProvider._();

final class CallLogServiceProvider
    extends $AsyncNotifierProvider<CallLogService, CallLogList> {
  CallLogServiceProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'callLogServiceProvider',
          isAutoDispose: false,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$callLogServiceHash();

  @$internal
  @override
  CallLogService create() => CallLogService();
}

String _$callLogServiceHash() => r'412d5484fcabb987e598c35d0ae6a790e855eb19';

abstract class _$CallLogService extends $AsyncNotifier<CallLogList> {
  FutureOr<CallLogList> build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<AsyncValue<CallLogList>, CallLogList>;
    final element = ref.element as $ClassProviderElement<
        AnyNotifier<AsyncValue<CallLogList>, CallLogList>,
        AsyncValue<CallLogList>,
        Object?,
        Object?>;
    element.handleCreate(ref, build);
  }
}

@ProviderFor(SelectedCallTypeFilter)
final selectedCallTypeFilterProvider = SelectedCallTypeFilterProvider._();

final class SelectedCallTypeFilterProvider
    extends $NotifierProvider<SelectedCallTypeFilter, RevoCallType> {
  SelectedCallTypeFilterProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'selectedCallTypeFilterProvider',
          isAutoDispose: true,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$selectedCallTypeFilterHash();

  @$internal
  @override
  SelectedCallTypeFilter create() => SelectedCallTypeFilter();

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(RevoCallType value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<RevoCallType>(value),
    );
  }
}

String _$selectedCallTypeFilterHash() =>
    r'1d1544338c13cb1408b62196e9922cf88a68dfcd';

abstract class _$SelectedCallTypeFilter extends $Notifier<RevoCallType> {
  RevoCallType build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<RevoCallType, RevoCallType>;
    final element = ref.element as $ClassProviderElement<
        AnyNotifier<RevoCallType, RevoCallType>,
        RevoCallType,
        Object?,
        Object?>;
    element.handleCreate(ref, build);
  }
}

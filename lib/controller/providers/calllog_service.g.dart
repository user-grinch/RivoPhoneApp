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

String _$callLogServiceHash() => r'd2958a4928efb882185131f03338c1a58ee619ef';

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

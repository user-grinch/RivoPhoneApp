// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'theme_service.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(ThemeService)
final themeServiceProvider = ThemeServiceProvider._();

final class ThemeServiceProvider
    extends $NotifierProvider<ThemeService, ThemeState> {
  ThemeServiceProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'themeServiceProvider',
          isAutoDispose: true,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$themeServiceHash();

  @$internal
  @override
  ThemeService create() => ThemeService();

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(ThemeState value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<ThemeState>(value),
    );
  }
}

String _$themeServiceHash() => r'2696b2a3baf6f1de63d6531ea6ae88331cb2e024';

abstract class _$ThemeService extends $Notifier<ThemeState> {
  ThemeState build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<ThemeState, ThemeState>;
    final element = ref.element as $ClassProviderElement<
        AnyNotifier<ThemeState, ThemeState>, ThemeState, Object?, Object?>;
    element.handleCreate(ref, build);
  }
}

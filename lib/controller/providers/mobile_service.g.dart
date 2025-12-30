// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'mobile_service.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(getSimInfo)
final getSimInfoProvider = GetSimInfoProvider._();

final class GetSimInfoProvider extends $FunctionalProvider<
        AsyncValue<List<SimCard>>, List<SimCard>, FutureOr<List<SimCard>>>
    with $FutureModifier<List<SimCard>>, $FutureProvider<List<SimCard>> {
  GetSimInfoProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'getSimInfoProvider',
          isAutoDispose: false,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$getSimInfoHash();

  @$internal
  @override
  $FutureProviderElement<List<SimCard>> $createElement(
          $ProviderPointer pointer) =>
      $FutureProviderElement(pointer);

  @override
  FutureOr<List<SimCard>> create(Ref ref) {
    return getSimInfo(ref);
  }
}

String _$getSimInfoHash() => r'16e9cdc41efe6b9a23a11a552dca03ec688b4f3d';

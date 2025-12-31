// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'contact_service.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(ContactService)
final contactServiceProvider = ContactServiceProvider._();

final class ContactServiceProvider
    extends $AsyncNotifierProvider<ContactService, ContactList> {
  ContactServiceProvider._()
      : super(
          from: null,
          argument: null,
          retry: null,
          name: r'contactServiceProvider',
          isAutoDispose: false,
          dependencies: null,
          $allTransitiveDependencies: null,
        );

  @override
  String debugGetCreateSourceHash() => _$contactServiceHash();

  @$internal
  @override
  ContactService create() => ContactService();
}

String _$contactServiceHash() => r'8f9f6f9278934d4f3aed54a2fb8e099586feb033';

abstract class _$ContactService extends $AsyncNotifier<ContactList> {
  FutureOr<ContactList> build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<AsyncValue<ContactList>, ContactList>;
    final element = ref.element as $ClassProviderElement<
        AnyNotifier<AsyncValue<ContactList>, ContactList>,
        AsyncValue<ContactList>,
        Object?,
        Object?>;
    element.handleCreate(ref, build);
  }
}

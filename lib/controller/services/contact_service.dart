import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:permission_handler/permission_handler.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart';
import 'package:revo/controller/services/activity_service.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/controller/services/mobile_service.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'contact_service.g.dart';

typedef ContactList = List<Contact>; // needed for generator, don't remove

@Riverpod(keepAlive: true)
class ContactService extends _$ContactService {
  @override
  Future<ContactList> build() async {
    return _fetchContacts();
  }

  Future<ContactList> _fetchContacts() async {
    await ActivityService().requestPermissions();

    if (await Permission.contacts.status.isGranted) {
      final simInfo = await ref.read(getSimInfoProvider.future);
      final defaultSim = ref.read(defaultSimProvider);
      final countryCode = simInfo.isNotEmpty
          ? (simInfo.length > 1
              ? simInfo[defaultSim].countryCode
              : simInfo[0].countryCode)
          : null;

      var contacts = (await fc.FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
        deduplicateProperties: true,
      ))
          .toList();

      contacts = contacts.where((e) => e.phones.isNotEmpty).toList();
      return contacts
          .map((e) => Contact.fromInternal(e, countryCode: countryCode))
          .toList();
    }
    return [];
  }

  void refresh() {
    ref.invalidateSelf();
  }

  ContactList filterByStars() {
    final currentList = state.value ?? [];
    return currentList.where((e) => e.isStarred).toList();
  }

  Contact findByNumber(String number) {
    final currentList = state.value ?? [];
    final simInfo = ref.read(getSimInfoProvider).value;
    final defaultSim = ref.read(defaultSimProvider);
    final countryCode = simInfo?.isNotEmpty ?? false
        ? (simInfo!.length > 1
            ? simInfo[defaultSim].countryCode
            : simInfo[0].countryCode)
        : null;
    String target = normalizePhoneNumber(number, countryCode: countryCode);
    try {
      return currentList.firstWhere((f) {
        return f.numbers.any((g) {
          String contactNumber = g.international;
          return contactNumber.endsWith(target) || target == contactNumber;
        });
      });
    } catch (_) {
      return Contact(id: number, name: "Unknown", numbers: [
        PhoneNumber(
            isoCode: IsoCode.values.byName((countryCode ?? "BD").toUpperCase()),
            nsn: target)
      ]);
    }
  }

  List<Contact> fuzzyFindByNameOrNumber(String name, String number) {
    final currentList = state.value ?? [];

    final targetName = name.toLowerCase();
    final targetNumber = number.replaceAll(RegExp(r'[\s-]'), '');

    if (targetName.isEmpty && targetNumber.isEmpty) return currentList;

    return currentList.where((contact) {
      final nameMatches = contact.name.toLowerCase().contains(targetName);

      final numberMatches = contact.numbers.any((phone) {
        final contactNumber =
            phone.international.replaceAll(RegExp(r'[\s-]'), '');
        return contactNumber.contains(targetNumber);
      });

      return nameMatches || numberMatches;
    }).toList();
  }

  Future<void> createNewContact({String? number}) async {
    if (await Permission.contacts.status.isGranted) {
      if (number == null) {
        await fc.FlutterContacts.openExternalInsert();
      } else {
        await fc.FlutterContacts.openExternalInsert(
            fc.Contact(phones: [fc.Phone(number)]));
      }
      ref.invalidateSelf();
    }
  }

  Future<void> insertContact(Contact contact) async {
    if (await Permission.contacts.status.isGranted) {
      await fc.FlutterContacts.insertContact(contact.toInternal());
      ref.invalidateSelf();
    }
  }

  Future<void> insertContactFromVCard(String data) async {
    if (await Permission.contacts.status.isGranted) {
      try {
        await fc.FlutterContacts.insertContact(fc.Contact.fromVCard(data));
        debugPrint('Contact added successfully!');
        ref.invalidateSelf();
      } catch (e) {
        debugPrint('Error adding contact: $e');
      }
    } else {
      debugPrint('Permission to access contacts denied!');
    }
  }

  Future<void> editContact(Contact contact) async {
    if (await Permission.contacts.status.isGranted) {
      await fc.FlutterContacts.openExternalEdit(contact.id);
      ref.invalidateSelf();
    } else {
      debugPrint("Permission denied to access contacts");
    }
  }

  void updateContact({
    required Contact contact,
    bool withGroups = false,
  }) async {
    if (await Permission.contacts.status.isGranted) {
      fc.Contact fcContact = contact.toInternal();
      fcContact.groups = [];
      await fc.FlutterContacts.updateContact(
        fcContact,
        withGroups: withGroups,
      );
      ref.invalidateSelf();
    } else {
      debugPrint("Permission denied to access contacts");
    }
  }
}

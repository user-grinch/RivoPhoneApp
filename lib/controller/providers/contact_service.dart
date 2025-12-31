import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:flutter_contacts/flutter_contacts.dart' as lib;
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/utils/utils.dart';
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
      var contacts = (await fc.FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
      ))
          .toList();

      contacts = contacts.where((e) => e.phones.isNotEmpty).toList();
      return contacts.map((e) => Contact.fromInternal(e)).toList();
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
    String target = normalizePhoneNumber(number);
    try {
      return currentList.firstWhere((f) {
        return f.phones.any((g) {
          String contactNumber = normalizePhoneNumber(g);
          return contactNumber.endsWith(target) || target == contactNumber;
        });
      });
    } catch (_) {
      return Contact(
          id: '"Unknown"',
          displayName: "Unknown",
          fullName: "Unknown",
          phones: [number]);
    }
  }

  Contact findByName(String name) {
    final currentList = state.value ?? [];
    try {
      return currentList.firstWhere((f) {
        return name.isNotEmpty &&
            f.phones.isNotEmpty &&
            f.fullName.toLowerCase().contains(name.toLowerCase());
      });
    } catch (_) {
      return Contact(id: '"Unknown"', displayName: "Unknown", fullName: name);
    }
  }

  List<Contact> findAllByNameOrNumber(String name, String number) {
    String target = normalizePhoneNumber(number);
    final currentList = state.value ?? [];

    if (name == "" && number == "") {
      return currentList;
    }
    try {
      return currentList.where((f) {
        bool nameMatches = name.isNotEmpty &&
            f.fullName.toLowerCase().contains(name.toLowerCase());

        bool isNumber = number.isNotEmpty && num.tryParse(number) != null;
        bool numberMatches = isNumber &&
            f.phones.any((g) {
              String contactNumber = normalizePhoneNumber(g);
              return contactNumber.contains(target) || target == contactNumber;
            });
        return nameMatches || numberMatches;
      }).toList();
    } catch (_) {
      return [];
    }
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
        await fc.FlutterContacts.insertContact(lib.Contact.fromVCard(data));
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
      await fc.FlutterContacts.updateContact(
        contact.toInternal(),
        withGroups: withGroups,
      );
      ref.invalidateSelf();
    } else {
      debugPrint("Permission denied to access contacts");
    }
  }
}

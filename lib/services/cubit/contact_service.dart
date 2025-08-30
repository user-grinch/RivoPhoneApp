import 'package:bloc/bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/utils/utils.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as lib;

class ContactService extends Cubit<List<Contact>> {
  ContactService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    await ActivityService().requestPermissions();
    if (await Permission.contacts.status.isGranted) {
      var contact = (await fc.FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
      ))
          .toList();

      contact = contact.where((e) => e.phones.isNotEmpty).toList();
      emit(contact.map((e) => Contact.fromInternal(e)).toList());
    }
  }

  List<Contact> filterByStars() {
    return state.where((e) => e.isStarred).toList();
  }

  ContactService refresh() {
    _initialize();
    return this;
  }

  Contact findByNumber(String number) {
    String target = normalizePhoneNumber(number);
    try {
      return state.firstWhere((f) {
        return f.phones.any((g) {
          String contactNumber = normalizePhoneNumber(g);
          return contactNumber.endsWith(target) ||
              // target.endsWith(contactNumber) ||
              target == contactNumber;
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
    try {
      return state.firstWhere((f) {
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
    try {
      return state.where((f) {
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
    }
  }

  Future<void> insertContact(Contact contact) async {
    if (await Permission.contacts.status.isGranted) {
      await fc.FlutterContacts.insertContact(contact.toInternal());
    }
  }

  Future<void> insertContactFromVCard(String data) async {
    if (await Permission.contacts.status.isGranted) {
      try {
        await fc.FlutterContacts.insertContact(lib.Contact.fromVCard(data));
        debugPrint('Contact added successfully!');
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
    } else {
      debugPrint("Permission denied to access contacts");
    }
  }

  void updateContact({
    required Contact contact,
    bool withGroups = false,
  }) async {
    if (await Permission.contacts.status.isGranted) {
      fc.FlutterContacts.updateContact(
        contact.toInternal(),
        withGroups: withGroups,
      );
    } else {
      debugPrint("Permission denied to access contacts");
    }
  }
}

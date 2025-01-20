import 'package:bloc/bloc.dart';
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
    if (state.isEmpty && await Permission.contacts.status.isGranted) {
      var contact = (await fc.FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
      ))
          .toList();
      emit(contact.map((e) => Contact.fromInternal(e)).toList());
    }
  }

  List<Contact> filterByStars() {
    return state.where((e) => e.isStarred).toList();
  }

  ContactService getAll() {
    _initialize();
    return this;
  }

  Contact findByNumber(String number) {
    String target = normalizePhoneNumber(number);
    try {
      return state.firstWhere((f) {
        return f.phones.any((g) {
          String contactNumber = normalizePhoneNumber(g);
          return target.endsWith(contactNumber) || target == contactNumber;
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

  List<Contact> findAllByNameOrNumber(String name, String number) {
    String target = normalizePhoneNumber(number);
    try {
      return state.where((f) {
        return f.phones.any((g) {
              String contactNumber = normalizePhoneNumber(g);
              return contactNumber.contains(target) || target == contactNumber;
            }) ||
            f.fullName.toLowerCase().contains(name.toLowerCase());
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
        print('Contact added successfully!');
      } catch (e) {
        print('Error adding contact: $e');
      }
    } else {
      print('Permission to access contacts denied!');
    }
  }

  Future<void> editContact(Contact contact) async {
    if (await Permission.contacts.status.isGranted) {
      await fc.FlutterContacts.openExternalEdit(contact.id);
    } else {
      print("Permission denied to access contacts");
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
      print("Permission denied to access contacts");
    }
  }
}

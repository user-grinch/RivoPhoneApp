import 'package:bloc/bloc.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/utils/utils.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as lib;

class ContactService extends Cubit<List<Contact>> {
  ContactService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    if (state.isEmpty) {
      if (await Permission.contacts.request().isGranted) {
        var f_contact = (await fc.FlutterContacts.getContacts(
          withProperties: true,
          withAccounts: true,
          withGroups: true,
          withPhoto: true,
          withThumbnail: true,
        ))
            .toList();
        emit(f_contact.map((e) => Contact.fromInternal(e)).toList());
      }
    }
  }

  List<Contact> filterByStars() {
    return state.where((e) => e.isStarred).toList();
  }

  ContactService getAll() {
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

  Future<void> insertContact(Contact contact) async {
    await fc.FlutterContacts.insertContact(contact.toInternal());
  }

  Future<void> insertContactFromVCard(String data) async {
    if (await fc.FlutterContacts.requestPermission()) {
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
    if (await fc.FlutterContacts.requestPermission()) {
      await fc.FlutterContacts.openExternalEdit(contact.id);
    } else {
      print("Permission denied to access contacts");
    }
  }

  void updateContact({required Contact contact, bool withGroups = false}) {
    fc.FlutterContacts.updateContact(
      contact.toInternal(),
      withGroups: withGroups,
    );
  }
}

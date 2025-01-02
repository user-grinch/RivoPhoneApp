import 'package:flutter_contacts/contact.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:permission_handler/permission_handler.dart';

class ContactService {
  late List<Contact> _contacts = [];
  late List<Contact> _favContacts = [];

  ContactService._internal();
  static final ContactService _instance = ContactService._internal();

  factory ContactService() {
    return _instance;
  }

  Future<void> _fetchContacts() async {
    if (await Permission.contacts.request().isGranted) {
      _contacts = await FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
      );
      _favContacts = _contacts.where((element) => element.isStarred).toList();
    }
  }

  Future<void> initialize() async {
    if (_contacts.isEmpty) {
      await _fetchContacts();
    }
  }

  Contact getContact(String id) {
    return _contacts.firstWhere((element) => element.id == id);
  }

  int getLength() {
    return _contacts.length;
  }

  int getFavLength() {
    return _favContacts.length;
  }

  Contact elementAt(int index) {
    return _contacts[index];
  }

  List<Contact> getContacts() {
    return _contacts;
  }

  List<Contact> getFavContacts() {
    return _favContacts;
  }

  Contact favElementAt(int index) {
    return _favContacts[index];
  }

  List<Contact> getContactsFiltered(String filter) {
    return _contacts
        .where((element) => element.displayName.contains(filter))
        .toList();
  }
}

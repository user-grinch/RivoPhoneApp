import 'dart:typed_data';

import 'package:call_e_log/call_log.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/modal/call_log_data.dart';

class ContactService {
  late List<Contact> _contacts = [];
  late List<Contact> _favContacts = [];
  late List<CallLogData> _callLog = [];

  List<CallLogData> get callLogs => _callLog;
  List<Contact> get contacts => _contacts;
  List<Contact> get favContacts => _favContacts;

  ContactService._internal();
  static final ContactService _instance = ContactService._internal();

  factory ContactService() {
    return _instance;
  }

  Future<void> initialize() async {
    if (_contacts.isEmpty) {
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
    if (_callLog.isEmpty) {
      List<CallLogEntry> logs = (await CallLog.get() ?? []).toList();
      _callLog = (logs.map((e) {
        Uint8List? photo;
        try {
          photo = _contacts
              .firstWhere((element) => element.displayName == e.name)
              .photo;
        } catch (e) {
          photo = null;
        }
        return CallLogData.fromEntry(entry: e, profile: photo);
      })).toList();
    }
  }

  List<Contact> getContactsFiltered(String filter) {
    return _contacts
        .where((element) => element.displayName.contains(filter))
        .toList();
  }
}

import 'dart:typed_data';
import 'package:call_e_log/call_log.dart' as lib;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/utils/utils.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;

class CallLogService extends Cubit<List<CallLog>> {
  CallLogService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    await ActivityService().requestPermissions();
    if (await Permission.phone.status.isGranted) {
      var fContact = (await fc.FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withThumbnail: true,
      ))
          .toList();

      List<lib.CallLogEntry> logs = (await lib.CallLog.get()).toList();

      var list = logs.map((e) {
        Uint8List? photo;

        try {
          String logNumber = normalizePhoneNumber(e.number!);
          fc.Contact contact = fContact.firstWhere((f) {
            return f.phones.any((g) {
              String contactNumber = normalizePhoneNumber(g.normalizedNumber);
              return logNumber.endsWith(contactNumber) ||
                  logNumber == contactNumber;
            });
          });

          if ((e.name ?? '').isEmpty) {
            e.name =
                '${contact.name.first} ${contact.name.middle} ${contact.name.last}'
                    .trim();
          }
          photo = contact.thumbnail;
        } catch (_) {
          photo = null;
        }

        return CallLog.fromEntry(entry: e, profile: photo);
      }).toList();

      emit(list);
    }
  }

  Future<void> fetchData(BuildContext context) async {
    if (state.isNotEmpty && await Permission.phone.status.isGranted) {
      List<CallLog?> list = state.map((e) {
        var contactList = context.read<ContactService>();
        var contact = contactList.findByNumber(e.number);
        e = CallLog(
          contact.photo,
          name: e.name,
          number: e.number,
          simDisplayName: e.simDisplayName,
          date: e.date,
          duration: e.duration,
          type: e.type,
          accountId: e.accountId,
        );
      }).toList();
      emit(state);
    }
  }

  List<CallLog> filterByNumber(List<String> numbers) {
    final filteredLogs = state
        .where(
          (element) => numbers.any(
            (e) {
              String p1 = normalizePhoneNumber(e);
              String p2 = normalizePhoneNumber(element.number);
              return p1 == p2 || p1.endsWith(p2) || p2.endsWith(p1);
            },
          ),
        )
        .toList();
    return filteredLogs;
  }

  CallLogService refresh() {
    _initialize();
    return this;
  }
}

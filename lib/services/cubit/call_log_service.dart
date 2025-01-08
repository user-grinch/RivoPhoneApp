import 'dart:typed_data';
import 'package:call_e_log/call_log.dart' as lib;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/utils/utils.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;

class CallLogService extends Cubit<List<CallLog>> {
  CallLogService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    if (state.isEmpty) {
      var f_contact = (await fc.FlutterContacts.getContacts(
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
          fc.Contact contact = f_contact.firstWhere((f) {
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
    if (state.isNotEmpty) {
      var list = state.map((e) {
        Uint8List? photo;
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
              return normalizePhoneNumber(e) ==
                  normalizePhoneNumber(element.number);
            },
          ),
        )
        .toList();
    return filteredLogs;
  }

  CallLogService getAll() {
    _initialize();
    return this;
  }
}

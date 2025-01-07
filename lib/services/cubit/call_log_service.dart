import 'dart:typed_data';
import 'package:bloc/bloc.dart';
import 'package:call_e_log/call_log.dart' as lib;
import 'package:revo/model/call_log.dart';
import 'package:revo/utils/utils.dart';

class CallLogService extends Cubit<List<CallLog>> {
  CallLogService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    if (state.isEmpty) {
      List<lib.CallLogEntry> logs = (await lib.CallLog.get()).toList();
      var list = logs.map((e) {
        Uint8List? photo;

        // Optional logic for assigning contact details
        // try {
        //   var contact = _contacts.firstWhere(
        //     (element) => element.phones.any((phone) =>
        //         normalizePhoneNumber(phone.number) == normalizePhoneNumber(e.number!)),
        //   );

        //   e.name = contact.displayName;
        //   photo = contact.photoOrThumbnail;
        // } catch (_) {
        //   e.name = '';
        //   photo = null;
        // }

        return CallLog.fromEntry(entry: e, profile: photo);
      }).toList();
      emit(list);
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

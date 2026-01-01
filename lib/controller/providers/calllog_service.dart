import 'dart:typed_data';
import 'package:call_log/call_log.dart' as lib;
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/view/utils/utils.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'calllog_service.g.dart';

typedef CallLogList = List<CallLog>; // for generator

@Riverpod(keepAlive: true)
class CallLogService extends _$CallLogService {
  @override
  Future<CallLogList> build() async {
    return _fetchLogs();
  }

  Future<CallLogList> _fetchLogs() async {
    await ActivityService().requestPermissions();
    if (!await Permission.phone.status.isGranted) return [];
    final contacts = ref.watch(contactServiceProvider.notifier);

    final logs = (await lib.CallLog.get()).toList();
    final fContacts = await contacts.build();

    return logs.map((entry) {
      Uint8List? photo;
      try {
        final logNumber = normalizePhoneNumber(entry.number!);
        final contact = fContacts.firstWhere((c) {
          return c.phones.any((p) {
            return logNumber.endsWith(p) ||
                logNumber == p ||
                logNumber.startsWith(p);
          });
        });

        if ((entry.name ?? '').isEmpty) {
          entry.name = contact.name;
        }
        photo = contact.photo;
      } catch (_) {
        photo = null;
      }

      return CallLog.fromInternal(entry: entry, profile: photo);
    }).toList();
  }

  Future<void> refresh() async {
    try {
      final newLogs = await _fetchLogs();
      state = AsyncValue.data(newLogs);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  List<CallLog> filterByNumber(List<String> numbers) {
    final current = state.value ?? [];
    return current.where((element) {
      return numbers.any((e) {
        final p1 = normalizePhoneNumber(e);
        final p2 = normalizePhoneNumber(element.number);
        return p1 == p2 || p1.endsWith(p2) || p2.endsWith(p1);
      });
    }).toList();
  }

  Future<void> enrichWithContacts(WidgetRef ref) async {
    final currentLogs = state.value ?? [];
    final contactService = ref.read(contactServiceProvider.notifier);

    final updatedLogs = currentLogs.map((log) {
      final contact = contactService.findByNumber(log.number);
      return CallLog(
        contact.photo,
        name: log.name,
        number: log.number,
        simDisplayName: log.simDisplayName,
        date: log.date,
        duration: log.duration,
        type: log.type,
        accountId: log.accountId,
      );
    }).toList();

    state = AsyncValue.data(updatedLogs);
  }
}

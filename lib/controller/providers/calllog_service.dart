import 'dart:typed_data';
import 'package:call_log/call_log.dart' as lib;
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart' as ct;
import 'package:revo/model/contact.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'calllog_service.g.dart';

typedef CallLogList = List<CallLog>; // for generator
typedef RevoCallType = ct.CallType; // for generator

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

    final simInfo = await ref.read(getSimInfoProvider.future);
    final defaultSim = ref.read(defaultSimProvider);
    final countryCode = simInfo.isNotEmpty
        ? (simInfo.length > 1
            ? simInfo[defaultSim].countryCode
            : simInfo[0].countryCode)
        : null;

    final logs = (await lib.CallLog.get()).toList();
    final fContacts = await contacts.build();

    return logs.map((entry) {
      Uint8List? photo;
      try {
        final logNumber =
            normalizePhoneNumber(entry.number!, countryCode: countryCode);
        Contact? contact;

        for (var c in fContacts) {
          bool isMatch = false;

          for (var p in c.numbers) {
            if (isSameNumber(p.international, logNumber)) {
              isMatch = true;
              break;
            }
          }

          if (isMatch) {
            contact = c;
            break;
          }
        }

        entry.name = contact?.name;
        photo = contact?.photo;
      } catch (_) {
        photo = null;
      }

      return CallLog.fromInternal(
          entry: entry, profile: photo, countryCode: countryCode);
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
    final simInfo = ref.read(getSimInfoProvider).value;
    final defaultSim = ref.read(defaultSimProvider);
    final countryCode = simInfo?.isNotEmpty ?? false
        ? (simInfo!.length > 1
            ? simInfo[defaultSim].countryCode
            : simInfo[0].countryCode)
        : null;

    return current.where((element) {
      return numbers.any((e) {
        final p1 = normalizePhoneNumber(e, countryCode: countryCode);
        final p2 = element.number.international;
        return isSameNumber(p1, p2);
      });
    }).toList();
  }
}

@riverpod
class SelectedCallTypeFilter extends _$SelectedCallTypeFilter {
  @override
  RevoCallType build() => RevoCallType.unknown;

  void update(RevoCallType value) {
    state = value;
  }
}

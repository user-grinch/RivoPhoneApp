import 'package:revo/model/call_log.dart';
import 'package:revo/utils/utils.dart';

class GroupedCallLog {
  final List<CallLog> logs;

  GroupedCallLog(this.logs);

  CallLog get latest => logs.first;
  int get count => logs.length;
}

List<GroupedCallLog> groupCallLogs(List<CallLog> logs) {
  if (logs.isEmpty) return [];

  final List<GroupedCallLog> grouped = [];
  List<CallLog> currentGroup = [logs.first];

  bool isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  bool isSameCaller(CallLog a, CallLog b) {
    final p1 = normalizePhoneNumber(a.number);
    final p2 = normalizePhoneNumber(b.number);
    return p1 == p2 || p1.endsWith(p2) || p2.endsWith(p1);
  }

  for (int i = 1; i < logs.length; i++) {
    final prev = logs[i - 1];
    final curr = logs[i];

    if (isSameDay(prev.date, curr.date) && isSameCaller(prev, curr)) {
      currentGroup.add(curr);
    } else {
      grouped.add(GroupedCallLog(currentGroup));
      currentGroup = [curr];
    }
  }

  grouped.add(GroupedCallLog(currentGroup));
  return grouped;
}

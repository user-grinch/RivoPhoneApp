import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/call_log.dart';

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

  for (int i = 1; i < logs.length; i++) {
    final prev = logs[i - 1];
    final curr = logs[i];

    if (isSameDay(prev.date, curr.date) &&
        isSameNumber(prev.number.international, curr.number.international)) {
      currentGroup.add(curr);
    } else {
      grouped.add(GroupedCallLog(currentGroup));
      currentGroup = [curr];
    }
  }

  grouped.add(GroupedCallLog(currentGroup));
  return grouped;
}

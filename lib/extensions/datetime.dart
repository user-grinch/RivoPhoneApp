import 'package:intl/intl.dart';

extension ContextAware on DateTime {
  String getContextAwareDate() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(Duration(days: 1));
    final aWeekAgo = today.subtract(Duration(days: 7));

    if (isAfter(today)) {
      return 'Today';
    } else if (isAfter(yesterday)) {
      return 'Yesterday';
    } else if (isAfter(aWeekAgo)) {
      return DateFormat.EEEE().format(this);
    } else {
      return DateFormat('d MMM, y').format(this);
    }
  }

  String getContextAwareDateTime() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(Duration(days: 1));
    final aWeekAgo = today.subtract(Duration(days: 7));

    if (isAfter(today)) {
      return '${DateFormat.jm().format(this)}, Today';
    } else if (isAfter(yesterday)) {
      return '${DateFormat.jm().format(this)}, Yesterday';
    } else if (isAfter(aWeekAgo)) {
      return '${DateFormat.EEEE().format(this)}, ${DateFormat.jm().format(this)}';
    } else {
      return '${DateFormat('d MMM, y').format(this)}, ${DateFormat.jm().format(this)}';
    }
  }
}

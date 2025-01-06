import 'package:flutter/material.dart';
import 'package:flutter_contacts/contact.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/datetime.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/modal/call_log_data.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/utils/calltypes.dart';
import 'package:revo/utils/math.dart';

class HistoryView extends StatefulWidget {
  List<Phone> numbers;
  HistoryView({super.key, required this.numbers});

  @override
  State<HistoryView> createState() => _HistoryViewState();
}

class _HistoryViewState extends State<HistoryView> {
  late ScrollController _controller;

  @override
  void initState() {
    _controller = ScrollController();
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    List<CallLogData> history =
        ContactService().getCallLogFiltered(widget.numbers);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Call History'),
      ),
      body: history.isEmpty
          ? const Center(
              child: Text(
                'No call history available.',
                style: TextStyle(fontSize: 16, color: Colors.grey),
              ),
            )
          : ListView.builder(
              itemCount: history.length,
              itemBuilder: (context, i) => displayHistory(context, history[i]),
            ),
    );
  }

  Widget displayHistory(BuildContext context, CallLogData history) {
    String underlineText =
        '${getCallText(history.type)}  ${convertSecondsToHMS(int.parse(history.duration))}';

    return ListTile(
      leading: Container(
        width: 50,
        height: 50,
        decoration: BoxDecoration(
          color: context.colorScheme.primary.withAlpha(25),
          shape: BoxShape.circle,
        ),
        child: Icon(getCallIcon(history.type),
            color: getCallColor(history.type, context), size: 28),
      ),
      title: Text(
        history.date.getContextAwareDateTime(),
        style: GoogleFonts.cabin(fontSize: 16),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            history.simDisplayName,
            style: const TextStyle(color: Colors.grey),
          ),
          Text(
            underlineText,
            style: const TextStyle(color: Colors.grey),
          ),
        ],
      ),
    );
  }
}

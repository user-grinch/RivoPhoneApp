import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extensions/datetime.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart';
import 'package:revo/services/cubit/call_log_service.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/utils.dart';

class HistoryView extends StatefulWidget {
  final List<String> numbers;

  const HistoryView({super.key, required this.numbers});

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
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text('Call History'),
      ),
      body: BlocBuilder<CallLogService, List<CallLog>>(
        builder: (BuildContext context, List<CallLog> state) {
          var logs =
              context.read<CallLogService>().filterByNumber(widget.numbers);
          if (logs.isEmpty) {
            return CenterText(
              text: 'No call logs found.',
            );
          }
          return ListView.builder(
            itemCount: logs.length,
            itemBuilder: (context, i) => _displayHistory(context, logs[i]),
          );
        },
      ),
    );
  }

  Widget _displayHistory(BuildContext context, CallLog history) {
    String underlineText =
        '${history.type.getText()}  ${convertSecondsToHMS(int.parse(history.duration))}';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 16),
      child: Container(
        decoration: BoxDecoration(
          color: context.colorScheme.secondaryContainer.withAlpha(100),
          shape: BoxShape.rectangle,
          borderRadius: BorderRadius.circular(25),
        ),
        child: ListTile(
          leading: Container(
            width: 50,
            height: 50,
            decoration: BoxDecoration(
              color: context.colorScheme.primary.withAlpha(25),
              shape: BoxShape.circle,
            ),
            child: Icon(history.type.getIcon(),
                color: history.type.getColor(), size: 28),
          ),
          title: Text(
            history.date.getContextAwareDateTime(),
            style: GoogleFonts.raleway(fontSize: 16),
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
              Text(
                history.number,
                style: const TextStyle(color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

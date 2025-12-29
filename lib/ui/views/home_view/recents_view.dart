import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extensions/datetime.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/model/group_call_log.dart';
import 'package:revo/services/cubit/call_log_service.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/popups/sim_choose_popup.dart';
import 'package:revo/utils/circle_profile.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:revo/utils/rounded_icon_btn.dart';
import 'package:revo/utils/utils.dart';

class RecentsView extends StatefulWidget {
  const RecentsView({super.key});

  @override
  State<RecentsView> createState() => _RecentsViewState();
}

class _RecentsViewState extends State<RecentsView> {
  late final ScrollController _controller;

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

  Future<void> _refreshLogs(BuildContext context) async {
    // Call a method in your CallLogService to refresh the call logs.
    context.read<CallLogService>().refresh(); // Example method
  }

  @override
  Widget build(BuildContext context) {
    return Scrollbar(
      trackVisibility: true,
      thickness: 2.5,
      interactive: true,
      radius: Radius.circular(30),
      controller: _controller,
      child: RefreshIndicator(
        onRefresh: () => _refreshLogs(context),
        child: BlocBuilder<CallLogService, List<CallLog>>(
          builder: (BuildContext context, List<CallLog> state) {
            if (state.isEmpty) {
              return ListView(
                physics: AlwaysScrollableScrollPhysics(),
                children: const [
                  Center(
                    child: Padding(
                      padding: EdgeInsets.all(20.0),
                      child: Text('No call logs found.'),
                    ),
                  ),
                ],
              );
            }
            final groupedLogs = groupCallLogs(state);

            return ListView.builder(
              itemCount: groupedLogs.length,
              controller: _controller,
              physics: AlwaysScrollableScrollPhysics(),
              itemBuilder: (context, i) {
                return _buildLog(
                  context,
                  groupedLogs[i],
                  _shouldShowHeader(groupedLogs, i),
                );
              },
            );
          },
        ),
      ),
    );
  }

  bool _shouldShowHeader(List<GroupedCallLog> logs, int i) {
    if (i == 0) return true;

    final currentDate = logs[i].latest.date;
    final previousDate = logs[i - 1].latest.date;

    return currentDate.day != previousDate.day ||
        currentDate.month != previousDate.month ||
        currentDate.year != previousDate.year;
  }

  Widget _buildLog(
      BuildContext context, GroupedCallLog groupedLog, bool showDateHeader) {
    final log = groupedLog.latest;

    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showDateHeader)
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 50, 0, 0),
            child: Text(
              log.date.getContextAwareDate(),
              style: GoogleFonts.raleway(
                fontSize: 20,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
        ListTile(
          onTap: () async {
            simChooserDialog(context, log.number);
          },
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          leading: CircleProfile(
            name: log.name,
            profile: log.profile,
            size: 30,
          ),
          title: Text(
            log.displayName,
            style: GoogleFonts.raleway(
              fontSize: 16,
              color: context.colorScheme.onSurface,
            ),
          ),
          trailing: RoundedIconButton(
            context,
            icon: HugeIcons.strokeRoundedArrowRight01,
            size: 30,
            onTap: () async {
              Contact contact =
                  context.read<ContactService>().findByName(log.name);
              if (contact.phones.isEmpty) {
                contact =
                    context.read<ContactService>().findByNumber(log.number);
              }
              await Navigator.of(context)
                  .pushNamed(contactInfoRoute, arguments: contact);
            },
          ),
          subtitle: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  ...groupedLog.logs
                      .map(
                        (e) => Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 2.0),
                          child: Icon(
                            e.type.getIcon(),
                            color: e.type.getColor(),
                            size: 16,
                          ),
                        ),
                      )
                      .toList(),
                  SizedBox(width: 5),
                  groupedLog.count > 1
                      ? Text(
                          log.date.getContextAwareDate(),
                          style: GoogleFonts.raleway(fontSize: 12),
                        )
                      : Text(
                          log.date.getContextAwareDateTime(),
                          style: GoogleFonts.raleway(
                            fontSize: 12,
                            color: log.type.getColor(),
                          ),
                        ),
                ],
              ),
              if (groupedLog.count == 1)
                Text(
                  convertSecondsToHMS(int.parse(log.duration)),
                  style: GoogleFonts.raleway(
                    fontSize: 12,
                    color: context.colorScheme.onSurface.withAlpha(200),
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }
}

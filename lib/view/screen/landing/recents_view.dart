import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/datetime.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/calllog_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/model/call_type.dart';
import 'package:revo/model/group_call_log.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/utils/circle_profile.dart';
import 'package:revo/view/utils/rounded_icon_btn.dart';
import 'package:revo/view/utils/utils.dart';

class RecentsView extends ConsumerStatefulWidget {
  const RecentsView({super.key});

  @override
  ConsumerState<RecentsView> createState() => _RecentsViewState();
}

class _RecentsViewState extends ConsumerState<RecentsView> {
  late final ScrollController _controller;

  @override
  void initState() {
    super.initState();
    _controller = ScrollController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  bool _shouldShowHeader(List<GroupedCallLog> logs, int i) {
    if (i == 0) return true;
    final current = logs[i].latest.date;
    final previous = logs[i - 1].latest.date;
    return current.day != previous.day ||
        current.month != previous.month ||
        current.year != previous.year;
  }

  @override
  Widget build(BuildContext context) {
    final callLogsAsync = ref.watch(callLogServiceProvider);

    return callLogsAsync.when(
      data: (logs) {
        if (logs.isEmpty) {
          // Show empty scrollable so RefreshIndicator works
          return ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            children: const [
              Center(
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Text('No call logs found.'),
                ),
              ),
            ],
          );
        }

        final groupedLogs = groupCallLogs(logs);

        return Scrollbar(
          controller: _controller,
          trackVisibility: true,
          thickness: 2.5,
          radius: const Radius.circular(30),
          child: ExpressiveRefreshIndicator(
            backgroundColor: Theme.of(context).colorScheme.primary,
            onRefresh: () async {
              await Future.delayed(const Duration(seconds: 2));
              await ref.read(callLogServiceProvider.notifier).refresh();
            },
            child: ListView.builder(
              controller: _controller,
              physics: const AlwaysScrollableScrollPhysics(),
              itemCount: groupedLogs.length,
              itemBuilder: (context, i) {
                return _buildLog(
                  context,
                  groupedLogs[i],
                  _shouldShowHeader(groupedLogs, i),
                );
              },
            ),
          ),
        );
      },
      loading: () => const Center(child: const LoadingIndicatorM3E()),
      error: (e, s) => Center(child: Text('Error loading call logs: $e')),
    );
  }

  Widget _buildLog(
      BuildContext context, GroupedCallLog groupedLog, bool showHeader) {
    final log = groupedLog.latest;
    final simCardsAsync = ref.watch(getSimInfoProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showHeader)
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 50, 0, 0),
            child: Text(
              log.date.getContextAwareDate(),
              style: GoogleFonts.outfit(
                fontSize: 20,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
        ListTile(
          onTap: () async {
            final contactService = ref.read(contactServiceProvider.notifier);
            var contact = contactService.findByName(log.name);
            if (contact.phones.isEmpty) {
              contact = contactService.findByNumber(log.number);
              contact.displayName = log.displayName;
              contact.fullName = log.name;
            }
            await Navigator.of(context)
                .pushNamed(contactInfoRoute, arguments: contact);
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
            style: GoogleFonts.outfit(
              fontSize: 16,
              color: context.colorScheme.onSurface,
            ),
          ),
          trailing: RoundedIconButton(
            context,
            icon: FluentIcons.call_24_regular,
            size: 35,
            onTap: () async {
              simCardsAsync.whenData(
                (simCards) {
                  SimPicker(
                          context: context,
                          simCards: simCards,
                          number: log.number)
                      .show();
                },
              );
            },
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  ...groupedLog.logs.map(
                    (e) => Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 2),
                      child: Icon(
                        e.type.getIcon(),
                        color: e.type.getColor(),
                        size: 16,
                      ),
                    ),
                  ),
                  const SizedBox(width: 5),
                  groupedLog.count > 1
                      ? Text(
                          log.date.getContextAwareDate(),
                          style: GoogleFonts.outfit(fontSize: 12),
                        )
                      : Text(
                          log.date.getContextAwareDateTime(),
                          style: GoogleFonts.outfit(
                            fontSize: 12,
                            color: log.type.getColor(),
                          ),
                        ),
                ],
              ),
              if (groupedLog.count == 1)
                Text(
                  convertSecondsToHMS(int.parse(log.duration)),
                  style: GoogleFonts.outfit(
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

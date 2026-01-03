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
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/components/circle_profile.dart';

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

  bool _isLastInSection(List<GroupedCallLog> logs, int i) {
    if (i == logs.length - 1) return true;
    final current = logs[i].latest.date;
    final next = logs[i + 1].latest.date;
    return current.day != next.day ||
        current.month != next.month ||
        current.year != next.year;
  }

  @override
  Widget build(BuildContext context) {
    final callLogsAsync = ref.watch(callLogServiceProvider);
    final callTypeFilter = ref.watch(selectedCallTypeFilterProvider);

    return callLogsAsync.when(
      data: (logs) {
        if (logs.isEmpty) {
          return ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            children: [_buildEmptyState(context)],
          );
        }

        final groupedLogs = groupCallLogs(logs, callTypeFilter);

        return Scrollbar(
          controller: _controller,
          child: ExpressiveRefreshIndicator(
            backgroundColor: context.colorScheme.primary,
            onRefresh: () async {
              await ref.read(callLogServiceProvider.notifier).refresh();
            },
            child: Column(
              children: [
                _buildCallTypeFilterChips(
                    context: context,
                    selected: callTypeFilter,
                    onSelected: (v) {
                      ref
                          .read(selectedCallTypeFilterProvider.notifier)
                          .update(v);
                    }),
                Expanded(
                  child: ListView.builder(
                    controller: _controller,
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 20),
                    physics: const AlwaysScrollableScrollPhysics(),
                    itemCount: groupedLogs.length,
                    itemBuilder: (context, i) {
                      final showHeader = _shouldShowHeader(groupedLogs, i);
                      final isLast = _isLastInSection(groupedLogs, i);

                      return _buildLog(
                        context,
                        groupedLogs[i],
                        showHeader,
                        isLast,
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
      loading: () => const Center(child: LoadingIndicatorM3E()),
      error: (e, s) => Center(child: Text('Error: $e')),
    );
  }

  Widget _buildCallTypeFilterChips({
    required BuildContext context,
    required CallType selected,
    required ValueChanged<CallType> onSelected,
  }) {
    final colorScheme = Theme.of(context).colorScheme;

    return SizedBox(
      height: 40,
      child: Padding(
        padding: const EdgeInsetsDirectional.fromSTEB(16, 0, 16, 8),
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          itemCount: CallType.values.length,
          separatorBuilder: (_, __) => const SizedBox(width: 8),
          itemBuilder: (context, index) {
            final type = CallType.values[index];
            final isSelected = type == selected;

            return FilterChip(
              selected: isSelected,
              onSelected: (_) => onSelected(type),
              showCheckmark: false,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              avatar: isSelected
                  ? Icon(
                      FluentIcons.checkmark_24_filled,
                      size: 18,
                      color: isSelected
                          ? colorScheme.onSecondaryContainer
                          : type.getColor(),
                    )
                  : null,
              label: Text(
                type.getText(),
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: isSelected
                          ? colorScheme.onSecondaryContainer
                          : colorScheme.onSurfaceVariant,
                    ),
              ),
              backgroundColor: colorScheme.secondaryContainer,
              selectedColor: colorScheme.primaryContainer,
              side: BorderSide.none,
            );
          },
        ),
      ),
    );
  }

  Widget _buildLog(BuildContext context, GroupedCallLog groupedLog,
      bool showHeader, bool isLastInSection) {
    final log = groupedLog.latest;
    final simCardsAsync = ref.watch(getSimInfoProvider);
    final colorScheme = context.colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showHeader)
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 24, 0, 8),
            child: Text(
              log.date.getContextAwareDate(),
              style: GoogleFonts.outfit(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: colorScheme.primary,
              ),
            ),
          ),
        Container(
          decoration: BoxDecoration(
            color: colorScheme.secondaryContainer.withOpacity(0.35),
            borderRadius: BorderRadius.vertical(
              top: showHeader ? const Radius.circular(28) : Radius.zero,
              bottom: isLastInSection ? const Radius.circular(28) : Radius.zero,
            ),
          ),
          child: Column(
            children: [
              ListTile(
                onTap: () async {
                  final contactService =
                      ref.read(contactServiceProvider.notifier);
                  var contact =
                      contactService.findByNumber(log.number.international);
                  await Navigator.of(context)
                      .pushNamed(contactInfoRoute, arguments: contact);
                },
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.vertical(
                    top: showHeader ? const Radius.circular(28) : Radius.zero,
                    bottom: isLastInSection
                        ? const Radius.circular(28)
                        : Radius.zero,
                  ),
                ),
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                leading: CircleProfile(
                  name: log.name,
                  profile: log.profile,
                  size: 28,
                ),
                title: Text(
                  log.name.isEmpty ? log.number.international : log.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.outfit(
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                    color: colorScheme.onSurface,
                  ),
                ),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        ...groupedLog.logs.take(3).map((e) => Padding(
                              padding: const EdgeInsets.only(right: 4),
                              child: Icon(e.type.getIcon(),
                                  color: e.type.getColor(), size: 14),
                            )),
                        if (groupedLog.count > 3)
                          Text(' +${groupedLog.count - 3}',
                              style: const TextStyle(fontSize: 10)),
                        const SizedBox(width: 4),
                        Text(
                          "${groupedLog.count} calls • ${log.date.getContextAwareDateTime()}",
                          style: TextStyle(
                            fontSize: 12,
                            color:
                                colorScheme.onSurfaceVariant.withOpacity(0.7),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                trailing: ActionIconButton(
                  FluentIcons.call_20_filled,
                  size: 40,
                  onPressed: () {
                    simCardsAsync.whenData((simCards) {
                      SimPicker(
                              context: context,
                              simCards: simCards,
                              number: log.number.international)
                          .show();
                    });
                  },
                ),
              ),
              if (!isLastInSection)
                Padding(
                  padding: const EdgeInsets.only(left: 72, right: 16),
                  child: Divider(
                    height: 1,
                    thickness: 1,
                    color: colorScheme.outlineVariant.withOpacity(0.2),
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.only(top: 100),
        child: Column(
          children: [
            Icon(FluentIcons.history_24_regular,
                size: 64, color: context.colorScheme.primary.withOpacity(0.2)),
            const SizedBox(height: 16),
            Text('No call logs found.',
                style: GoogleFonts.outfit(
                    fontSize: 18, color: context.colorScheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}

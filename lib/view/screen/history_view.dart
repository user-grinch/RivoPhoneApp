import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/datetime.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart';
import 'package:revo/controller/providers/calllog_service.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/utils/utils.dart';

class HistoryView extends ConsumerStatefulWidget {
  final List<String> numbers;

  const HistoryView({super.key, required this.numbers});

  @override
  ConsumerState<HistoryView> createState() => _HistoryViewState();
}

class _HistoryViewState extends ConsumerState<HistoryView> {
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
    final asyncLogs = ref.watch(callLogServiceProvider);

    return Scaffold(
      appBar: AppBar(
        leadingWidth: 70,
        leading: Center(
          child: RoundedIconButton(
            FluentIcons.arrow_left_24_regular,
            size: 40,
            onPressed: () => Navigator.of(context).pop(),
          ),
        ),
        title: Text(
          'Call History',
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.bold,
            color: context.colorScheme.onSurface,
          ),
        ),
        centerTitle: true,
      ),
      body: asyncLogs.when(
        loading: () => Center(
          child: ExpressiveLoadingIndicator(
            color: context.colorScheme.primary,
          ),
        ),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(FluentIcons.error_circle_24_regular,
                  size: 48, color: Colors.red),
              const SizedBox(height: 16),
              Text("Error loading logs: $error"),
            ],
          ),
        ),
        data: (allLogs) {
          final filteredLogs = ref
              .read(callLogServiceProvider.notifier)
              .filterByNumber(widget.numbers);

          if (filteredLogs.isEmpty) {
            return _buildEmptyState(context);
          }

          return ListView.separated(
            controller: _controller,
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 40),
            itemCount: filteredLogs.length,
            separatorBuilder: (context, index) => const SizedBox(height: 12),
            itemBuilder: (context, i) =>
                _displayHistoryItem(context, filteredLogs[i]),
          );
        },
      ),
    );
  }

  Widget _displayHistoryItem(BuildContext context, CallLog history) {
    final duration = convertSecondsToHMS(int.parse(history.duration));
    final colorScheme = context.colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.secondaryContainer.withOpacity(0.35),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(
                color: colorScheme.surface,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(
                history.type.getIcon(),
                color: history.type.getColor(),
                size: 24,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    history.date.getContextAwareDateTime(),
                    style: GoogleFonts.outfit(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: colorScheme.onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Text(
                        history.type.getText(),
                        style: TextStyle(
                          color: history.type.getColor(),
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                        child: CircleAvatar(
                          radius: 1.5,
                          backgroundColor:
                              colorScheme.onSurfaceVariant.withOpacity(0.4),
                        ),
                      ),
                      Text(
                        duration,
                        style: TextStyle(
                          color: colorScheme.onSurfaceVariant,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    "${history.name} • ${history.number}",
                    style: TextStyle(
                      color: colorScheme.onSurfaceVariant.withOpacity(0.6),
                      fontSize: 11,
                      letterSpacing: 0.1,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    final colorScheme = context.colorScheme;
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(32),
            decoration: BoxDecoration(
              color: colorScheme.secondaryContainer.withOpacity(0.3),
              shape: BoxShape.circle,
            ),
            child: Icon(
              FluentIcons.history_24_regular,
              size: 64,
              color: colorScheme.primary.withOpacity(0.4),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'No History Found',
            style: GoogleFonts.outfit(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: colorScheme.onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 48),
            child: Text(
              'Call logs for these numbers will appear here once they are available.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                height: 1.4,
                color: colorScheme.onSurfaceVariant.withOpacity(0.7),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

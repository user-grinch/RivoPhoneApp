import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/datetime.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart';
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

  @override
  Widget build(BuildContext context) {
    return Scrollbar(
      trackVisibility: true,
      thickness: 2.5,
      interactive: true,
      radius: Radius.circular(30),
      controller: _controller,
      child: BlocBuilder<CallLogService, List<CallLog>>(
        builder: (BuildContext context, List<CallLog> state) {
          if (state.isEmpty) {
            return const Center(
              child: Text('No call logs found.'),
            );
          }

          return ListView.builder(
            itemCount: state.length,
            controller: _controller,
            itemBuilder: (context, i) {
              return _buildLog(
                context,
                state[i],
                _shouldShowHeader(state, i),
              );
            },
          );
        },
      ),
    );
  }

  bool _shouldShowHeader(List<CallLog> logs, int i) {
    return i == 0 || logs[i].date.weekday != logs[i - 1].date.weekday;
  }

  Widget _buildLog(BuildContext context, CallLog log, bool showDateHeader) {
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
            showDialog(
              context: context,
              builder: (context) => simChooserDialog(context, log.number),
            );
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
              await Navigator.of(context).pushNamed(contactInfoRoute,
                  arguments:
                      context.read<ContactService>().findByNumber(log.number));
            },
          ),
          subtitle: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    log.type.getIcon(),
                    color: log.type.getColor(),
                    size: 16,
                  ),
                  SizedBox(width: 5),
                  Text(
                    log.date.getContextAwareDateTime(),
                    style: GoogleFonts.raleway(
                      fontSize: 12,
                      color: log.type.getColor(),
                    ),
                  ),
                ],
              ),
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

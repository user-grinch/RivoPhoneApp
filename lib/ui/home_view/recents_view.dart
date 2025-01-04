import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/modal/call_log_type.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/utils/circle_profile.dart';

class RecentsView extends StatelessWidget {
  const RecentsView({super.key});

  @override
  Widget build(BuildContext context) {
    var callLogs = ContactService().callLogs;
    return FutureBuilder(
        future: ContactService().initialize(),
        builder: (context, snapshot) {
          return ListView.builder(
            itemCount: callLogs.length,
            itemBuilder: (context, i) {
              var log = callLogs[i];

              return ListTile(
                leading: CircleProfile(
                  profile: log.profile,
                  size: 30,
                ),
                title: Text(
                  log.name,
                  style: GoogleFonts.cabin(
                    fontSize: 16,
                    color: context.colorScheme.onSurface,
                  ),
                ),
                trailing: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: context.colorScheme.primary.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(Icons.call,
                      color: context.colorScheme.primary, size: 24),
                ),
                subtitle: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      log.date,
                      style: GoogleFonts.cabin(
                        fontSize: 12,
                        color: context.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    Row(
                      children: [
                        Icon(
                          log.type == CallLogType.incoming
                              ? Icons.call_received
                              : log.type == CallLogType.outgoing
                                  ? Icons.call_made
                                  : log.type == CallLogType.rejected
                                      ? Icons.call_end
                                      : log.type == CallLogType.blocked
                                          ? Icons.block
                                          : Icons.call_missed,
                          color: log.type == CallLogType.rejected
                              ? context.colorScheme.onSurface.withOpacity(0.6)
                              : log.type == CallLogType.missed
                                  ? context.colorScheme.error
                                  : log.type == CallLogType.blocked
                                      ? Colors.grey
                                      : context.colorScheme.primary,
                          size: 16,
                        ),
                        SizedBox(width: 5),
                        Text(
                          callLogs[i].simDisplayName,
                          style: GoogleFonts.cabin(
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                onTap: () async {
                  // Navigate to contact details if necessary
                  // await Navigator.of(context).push(MaterialPageRoute(
                  //   builder: (_) =>
                  //       ContactInfoView(ContactService().elementAt(i)),
                  // ));
                },
              );
            },
          );
        });
  }
}

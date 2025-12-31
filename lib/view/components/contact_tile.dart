import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/components/circle_profile.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ContactTile extends ConsumerWidget {
  final Contact contact;

  const ContactTile({
    super.key,
    required this.contact,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final simCards = ref.watch(getSimInfoProvider);
    final colorScheme = context.colorScheme;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Container(
        decoration: BoxDecoration(
          color: colorScheme.secondaryContainer.withOpacity(0.3),
          borderRadius: BorderRadius.circular(28),
        ),
        child: ListTile(
          onTap: () async {
            await Navigator.of(context).pushNamed(
              contactInfoRoute,
              arguments: contact,
            );
          },
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          leading: Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: colorScheme.primaryContainer.withOpacity(0.7),
              borderRadius: BorderRadius.circular(16),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: CircleProfile(
                name: contact.fullName,
                profile: contact.photo,
                size: 52,
              ),
            ),
          ),
          title: Text(
            contact.displayName,
            style: GoogleFonts.outfit(
              fontSize: 17,
              fontWeight: FontWeight.w600,
              color: colorScheme.onSurface,
            ),
          ),
          subtitle: contact.phones.isNotEmpty
              ? Text(
                  contact.phones[0],
                  style: GoogleFonts.outfit(
                    fontSize: 13,
                    color: colorScheme.onSurfaceVariant.withOpacity(0.7),
                  ),
                )
              : null,
          trailing: _buildCallAction(context, simCards),
        ),
      ),
    );
  }

  Widget _buildCallAction(BuildContext context, AsyncValue simCards) {
    final colorScheme = context.colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface.withOpacity(0.5),
        borderRadius: BorderRadius.circular(14),
      ),
      child: IconButton(
        icon: Icon(
          FluentIcons.call_24_filled,
          color: colorScheme.primary,
          size: 22,
        ),
        onPressed: () async {
          if (contact.phones.isNotEmpty) {
            simCards.whenData((value) => SimPicker(
                  context: context,
                  simCards: value,
                  number: contact.phones[0],
                ).show());
          }
        },
      ),
    );
  }
}

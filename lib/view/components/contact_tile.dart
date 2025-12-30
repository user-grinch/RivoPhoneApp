import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/utils/circle_profile.dart';
import 'package:revo/view/utils/rounded_icon_btn.dart';
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
    return ListTile(
      onTap: () async {
        if (contact.phones.isNotEmpty) {
          simCards.whenData((value) => SimPicker(
                  context: context, simCards: value, number: contact.phones[0])
              .show());
        }
      },
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      leading: CircleProfile(
        name: contact.fullName,
        profile: contact.photo,
        size: 30,
      ),
      title: Text(
        contact.displayName,
        style: GoogleFonts.raleway(
          fontSize: 16,
          color: context.colorScheme.onSurface.withAlpha(200),
        ),
      ),
      subtitle: Text(
        contact.phones
            .toString()
            .substring(1, contact.phones.toString().length - 1),
        style: GoogleFonts.raleway(
          fontSize: 12,
          color: context.colorScheme.onSurface.withAlpha(200),
        ),
      ),
      trailing: RoundedIconButton(
        context,
        icon: HugeIcons.strokeRoundedArrowRight01,
        size: 30,
        onTap: () async {
          await Navigator.of(context).pushNamed(
            contactInfoRoute,
            arguments: contact,
          );
        },
      ),
    );
  }
}

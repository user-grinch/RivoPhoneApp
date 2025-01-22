import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/ui/popups/sim_choose_popup.dart';
import 'package:revo/utils/circle_profile.dart';
import 'package:revo/utils/rounded_icon_btn.dart';

class ContactTile extends StatelessWidget {
  final Contact contact;

  const ContactTile({
    super.key,
    required this.contact,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: () async {
        showDialog(
          context: context,
          builder: (context) => simChooserDialog(context, contact.phones[0]),
        );
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

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/ui/sim_choose_popup.dart';
import 'package:revo/ui/views/contactinfo_view.dart';
import 'package:revo/utils/circle_profile.dart';

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
        await Navigator.of(context).pushNamed(
          contactInfoRoute,
          arguments: contact,
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
        style: GoogleFonts.cabin(
          fontSize: 16,
          color: context.colorScheme.onSurface.withAlpha(200),
        ),
      ),
      subtitle: Text(
        contact.phones
            .toString()
            .substring(1, contact.phones.toString().length - 1),
        style: GoogleFonts.cabin(
          fontSize: 12,
          color: context.colorScheme.primary.withAlpha(200),
        ),
      ),
      trailing: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: context.colorScheme.primary.withAlpha(25),
          shape: BoxShape.circle,
        ),
        child: IconButton(
          onPressed: () async {
            showDialog(
              context: context,
              builder: (context) =>
                  simChooserDialog(context, contact.phones[0]),
            );
          },
          icon: Icon(Icons.call, color: context.colorScheme.primary, size: 25),
        ),
      ),
    );
  }
}

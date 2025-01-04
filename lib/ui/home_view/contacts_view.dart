import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/ui/contactinfo_view.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/circle_profile.dart';

class ContactsView extends StatefulWidget {
  const ContactsView({super.key});

  @override
  State<ContactsView> createState() => _ContactsViewState();
}

class _ContactsViewState extends State<ContactsView> {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
        future: ContactService().initialize(),
        builder: (context, snapshot) {
          if (ContactService().contacts.length == 0) {
            return CenterText(text: 'No contacts');
          } else {
            return displayContacts(context);
          }
        });
  }

  Widget displayContacts(BuildContext context) {
    var contacts = ContactService().contacts;
    return ListView.builder(
        itemCount: contacts.length,
        itemBuilder: (context, i) => ListTile(
            title: Wrap(
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                CircleProfile(
                  profile: contacts[i].photoOrThumbnail,
                  size: 25,
                ),
                const SizedBox(width: 10),
                Text(ContactService().contacts[i].displayName,
                    style: GoogleFonts.cabin(
                      fontSize: 16,
                      color: context.colorScheme.onSurface,
                    )),
              ],
            ),
            onTap: () async {
              await Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) =>
                      ContactInfoView(ContactService().contacts[i])));
            }));
  }
}

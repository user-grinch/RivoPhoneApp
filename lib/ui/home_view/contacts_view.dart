import 'package:flutter/material.dart';
import 'package:flutter_contacts/contact.dart';
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
    return FutureBuilder(
        future: ContactService().initialize(),
        builder: (context, snapshot) {
          var contacts = ContactService().contacts;
          if (contacts.isEmpty) {
            return CenterText(text: 'No contacts');
          } else {
            return Scrollbar(
                trackVisibility: true,
                thickness: 2.5,
                interactive: true,
                radius: Radius.circular(30),
                controller: _controller,
                child: ListView.builder(
                  itemCount: contacts.length,
                  controller: _controller,
                  itemBuilder: (context, i) =>
                      displayContact(context, contacts, i),
                ));
          }
        });
  }

  Widget displayContact(
    BuildContext context,
    List<Contact> contacts,
    int i,
  ) {
    bool showDateHeader =
        i == 0 || contacts[i].name.first[0] != contacts[i - 1].name.first[0];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showDateHeader)
          Padding(
            padding: const EdgeInsets.fromLTRB(30, 50, 0, 0),
            child: Text(
              contacts[i].name.first[0],
              style: GoogleFonts.cabin(
                fontSize: 20,
                color: context.colorScheme.primary,
              ),
            ),
          ),
        ListTile(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
            ),
            title: Row(
              children: [
                const SizedBox(width: 10),
                CircleProfile(
                  name: contacts[i].displayName,
                  profile: contacts[i].photo,
                  size: 30,
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
            }),
      ],
    );
  }
}

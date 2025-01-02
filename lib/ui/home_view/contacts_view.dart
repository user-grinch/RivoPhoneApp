import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/ui/contactinfo_view.dart';

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
          final length = ContactService().getLength();
          if (length == 0) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    'No contacts found',
                    style: GoogleFonts.cabin(
                      fontSize: 16,
                      color: context.colorScheme.onSurface,
                    ),
                  ),
                ],
              ),
            );
          } else {
            return ListView.builder(
                itemCount: length,
                itemBuilder: (context, i) => ListTile(
                    title: Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        CircleAvatar(
                          radius: 25,
                          backgroundImage:
                              ContactService().elementAt(i).photoOrThumbnail !=
                                      null
                                  ? MemoryImage(ContactService()
                                      .elementAt(i)
                                      .photoOrThumbnail!)
                                  : null,
                          child: ContactService()
                                      .elementAt(i)
                                      .photoOrThumbnail ==
                                  null
                              ? Icon(
                                  Icons.person,
                                  size: 25,
                                  color: context.colorScheme.onPrimaryContainer,
                                )
                              : null,
                        ),
                        const SizedBox(width: 10),
                        Text(ContactService().elementAt(i).displayName,
                            style: GoogleFonts.cabin(
                              fontSize: 16,
                              color: context.colorScheme.onSurface,
                            )),
                      ],
                    ),
                    onTap: () async {
                      await Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) =>
                              ContactInfoView(ContactService().elementAt(i))));
                    }));
          }
        });
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_contacts/contact.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/ui/contactinfo_view.dart';

class ContactsView extends StatefulWidget {
  const ContactsView({super.key});

  @override
  State<ContactsView> createState() => _ContactsViewState();
}

class _ContactsViewState extends State<ContactsView> {
  List<Contact>? _contacts;
  bool _permissionDenied = false;

  @override
  void initState() {
    super.initState();
    _fetchContacts();
  }

  Future _fetchContacts() async {
    if (await Permission.contacts.request().isGranted) {
      final contacts = await FlutterContacts.getContacts(
        withProperties: true,
        withAccounts: true,
        withGroups: true,
        withPhoto: true,
        withThumbnail: true,
      );
      setState(() => _contacts = contacts);
    } else {
      setState(() => _permissionDenied = true);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_permissionDenied) {
      return Center(
          child: Text(
        'Permission denied',
        style: GoogleFonts.cabin(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: context.colorScheme.onSurface,
        ),
      ));
    }
    if (_contacts == null) return Center(child: CircularProgressIndicator());
    return ListView.builder(
        itemCount: _contacts!.length,
        itemBuilder: (context, i) => ListTile(
            title: Wrap(
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundImage: _contacts![i].photoOrThumbnail != null
                      ? MemoryImage(_contacts![i].photoOrThumbnail!)
                      : null,
                  child: _contacts![i].photoOrThumbnail == null
                      ? Icon(
                          Icons.person,
                          size: 20,
                          color: context.colorScheme.onPrimaryContainer,
                        )
                      : null,
                ),
                const SizedBox(width: 10),
                Text(_contacts![i].displayName,
                    style: GoogleFonts.cabin(
                      fontSize: 16,
                      color: context.colorScheme.onSurface,
                    )),
              ],
            ),
            onTap: () async {
              await Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => ContactInfoView(_contacts![i])));
            }));
  }
}

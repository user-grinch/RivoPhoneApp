import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/cubit/contact_service.dart';
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
    return BlocBuilder<ContactService, List<Contact>>(
      builder: (context, state) {
        if (state.isEmpty) {
          return CenterText(text: 'No contacts found');
        }
        return Scrollbar(
          trackVisibility: true,
          thickness: 2.5,
          interactive: true,
          radius: Radius.circular(30),
          controller: _controller,
          child: ListView.builder(
            itemCount: state.length,
            controller: _controller,
            itemBuilder: (context, i) => _displayContact(context, state, i),
          ),
        );
      },
    );
  }

  bool _shouldShowHeader(List<Contact> contacts, int i) {
    return i == 0 ||
        contacts[i].fullName.isNotEmpty &&
            contacts[i].fullName[0] != contacts[i - 1].fullName[0];
  }

  Widget _displayContact(
    BuildContext context,
    List<Contact> contacts,
    int i,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_shouldShowHeader(contacts, i))
          Padding(
            padding: EdgeInsets.fromLTRB(30, i == 0 ? 10 : 50, 0, 0),
            child: Text(
              contacts[i].fullName[0],
              style: GoogleFonts.cabin(
                fontSize: 20,
                color: context.colorScheme.onSurface.withAlpha(200),
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
                Text(contacts[i].displayName,
                    style: GoogleFonts.cabin(
                      fontSize: 16,
                      color: context.colorScheme.onSurface,
                    )),
              ],
            ),
            onTap: () async {
              await Navigator.of(context).pushNamed(
                contactInfoRoute,
                arguments: contacts[i],
              );
            }),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/circle_profile.dart';
import 'package:sticky_az_list/sticky_az_list.dart';

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

  Future<void> _refreshContacts(BuildContext context) async {
    context.read<ContactService>().refresh();
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ContactService, List<Contact>>(
      builder: (context, state) {
        if (state.isEmpty) {
          return RefreshIndicator(
            onRefresh: () => _refreshContacts(context),
            child: ListView(
              physics: AlwaysScrollableScrollPhysics(),
              children: [
                CenterText(text: 'No contacts found'),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () => _refreshContacts(context),
          child: StickyAzList(
            controller: _controller,
            items: state,
            builder: (context, index, item) => Builder(builder: (context) {
              return _displayContact(context, item);
            }),
            options: StickyAzOptions(
                scrollBarOptions: ScrollBarOptions(
                    // TODO: Fix look on AMOLED dark mode
                    ),
                listOptions: ListOptions(
                  listHeaderBuilder: (context, symbol) => Container(
                    margin: EdgeInsets.only(top: 30, bottom: 10),
                    padding: EdgeInsets.fromLTRB(30, 10, 0, 10),
                    decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(15),
                        color: Theme.of(context).colorScheme.surface),
                    child: Text(
                      symbol,
                      style: GoogleFonts.raleway(
                        fontSize: 20,
                        color: context.colorScheme.onSurface.withAlpha(200),
                      ),
                    ),
                  ),
                )),
          ),
          // Scrollbar(
          //   trackVisibility: true,
          //   thickness: 2.5,
          //   interactive: true,
          //   radius: Radius.circular(30),
          //   controller: _controller,
          //   child: ListView.builder(
          //     itemCount: state.length,
          //     controller: _controller,
          //     physics: AlwaysScrollableScrollPhysics(),
          //     itemBuilder: (context, i) => _displayContact(context, state, i),
          //   ),
          // ),
        );
      },
    );
  }

  Widget _displayContact(BuildContext context, Contact contact) => ListTile(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        title: Row(
          children: [
            const SizedBox(width: 10),
            CircleProfile(
              name: contact.displayName,
              profile: contact.photo,
              size: 30,
            ),
            const SizedBox(width: 10),
            Flexible(
              child: Text(
                contact.displayName,
                style: GoogleFonts.raleway(
                  fontSize: 16,
                  color: context.colorScheme.onSurface,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        onTap: () async {
          await Navigator.of(context).pushNamed(
            contactInfoRoute,
            arguments: contact,
          );
        },
      );
}

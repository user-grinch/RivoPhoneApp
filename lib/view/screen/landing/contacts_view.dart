import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/utils/center_text.dart';
import 'package:revo/view/utils/circle_profile.dart';
import 'package:sticky_az_list/sticky_az_list.dart';

class ContactsView extends ConsumerStatefulWidget {
  const ContactsView({super.key});

  @override
  ConsumerState<ContactsView> createState() => _ContactsViewState();
}

class _ContactsViewState extends ConsumerState<ContactsView> {
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
    await Future.delayed(const Duration(seconds: 2));
    ref.read(contactServiceProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    final contacts = ref.watch(contactServiceProvider);

    return contacts.when(
      data: (v) {
        if (v.isEmpty) {
          return ExpressiveRefreshIndicator(
            onRefresh: () => _refreshContacts(context),
            child: ListView(
              physics: AlwaysScrollableScrollPhysics(),
              children: [
                CenterText(text: 'No contacts found'),
              ],
            ),
          );
        }

        return ExpressiveRefreshIndicator(
          onRefresh: () => _refreshContacts(context),
          child: StickyAzList(
            controller: _controller,
            items: v,
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
                      style: GoogleFonts.roboto(
                        fontSize: 20,
                        color: context.colorScheme.onSurface.withAlpha(200),
                      ),
                    ),
                  ),
                )),
          ),
        );
      },
      loading: () => Center(
        child: const ExpressiveLoadingIndicator(),
      ),
      error: (e, s) => Center(
        child: const Text("Error occured"),
      ),
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
                style: GoogleFonts.roboto(
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

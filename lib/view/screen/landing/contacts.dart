import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/components/center_text.dart';
import 'package:revo/view/components/circle_profile.dart';
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

  @override
  Widget build(BuildContext context) {
    final contactsAsync = ref.watch(contactServiceProvider);
    final colorScheme = context.colorScheme;

    return contactsAsync.when(
      loading: () => const Center(child: ExpressiveLoadingIndicator()),
      error: (e, s) => const Center(child: Text("Error occurred")),
      data: (v) {
        if (v.isEmpty) {
          return ExpressiveRefreshIndicator(
            onRefresh: () async =>
                ref.read(contactServiceProvider.notifier).refresh(),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              children: const [
                SizedBox(height: 100),
                CenterText(text: 'No contacts found'),
              ],
            ),
          );
        }

        return ExpressiveRefreshIndicator(
          onRefresh: () async =>
              ref.read(contactServiceProvider.notifier).refresh(),
          child: StickyAzList(
            controller: _controller,
            items: v,
            builder: (context, index, item) {
              final contact = item as Contact;
              final String currentSymbol = contact.name[0].toUpperCase();

              bool isFirstInSection = index == 0 ||
                  v[index - 1].name[0].toUpperCase() != currentSymbol;
              bool isLastInSection = index == v.length - 1 ||
                  v[index + 1].name[0].toUpperCase() != currentSymbol;

              return Padding(
                padding: const EdgeInsets.fromLTRB(10, 0, 0, 0),
                child: Column(
                  children: [
                    Container(
                      decoration: BoxDecoration(
                        color: colorScheme.secondaryContainer.withOpacity(0.35),
                        borderRadius: BorderRadius.vertical(
                          top: isFirstInSection
                              ? const Radius.circular(28)
                              : Radius.zero,
                          bottom: isLastInSection
                              ? const Radius.circular(28)
                              : Radius.zero,
                        ),
                      ),
                      child: Column(
                        children: [
                          ListTile(
                            onTap: () => Navigator.of(context).pushNamed(
                                contactInfoRoute,
                                arguments: contact),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.vertical(
                                top: isFirstInSection
                                    ? const Radius.circular(28)
                                    : Radius.zero,
                                bottom: isLastInSection
                                    ? const Radius.circular(28)
                                    : Radius.zero,
                              ),
                            ),
                            contentPadding: const EdgeInsets.symmetric(
                                horizontal: 12, vertical: 12),
                            leading: CircleProfile(
                              name: contact.name,
                              profile: contact.photo,
                              size: 28,
                            ),
                            title: Text(
                              contact.name,
                              style: GoogleFonts.outfit(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: colorScheme.onSurface,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          if (!isLastInSection)
                            Padding(
                              padding:
                                  const EdgeInsets.only(left: 72, right: 16),
                              child: Divider(
                                height: 1,
                                thickness: 1,
                                color:
                                    colorScheme.outlineVariant.withOpacity(0.2),
                              ),
                            ),
                        ],
                      ),
                    ),
                    if (isLastInSection) const SizedBox(height: 12),
                  ],
                ),
              );
            },
            options: StickyAzOptions(
              scrollBarOptions: ScrollBarOptions(
                showDeactivated: false,
                margin: const EdgeInsets.only(top: 30, bottom: 20, left: 5),
                padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 4),
                scrollable: true,
                symbolBuilder: (context, val, state) {
                  final isActive = state == ScrollbarItemState.active;
                  return Container(
                    alignment: Alignment.center,
                    width: 20,
                    height: 20,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color:
                          isActive ? colorScheme.primary : Colors.transparent,
                    ),
                    child: Text(
                      val,
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight:
                            isActive ? FontWeight.bold : FontWeight.normal,
                        color: isActive
                            ? colorScheme.onPrimary
                            : colorScheme.onSurfaceVariant.withOpacity(0.5),
                      ),
                    ),
                  );
                },
              ),
              listOptions: ListOptions(
                stickySectionHeader: false,
                backgroundColor: Colors.transparent,
                headerColor: Colors.transparent,
                listHeaderBuilder: (context, symbol) => Padding(
                  padding: const EdgeInsets.fromLTRB(14, 12, 0, 8),
                  child: Text(
                    symbol,
                    style: GoogleFonts.outfit(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: colorScheme.primary,
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

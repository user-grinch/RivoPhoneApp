import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
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
    final contacts = ref.watch(contactServiceProvider);
    final colorScheme = context.colorScheme;

    return contacts.when(
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
              return Padding(
                padding: const EdgeInsets.fromLTRB(20, 4, 0, 4),
                child: Container(
                  decoration: BoxDecoration(
                    color: colorScheme.secondaryContainer.withOpacity(0.35),
                    borderRadius: BorderRadius.circular(28),
                  ),
                  child: ListTile(
                    onTap: () => Navigator.of(context)
                        .pushNamed(contactInfoRoute, arguments: item),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(28)),
                    contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16, vertical: 12),
                    leading: CircleProfile(
                      name: item.displayName,
                      profile: item.photo,
                      size: 28,
                    ),
                    title: Text(
                      item.displayName,
                      style: GoogleFonts.outfit(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: colorScheme.onSurface,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
              );
            },
            options: StickyAzOptions(
              scrollBarOptions: ScrollBarOptions(
                margin: const EdgeInsets.only(top: 30, bottom: 20, left: 16),
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                scrollable: true,
                symbolBuilder: (context, val, state) {
                  final isActive = state == ScrollbarItemState.active;
                  return Container(
                    alignment: Alignment.center,
                    width: 24,
                    height: 24,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color:
                          isActive ? colorScheme.primary : Colors.transparent,
                    ),
                    child: Text(
                      val,
                      style: TextStyle(
                        fontSize: 11,
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
                backgroundColor: Colors.transparent,
                headerColor: Colors.transparent,
                listHeaderBuilder: (context, symbol) => Text(
                  symbol,
                  style: GoogleFonts.outfit(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: colorScheme.primary,
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

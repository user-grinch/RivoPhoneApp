import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/view/components/contact_tile.dart';

class MatchedView extends ConsumerWidget {
  final ScrollController scrollController;
  final String searchText;

  const MatchedView({
    super.key,
    required this.scrollController,
    required this.searchText,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colorScheme = context.colorScheme;
    final contacts = ref
        .watch(contactServiceProvider.notifier)
        .findAllByNameOrNumber(searchText, searchText);

    if (contacts.isEmpty) {
      if (searchText.isEmpty) return const SizedBox.shrink();
      return Center(
        child: Text(
          "No contacts found",
          style: GoogleFonts.outfit(
            color: colorScheme.onSurfaceVariant.withOpacity(0.5),
          ),
        ),
      );
    }

    return Scrollbar(
      controller: scrollController,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 20),
        itemCount: contacts.length,
        controller: scrollController,
        itemBuilder: (context, i) {
          return ContactTile(
            contact: contacts[i],
          );
        },
      ),
    );
  }
}

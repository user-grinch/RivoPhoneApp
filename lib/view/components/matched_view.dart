import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/view/components/contact_tile.dart';
import 'package:revo/view/components/scroll_to_top.dart';

class MatchedView extends ConsumerWidget {
  final ScrollController scrollController;
  final String searchText;

  const MatchedView({
    super.key,
    required this.scrollController,
    required this.searchText,
  });

  Widget _buildEmpty(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              decoration: BoxDecoration(
                color: colorScheme.surfaceVariant.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              padding: const EdgeInsets.all(24),
              child: Icon(
                Icons.phone_outlined,
                size: 48,
                color: colorScheme.primary,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              "No contacts found",
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              "Use the keypad to dial a number",
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colorScheme.onSurfaceVariant.withOpacity(0.7),
                  ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contacts = ref
        .watch(contactServiceProvider.notifier)
        .fuzzyFindByNameOrNumber(searchText, searchText);

    if (contacts.isEmpty) {
      if (searchText.isEmpty) return const SizedBox.shrink();
      return _buildEmpty(context);
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

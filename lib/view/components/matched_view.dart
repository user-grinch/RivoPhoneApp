import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/view/components/contact_tile.dart';
import 'package:revo/view/components/empty_view.dart';
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
    return EmptyView(
      icon: Icons.phone_outlined,
      title: 'No contacts found',
      subtitle: 'Use the keypad to dial a number',
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

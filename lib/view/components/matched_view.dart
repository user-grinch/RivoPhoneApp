import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
    final contacts = ref
        .watch(contactServiceProvider.notifier)
        .findAllByNameOrNumber(searchText, searchText);

    if (contacts.isNotEmpty) {
      return Scrollbar(
        controller: scrollController,
        child: ListView.builder(
          padding: const EdgeInsets.only(top: 20.0),
          shrinkWrap: true,
          itemCount: contacts.length,
          controller: scrollController,
          itemBuilder: (context, i) {
            return ContactTile(contact: contacts[i]);
          },
        ),
      );
    } else {
      return Center(child: const Text("No contacts found"));
    }
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/view/components/contact_tile.dart';

class MatchedView extends ConsumerWidget {
  final ScrollController scrollController;
  final String number;

  const MatchedView({
    super.key,
    required this.scrollController,
    required this.number,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contacts = ref.watch(contactServiceProvider);

    return contacts.when(
      data: (value) {
        return Scrollbar(
          controller: scrollController,
          child: ListView.builder(
            padding: const EdgeInsets.only(top: 20.0),
            shrinkWrap: true,
            itemCount: value.length,
            controller: scrollController,
            itemBuilder: (context, i) {
              return ContactTile(contact: value[i]);
            },
          ),
        );
      },
      loading: () => Center(
        child: CircularProgressIndicator(),
      ),
      error: (e, s) => Center(
        child: const Text("Error occured"),
      ),
    );
  }
}

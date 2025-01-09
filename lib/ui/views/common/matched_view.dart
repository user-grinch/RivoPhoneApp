import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/views/common/contact_tile.dart';

class MatchedView extends StatelessWidget {
  final ScrollController scrollController;
  final String number;

  const MatchedView({
    super.key,
    required this.scrollController,
    required this.number,
  });

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ContactService, List<Contact>>(
      builder: (context, state) {
        var contacts = context
            .read<ContactService>()
            .findAllByNameOrNumber(number, number);

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
      },
    );
  }
}

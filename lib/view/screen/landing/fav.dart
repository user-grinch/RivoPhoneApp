import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/screen/contactinfo_view.dart';
import 'package:revo/view/utils/center_text.dart';
import 'package:revo/view/utils/circle_profile.dart';

class FavView extends ConsumerWidget {
  const FavView({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contacts = ref.watch(contactServiceProvider);
    return contacts.when(
      data: (v) {
        final starred =
            ref.watch(contactServiceProvider.notifier).filterByStars();

        if (starred.isEmpty) {
          return CenterText(text: 'No favorite contacts found');
        }

        return Padding(
          padding: const EdgeInsets.only(top: 30.0),
          child: GridView.builder(
            padding: const EdgeInsets.all(8.0),
            shrinkWrap: true,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 16.0,
              mainAxisSpacing: 16.0,
              childAspectRatio: 0.8,
            ),
            itemCount: starred.length,
            itemBuilder: (context, i) {
              return _buildFavs(context, starred[i]);
            },
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

  Widget _buildFavs(BuildContext context, Contact contact) {
    return InkWell(
      borderRadius: BorderRadius.all(Radius.circular(15)),
      onTap: () {
        Navigator.of(context)
            .push(MaterialPageRoute(builder: (_) => ContactInfoView(contact)));
      },
      child: Column(
        children: [
          CircleProfile(
            name: contact.displayName,
            profile: contact.photo,
            size: 45,
          ),
          const SizedBox(height: 10),
          Flexible(
            child: Center(
              child: Text(
                contact.displayName,
                textAlign: TextAlign.center,
                style: GoogleFonts.outfit(
                  color: context.colorScheme.onSurface,
                  fontSize: 14,
                ),
                overflow: TextOverflow.ellipsis,
                maxLines: 2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

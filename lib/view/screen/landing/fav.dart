import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/view/components/circle_profile.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/screen/contactinfo_view.dart';
import 'package:revo/view/utils/utils.dart';

class FavView extends ConsumerWidget {
  const FavView({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contacts = ref.watch(contactServiceProvider);
    final colorScheme = context.colorScheme;

    return contacts.when(
      data: (v) {
        final starred =
            ref.watch(contactServiceProvider.notifier).filterByStars();
        final simCards = ref.watch(getSimInfoProvider);

        if (starred.isEmpty) {
          return const Center(child: Text('No favorites pinned'));
        }

        return GridView.builder(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: 16.0,
            mainAxisSpacing: 16.0,
            childAspectRatio: 0.8,
          ),
          itemCount: starred.length,
          itemBuilder: (context, i) {
            final contact = starred[i];
            return Container(
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer.withOpacity(0.35),
                borderRadius: BorderRadius.circular(28),
              ),
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => ContactInfoView(contact)),
                  ),
                  borderRadius: BorderRadius.circular(28),
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      children: [
                        CircleProfile(
                          name: contact.displayName,
                          profile: contact.photo,
                          size: 40,
                        ),
                        const SizedBox(height: 12),
                        Wrap(
                          children: [
                            Text(
                              contact.displayName,
                              style: GoogleFonts.outfit(
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: colorScheme.onSurface,
                              ),
                              textAlign: TextAlign.center,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                        const Spacer(),
                        GestureDetector(
                          onTap: () {
                            hapticVibration();
                            simCards.whenData((value) => SimPicker(
                                  context: context,
                                  simCards: value,
                                  number: contact.phones.isNotEmpty
                                      ? contact.phones.first
                                      : '',
                                ).show());
                          },
                          child: Container(
                            height: 42,
                            width: double.infinity,
                            decoration: BoxDecoration(
                              color:
                                  colorScheme.primaryContainer.withAlpha(150),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(
                                  FluentIcons.call_20_filled,
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onPrimaryContainer,
                                  size: 18,
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  'Call',
                                  style: GoogleFonts.outfit(
                                    color: Theme.of(context)
                                        .colorScheme
                                        .onPrimaryContainer,
                                    fontSize: 14,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            );
          },
        );
      },
      loading: () => const Center(child: ExpressiveLoadingIndicator()),
      error: (e, s) => const Center(child: Text("Error occurred")),
    );
  }
}

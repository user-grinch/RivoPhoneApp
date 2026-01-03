import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/view/components/circle_profile.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/screen/contactinfo_view.dart';

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
          return _buildEmtyState(context);
        }

        return GridView.builder(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: 12.0,
            mainAxisSpacing: 12.0,
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
                          name: contact.name,
                          profile: contact.photo,
                          col: contact.color,
                          size: 40,
                        ),
                        const SizedBox(height: 6),
                        Wrap(
                          children: [
                            Text(
                              contact.name,
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
                        ActionIconButton(
                          FluentIcons.call_20_filled,
                          size: 50,
                          onPressed: () {
                            simCards.whenData((value) => SimPicker(
                                  context: context,
                                  simCards: value,
                                  number: contact.numbers.isNotEmpty
                                      ? contact.numbers.first.international
                                      : '',
                                ).show());
                          },
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

  Widget _buildEmtyState(BuildContext context) {
    final colorScheme = context.colorScheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 140,
              height: 140,
              decoration: BoxDecoration(
                color: colorScheme.surfaceContainerHigh,
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(56),
                  topRight: Radius.circular(24),
                  bottomLeft: Radius.circular(24),
                  bottomRight: Radius.circular(56),
                ),
              ),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  Icon(
                    FluentIcons.star_24_filled,
                    size: 100,
                    color: colorScheme.primary.withOpacity(0.1),
                  ),
                  Icon(
                    FluentIcons.star_24_regular,
                    size: 60,
                    color: colorScheme.primary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
            Text(
              "Lonely Circle",
              style: GoogleFonts.outfit(
                fontSize: 28,
                fontWeight: FontWeight.w800,
                letterSpacing: -0.5,
                color: colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              "Pin your favorite contacts here for\nlightning-fast access.",
              textAlign: TextAlign.center,
              style: GoogleFonts.outfit(
                fontSize: 16,
                height: 1.4,
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

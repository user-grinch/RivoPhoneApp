import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/ui/views/common/constants.dart';
import 'package:revo/utils/menu_tile.dart';
import 'package:revo/utils/utils.dart';

class AboutView extends StatelessWidget {
  const AboutView({super.key});

  final String githubUrl = "https://github.com/user-grinch/Rivo";
  final String patreonUrl = "https://www.patreon.com/grinch_";

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'About',
          style: GoogleFonts.raleway(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: context.colorScheme.onSurface,
          ),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Image.asset(
              'assets/icon.png',
              width: 100,
              height: 100,
              fit: BoxFit.cover,
            ),
            const SizedBox(height: 12),
            Text(
              'Rivo',
              style: GoogleFonts.raleway(
                fontSize: 26,
                fontWeight: FontWeight.w700,
                color: context.colorScheme.onBackground,
              ),
            ),
            const SizedBox(height: 20),
            Container(
              decoration: BoxDecoration(
                color: Theme.of(context)
                    .colorScheme
                    .secondaryContainer
                    .withAlpha(110),
                borderRadius: BorderRadius.all(
                  Radius.circular(15),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'About the App',
                      style: GoogleFonts.raleway(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: context.colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Rivo is a modern dialer app that brings simplicity and elegance to calling. '
                      'Designed with Material You, it adapts seamlessly to your theme while ensuring a smooth and intuitive experience.',
                      style: GoogleFonts.raleway(
                        fontSize: 15,
                        color: context.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            MenuTile(
              title: 'Author',
              subtitle: 'Grinch_',
              icon: HugeIcons.strokeRoundedUser,
              onTap: () {},
              isFirst: true,
            ),
            MenuTile(
              title: 'Version',
              subtitle: version,
              icon: HugeIcons.strokeRoundedInformationCircle,
              onTap: () {},
              isLast: true,
            ),
            const SizedBox(height: 10),
            MenuTile(
              title: 'Source Code',
              subtitle: 'View the source code on GitHub',
              icon: HugeIcons.strokeRoundedGithub01,
              onTap: () async =>
                  await launchURL('https://github.com/user-grinch/Rivo'),
              isFirst: true,
            ),
            MenuTile(
              title: 'Support Us on Patreon',
              subtitle: 'Contribute to our development',
              icon: HugeIcons.strokeRoundedFavourite,
              onTap: () async =>
                  await launchURL('https://www.patreon.com/grinch_'),
              isLast: true,
            ),
          ],
        ),
      ),
    );
  }
}

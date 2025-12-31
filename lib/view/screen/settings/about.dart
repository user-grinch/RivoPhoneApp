import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/ui.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/screen/settings/contributors.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/utils/utils.dart';

class AboutView extends StatelessWidget {
  const AboutView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const RoundedIconButton(FluentIcons.arrow_left_24_regular),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'About',
          style: GoogleFonts.outfit(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: context.colorScheme.onSurface,
          ),
        ),
      ),
      body: SingleChildScrollView(
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
              style: GoogleFonts.outfit(
                fontSize: 26,
                fontWeight: FontWeight.w700,
                color: context.colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 20),

            // App Description
            Container(
              decoration: BoxDecoration(
                color: Theme.of(context)
                    .colorScheme
                    .secondaryContainer
                    .withAlpha(110),
                borderRadius: const BorderRadius.all(Radius.circular(20)),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'About the App',
                      style: GoogleFonts.outfit(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: context.colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Rivo is a modern dialer app that brings simplicity and elegance to calling. '
                      'Designed with Material You, it adapts seamlessly to your theme.',
                      style: GoogleFonts.outfit(
                        fontSize: 15,
                        color: context.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            MenuTile(
              title: 'Version',
              subtitle: version,
              icon: FluentIcons.info_24_regular,
              onTap: () {},
              isFirst: true,
            ),
            MenuTile(
              title: 'Build number',
              subtitle: buildNumber,
              icon: FluentIcons.number_symbol_24_regular,
              onTap: () {},
              isLast: true,
            ),
            const SizedBox(height: 20),

            MenuTile(
              title: 'Contributors',
              subtitle: 'Meet the team behind the project',
              icon: FluentIcons.people_community_24_regular,
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => const ContributorsView()),
                );
              },
              isFirst: true,
            ),
            MenuTile(
              title: 'Support Us on Patreon',
              subtitle: 'Help keep the project alive',
              icon: FluentIcons.heart_24_regular,
              onTap: () async => await launchURL(patreonUrl),
              isLast: true,
            ),
            const SizedBox(height: 20),

            // Source Code
            MenuTile(
              title: 'Source Code',
              subtitle: 'View the source code on GitHub',
              icon: FontAwesomeIcons.github,
              onTap: () async => await launchURL(githubUrl),
              isFirst: true,
              isLast: true,
            ),
          ],
        ),
      ),
    );
  }
}

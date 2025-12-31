import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/utils/utils.dart';

class Contributor {
  final String name;
  final String role;
  final String? githubUrl;

  const Contributor({
    required this.name,
    required this.role,
    this.githubUrl,
  });
}

const List<Contributor> appContributors = [
  Contributor(
    name: 'Grinch_',
    role: 'Lead Developer',
    githubUrl: 'https://github.com/user-grinch',
  ),
  Contributor(
    name: 'RoBoT_095',
    role: 'Developer',
    githubUrl: 'https://github.com/RoBoT095',
  ),
  Contributor(
    name: 'UniconLabs',
    role: 'App icon',
    githubUrl: 'https://www.flaticon.com/authors/uniconlabs',
  ),
];

class ContributorsView extends StatelessWidget {
  const ContributorsView({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        centerTitle: true,
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'Contributors',
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.bold,
            color: colorScheme.onSurface,
          ),
        ),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
        itemCount: appContributors.length,
        separatorBuilder: (context, index) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final contributor = appContributors[index];
          final bool hasLink = contributor.githubUrl != null;

          return Container(
            decoration: BoxDecoration(
              color: colorScheme.secondaryContainer.withOpacity(0.35),
              borderRadius: BorderRadius.circular(28),
            ),
            child: ListTile(
              onTap: hasLink ? () => launchURL(contributor.githubUrl!) : null,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(28)),
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              leading: _buildContributorSquircle(context, contributor),
              title: Text(
                contributor.name,
                style: GoogleFonts.outfit(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                  color: colorScheme.onSurface,
                ),
              ),
              subtitle: Text(
                contributor.role,
                style: GoogleFonts.outfit(
                  fontSize: 13,
                  color: colorScheme.onSurfaceVariant.withOpacity(0.8),
                ),
              ),
              trailing: hasLink
                  ? Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: colorScheme.surface.withOpacity(0.5),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Icon(
                        FluentIcons.open_24_filled,
                        size: 18,
                        color: colorScheme.primary,
                      ),
                    )
                  : null,
            ),
          );
        },
      ),
    );
  }

  Widget _buildContributorSquircle(
      BuildContext context, Contributor contributor) {
    final colorScheme = context.colorScheme;

    return Container(
      width: 56,
      height: 56,
      decoration: BoxDecoration(
          color: colorScheme.primaryContainer,
          borderRadius: BorderRadius.circular(16)),
      alignment: Alignment.center,
      child: Text(
        contributor.name[0].toUpperCase(),
        style: GoogleFonts.outfit(
          color: colorScheme.onPrimaryContainer,
          fontSize: 24,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

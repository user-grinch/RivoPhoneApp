import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

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
      appBar: AppBarComponent("Contributors"),
      body: ListView.builder(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
        itemCount: appContributors.length,
        itemBuilder: (context, index) {
          final contributor = appContributors[index];
          final bool hasLink = contributor.githubUrl != null;
          final bool isFirst = index == 0;
          final bool isLast = index == appContributors.length - 1;

          return Column(
            children: [
              Container(
                decoration: BoxDecoration(
                  color: colorScheme.secondaryContainer.withOpacity(0.35),
                  borderRadius: BorderRadius.vertical(
                    top: isFirst ? const Radius.circular(28) : Radius.zero,
                    bottom: isLast ? const Radius.circular(28) : Radius.zero,
                  ),
                ),
                child: Column(
                  children: [
                    ListTile(
                      onTap: hasLink
                          ? () => launchURL(contributor.githubUrl!)
                          : null,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.vertical(
                          top:
                              isFirst ? const Radius.circular(28) : Radius.zero,
                          bottom:
                              isLast ? const Radius.circular(28) : Radius.zero,
                        ),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 12),
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
                    if (!isLast)
                      Padding(
                        padding: const EdgeInsets.only(left: 88.0, right: 16.0),
                        child: Divider(
                          height: 1,
                          thickness: 1,
                          color: colorScheme.outlineVariant.withOpacity(0.2),
                        ),
                      ),
                  ],
                ),
              ),
            ],
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
          color: colorScheme.primaryContainer.withOpacity(0.4),
          borderRadius: BorderRadius.circular(20)),
      alignment: Alignment.center,
      child: Text(
        contributor.name[0].toUpperCase(),
        style: GoogleFonts.outfit(
          color: colorScheme.primary,
          fontSize: 24,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

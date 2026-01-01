import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';

class NavigationView extends StatefulWidget {
  final PageController pageController;

  const NavigationView({super.key, required this.pageController});

  @override
  State<NavigationView> createState() => _NavigationViewState();
}

class _NavigationViewState extends State<NavigationView> {
  int _selectedIndex = 0;

  @override
  void initState() {
    widget.pageController.addListener(() {
      final currentPage = widget.pageController.page?.round() ?? 0;
      if (_selectedIndex != currentPage) {
        setState(() => _selectedIndex = currentPage);
      }
    });

    SharedPrefService().onPreferenceChanged.listen((key) {
      if (key == PREF_ICON_ONLY_BOTTOMSHEET ||
          key == PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET) {
        if (mounted) setState(() {});
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    bool onlyIcons = SharedPrefService().getBool(PREF_ICON_ONLY_BOTTOMSHEET);
    bool alwaysSelectedIcon =
        SharedPrefService().getBool(PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET);

    return NavigationBarTheme(
      data: NavigationBarThemeData(
        height: 84,
        indicatorShape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
        indicatorColor: colorScheme.primary.withOpacity(0.12),
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          return GoogleFonts.outfit(
            fontSize: 12,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w700
                : FontWeight.w500,
            color: states.contains(WidgetState.selected)
                ? colorScheme.primary
                : colorScheme.onSurfaceVariant.withOpacity(0.8),
          );
        }),
      ),
      child: Container(
        decoration: BoxDecoration(
          color: colorScheme.secondaryContainer.withOpacity(0.4),
          border: Border(
            top: BorderSide(
              color: colorScheme.outlineVariant.withOpacity(0.2),
              width: 1,
            ),
          ),
        ),
        child: NavigationBar(
          backgroundColor: Colors.transparent,
          elevation: 0,
          selectedIndex: _selectedIndex,
          labelBehavior: onlyIcons
              ? alwaysSelectedIcon
                  ? NavigationDestinationLabelBehavior.onlyShowSelected
                  : NavigationDestinationLabelBehavior.alwaysHide
              : NavigationDestinationLabelBehavior.alwaysShow,
          onDestinationSelected: (index) {
            widget.pageController.animateToPage(
              index,
              duration: const Duration(milliseconds: 500),
              curve: Curves.easeInOutCubicEmphasized,
            );
          },
          destinations: [
            _buildDestination(FluentIcons.history_24_regular,
                FluentIcons.history_24_filled, 'Recents'),
            _buildDestination(FluentIcons.person_24_regular,
                FluentIcons.person_24_filled, 'Contacts'),
            _buildDestination(FluentIcons.star_24_regular,
                FluentIcons.star_24_filled, 'Favorites'),
          ],
        ),
      ),
    );
  }

  NavigationDestination _buildDestination(
      IconData icon, IconData active, String label) {
    return NavigationDestination(
      icon: Icon(icon, size: 24),
      selectedIcon: Icon(active, size: 24, color: context.colorScheme.primary),
      label: label,
    );
  }
}

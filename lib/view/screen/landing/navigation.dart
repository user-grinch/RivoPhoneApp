import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
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
      setState(() {
        _selectedIndex = widget.pageController.page?.round() ?? 0;
      });
    });
    SharedPrefService().onPreferenceChanged.listen((key) {
      if (key == PREF_ICON_ONLY_BOTTOMSHEET ||
          key == PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET) {
        setState(() {});
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    bool onlyIcons = SharedPrefService().getBool(PREF_ICON_ONLY_BOTTOMSHEET);
    bool alwaysSelectedIcon =
        SharedPrefService().getBool(PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET);
    return NavigationBar(
      backgroundColor: context.colorScheme.surface,
      elevation: 3,
      indicatorColor: context.colorScheme.secondaryContainer,
      surfaceTintColor: context.colorScheme.surfaceTint,
      labelBehavior: onlyIcons
          ? alwaysSelectedIcon
              ? NavigationDestinationLabelBehavior.onlyShowSelected
              : NavigationDestinationLabelBehavior.alwaysHide
          : NavigationDestinationLabelBehavior.alwaysShow,
      destinations: [
        NavigationDestination(
          icon: Icon(FluentIcons.history_24_regular),
          label: 'Recents',
          selectedIcon: Icon(FluentIcons.history_24_filled),
        ),
        NavigationDestination(
          icon: Icon(FluentIcons.person_24_regular),
          label: 'Contacts',
          selectedIcon: Icon(FluentIcons.person_24_filled),
        ),
        NavigationDestination(
          icon: Icon(FluentIcons.star_24_regular),
          label: 'Favorites',
          selectedIcon: Icon(FluentIcons.star_24_filled),
        ),
      ],
      onDestinationSelected: (index) {
        setState(() {
          _selectedIndex = index;
        });
        widget.pageController.animateToPage(
          index,
          duration: Duration(milliseconds: 250),
          curve: Curves.easeInOut,
        );
      },
      selectedIndex: _selectedIndex,
    );
  }
}

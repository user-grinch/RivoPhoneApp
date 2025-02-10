import 'package:flutter/material.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/prefservice.dart';

class NavigationView extends StatefulWidget {
  final PageController pageController;

  const NavigationView({super.key, required this.pageController});

  @override
  State<NavigationView> createState() => _NavigationViewState();
}

class _NavigationViewState extends State<NavigationView> {
  int _selectedIndex = 0;
  bool _prevFlag = false;

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
          icon: Icon(HugeIcons.strokeRoundedClock01),
          label: 'Recents',
          selectedIcon: Icon(HugeIcons.strokeRoundedClock01),
        ),
        NavigationDestination(
          icon: Icon(HugeIcons.strokeRoundedUser),
          label: 'Contacts',
          selectedIcon: Icon(HugeIcons.strokeRoundedUser),
        ),
        NavigationDestination(
          icon: Icon(HugeIcons.strokeRoundedFavourite),
          label: 'Favorites',
          selectedIcon: Icon(HugeIcons.strokeRoundedFavourite),
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

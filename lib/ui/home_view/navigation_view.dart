import 'package:flutter/material.dart';
import 'package:revo/extentions/theme.dart';

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
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      backgroundColor: context.colorScheme.surface,
      elevation: 1,
      surfaceTintColor: context.colorScheme.surfaceTint,
      labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      destinations: [
        NavigationDestination(
          icon: Icon(Icons.phone_outlined),
          label: 'Recents',
          selectedIcon: Icon(Icons.phone),
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outlined),
          label: 'Contacts',
          selectedIcon: Icon(Icons.person),
        ),
        NavigationDestination(
          icon: Icon(Icons.star_outline),
          label: 'Favorites',
          selectedIcon: Icon(Icons.star),
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

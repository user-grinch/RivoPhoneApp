import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/constants/ui.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/controller/providers/theme_service.dart';
import 'package:revo/view/components/welcome_popup.dart';
import 'package:revo/view/screen/landing/appbar.dart';
import 'package:revo/view/screen/landing/contacts.dart';
import 'package:revo/view/screen/landing/fav.dart';
import 'package:revo/view/screen/landing/navigation.dart';
import 'package:revo/view/screen/landing/recents.dart';

class HomeView extends ConsumerStatefulWidget {
  const HomeView({super.key});

  @override
  ConsumerState<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends ConsumerState<HomeView> {
  late final PageController _pageController;
  int _currentPage = 0;

  @override
  void initState() {
    _pageController = PageController();
    _pageController.addListener(() {
      final pageIndex = _pageController.page?.round() ?? 0;
      if (pageIndex != _currentPage) {
        setState(() {
          _currentPage = pageIndex;
        });
      }
    });
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      Future.delayed(Duration(milliseconds: 100), () async {
        if (mounted) {
          await ref.watch(themeServiceProvider.notifier).initTheme();
          bool flag = SharedPrefService().getBool("WelcomeShown$buildNumber");
          // if (!flag && mounted) {
          WelcomePopup(
                  context: context,
                  changelog: changelog,
                  version: version,
                  buildNumber: buildNumber)
              .show();
          SharedPrefService().saveBool("WelcomeShown$buildNumber", true);
          // }
        }
      });
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBarView(),
      body: PageView(
        controller: _pageController,
        children: const [
          RecentsView(),
          ContactsView(),
          FavView(),
        ],
      ),
      bottomNavigationBar: NavigationView(pageController: _pageController),
      floatingActionButton: FloatingActionButton(
        backgroundColor: context.colorScheme.secondaryContainer,
        onPressed: () {
          if (_pageController.page == 1.0) {
            final service = ref.read(contactServiceProvider.notifier);
            service.createNewContact();
          } else {
            Navigator.of(context).pushNamed(dialpadRoute);
          }
        },
        elevation: 1,
        child: Icon(
          _currentPage == 1.0
              ? FluentIcons.person_add_24_regular
              : FluentIcons.dialpad_24_regular,
          size: 35,
        ),
      ),
    );
  }
}

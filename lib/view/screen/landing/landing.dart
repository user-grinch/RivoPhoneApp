import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:revo/constants/ui.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/controller/services/pref_service.dart';
import 'package:revo/controller/services/theme_service.dart';
import 'package:revo/router/router.dart';
import 'package:revo/view/components/welcome_popup.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/view/screen/landing/appbar.dart';
import 'package:revo/view/screen/landing/contacts.dart';
import 'package:revo/view/screen/landing/favourite.dart';
import 'package:revo/view/screen/landing/navigation.dart';
import 'package:revo/view/screen/landing/recents.dart';

class LandingScreen extends ConsumerStatefulWidget {
  const LandingScreen({super.key});

  @override
  ConsumerState<LandingScreen> createState() => _LandingScreenState();
}

class _LandingScreenState extends ConsumerState<LandingScreen> {
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
          if (!flag && mounted) {
            WelcomePopup(
                    context: context,
                    changelog: changelog,
                    version: version,
                    buildNumber: buildNumber)
                .show();
            SharedPrefService().saveBool("WelcomeShown$buildNumber", true);
          }
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
        heroTag: null,
        backgroundColor: context.colorScheme.secondaryContainer,
        foregroundColor: context.colorScheme.primary,
        onPressed: () {
          if (_pageController.page == 1.0) {
            final service = ref.read(contactServiceProvider.notifier);
            service.createNewContact();
          } else {
            gRouter.pushNamed(AppRoutes.dialpadRoute);
          }
        },
        elevation: 0,
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

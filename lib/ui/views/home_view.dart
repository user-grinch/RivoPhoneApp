import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/services/prefservice.dart';
import 'package:revo/ui/popups/welcome_changelog.dart';
import 'package:revo/ui/theme/handler.dart';
import 'package:revo/ui/views/common/constants.dart';
import 'package:revo/ui/views/home_view/appbar_view.dart';
import 'package:revo/ui/views/home_view/contacts_view.dart';
import 'package:revo/ui/views/home_view/fav_view.dart';
import 'package:revo/ui/views/home_view/navigation_view.dart';
import 'package:revo/ui/views/home_view/recents_view.dart';

class HomeView extends StatefulWidget {
  const HomeView({super.key});

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
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
        await SharedPrefService().init();
        bool flag = SharedPrefService().getBool("WelcomeShown$version");
        if (!flag) {
          showDialog(
            context: context,
            builder: (context) => welcomePopup(context, version, changelog),
          );
          SharedPrefService().saveBool("WelcomeShown$version", true);
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
    context.read<ThemeProvider>().initTheme();
    return Scaffold(
      appBar: AppBarView(),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8.0),
        child: PageView(
          controller: _pageController,
          children: const [
            RecentsView(),
            ContactsView(),
            FavView(),
          ],
        ),
      ),
      bottomNavigationBar: NavigationView(pageController: _pageController),
      floatingActionButton: FloatingActionButton(
        backgroundColor: context.colorScheme.secondaryContainer,
        onPressed: () {
          if (_pageController.page == 1.0) {
            context.read<ContactService>().createNewContact();
          } else {
            Navigator.of(context).pushNamed(dialpadRoute);
          }
        },
        elevation: 1,
        child: Icon(_currentPage == 1.0
            ? HugeIcons.strokeRoundedUserAdd01
            : HugeIcons.strokeRoundedDialpadCircle02),
      ),
    );
  }
}

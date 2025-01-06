import 'package:flutter/material.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/ui/home_view/appbar_view.dart';
import 'package:revo/ui/home_view/contacts_view.dart';
import 'package:revo/ui/home_view/fav_view.dart';
import 'package:revo/ui/home_view/navigation_view.dart';
import 'package:revo/ui/home_view/recents_view.dart';

class HomeView extends StatefulWidget {
  const HomeView({super.key});

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  late final PageController _pageController;

  @override
  void initState() {
    _pageController = PageController();
    super.initState();
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
        onPressed: () {
          Navigator.of(context).pushNamed(dialpadRoute);
        },
        elevation: 1,
        child: const Icon(Icons.dialpad),
      ),
    );
  }
}

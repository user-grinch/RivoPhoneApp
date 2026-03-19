import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';

import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/components/empty_view.dart';
import 'package:revo/view/components/matched_view.dart';
import 'package:revo/view/components/action_icon_btn.dart';
import 'package:revo/view/components/scroll_to_top.dart';

class SearchView extends StatefulWidget {
  const SearchView({super.key});

  @override
  State<SearchView> createState() => _SearchViewState();
}

class _SearchViewState extends State<SearchView> {
  late final TextEditingController _controller;
  String _searchQuery = '';
  late final FocusNode _focusNode;
  late ScrollController _scrollController;

  @override
  void initState() {
    _controller = TextEditingController();
    _focusNode = FocusNode();
    _scrollController = ScrollController();

    // Focus the search on page open
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
    });
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
              child: Container(
                height: 50,
                decoration: BoxDecoration(
                  color: colorScheme.secondaryContainer.withOpacity(0.35),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(
                  children: [
                    const SizedBox(width: 8),
                    ActionIconButton(
                      FluentIcons.arrow_left_24_regular,
                      size: 40,
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                    Expanded(
                      child: TextField(
                        controller: _controller,
                        focusNode: _focusNode,
                        onChanged: (query) =>
                            setState(() => _searchQuery = query),
                        style: GoogleFonts.outfit(
                          fontSize: 18,
                          fontWeight: FontWeight.w500,
                          color: colorScheme.onSurface,
                        ),
                        decoration: InputDecoration(
                          hintText: 'Search name or number...',
                          hintStyle: GoogleFonts.outfit(
                            color:
                                colorScheme.onSurfaceVariant.withOpacity(0.5),
                            fontWeight: FontWeight.w400,
                          ),
                          border: InputBorder.none,
                          contentPadding:
                              const EdgeInsets.symmetric(horizontal: 16),
                        ),
                      ),
                    ),
                    if (_searchQuery.isNotEmpty)
                      ActionIconButton(
                        FluentIcons.dismiss_24_regular,
                        size: 40,
                        onPressed: () {
                          _controller.clear();
                          setState(() => _searchQuery = '');
                        },
                      ),
                    const SizedBox(width: 8),
                  ],
                ),
              ),
            ),
            Expanded(
              child: _searchQuery.isEmpty
                  ? EmptyView(
                      icon: FluentIcons.search_24_regular,
                      title: 'Search Contacts',
                      subtitle: 'Start typing to find someone...',
                      iconTint: colorScheme.primary.withOpacity(0.3),
                    )
                  : Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Stack(
                        children: [
                          MatchedView(
                            searchText: _searchQuery,
                            scrollController: _scrollController,
                          ),
                          ScrollToTopButton(
                            controller: _scrollController,
                          ),
                        ],
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSquircleAction(
      {required IconData icon, required VoidCallback onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 48,
        height: 48,
        margin: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: context.colorScheme.surface.withOpacity(0.5),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Icon(icon, size: 22, color: context.colorScheme.primary),
      ),
    );
  }
}

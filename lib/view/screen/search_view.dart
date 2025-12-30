import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/components/matched_view.dart';

class SearchView extends StatefulWidget {
  const SearchView({super.key});

  @override
  State<SearchView> createState() => _SearchViewState();
}

class _SearchViewState extends State<SearchView> {
  late final TextEditingController _controller;
  String _searchQuery = '';

  late final ScrollController _scrollController;
  late final FocusNode _focusNode;

  @override
  void initState() {
    _controller = TextEditingController();
    _scrollController = ScrollController();
    _focusNode = FocusNode();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
    });
    super.initState();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(FluentIcons.arrow_left_24_regular),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: _buildSearchBox(),
        backgroundColor: context.colorScheme.surfaceTint.withAlpha(25),
        elevation: 0,
      ),
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.only(left: 16),
          child: MatchedView(
            scrollController: _scrollController,
            number: _searchQuery,
          ),
        ),
      ),
    );
  }

  Widget _buildSearchBox() {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(50),
      ),
      child: TextField(
        focusNode: _focusNode,
        controller: _controller,
        style: GoogleFonts.raleway(
          color: context.colorScheme.onSurface,
        ),
        decoration: InputDecoration(
          hintText: 'Search name/ number...',
          hintStyle: GoogleFonts.raleway(
            color: Colors.grey,
          ),
          border: InputBorder.none,
        ),
        onChanged: (query) {
          setState(() {
            _searchQuery = query;
          });
        },
      ),
    );
  }
}

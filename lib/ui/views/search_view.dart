import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/views/contactinfo_view.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/circle_profile.dart';

class SearchView extends StatefulWidget {
  const SearchView({super.key});

  @override
  _SearchViewState createState() => _SearchViewState();
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
          icon: Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
        title: _buildSearchBox(),
        backgroundColor: context.colorScheme.surfaceTint.withAlpha(25),
        elevation: 0,
      ),
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.only(left: 16),
          child: _searchQuery.isEmpty
              ? CenterText(text: 'Your results will appear here')
              : _buildSearchView(),
        ),
      ),
    );
  }

  Widget _buildSearchView() {
    return BlocBuilder<ContactService, List<Contact>>(
      builder: (context, state) {
        var contacts = context
            .read<ContactService>()
            .findAllByNameOrNumber(_searchQuery, _searchQuery);

        return Scrollbar(
          controller: _scrollController,
          child: ListView.builder(
            padding: const EdgeInsets.only(top: 20.0),
            shrinkWrap: true,
            itemCount: contacts.length,
            controller: _scrollController,
            itemBuilder: (context, i) {
              return _buildContact(
                context,
                contacts[i],
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildContact(
    BuildContext context,
    Contact contact,
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: ListTile(
        onTap: () async {
          await Navigator.of(context).push(MaterialPageRoute(
            builder: (_) => ContactInfoView(contact),
          ));
        },
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        leading: CircleProfile(
          name: contact.fullName,
          profile: contact.photo,
          size: 30,
        ),
        title: Text(
          contact.displayName,
          style: GoogleFonts.cabin(
            fontSize: 16,
            color: context.colorScheme.onSurface.withAlpha(200),
          ),
        ),
        subtitle: Text(
          contact.phones
              .toString()
              .substring(1, contact.phones.toString().length - 1),
          style: GoogleFonts.cabin(
            fontSize: 12,
            color: context.colorScheme.primary.withAlpha(200),
          ),
        ),
        trailing: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: context.colorScheme.primary.withAlpha(25),
            shape: BoxShape.circle,
          ),
          child: IconButton(
            onPressed: () async {},
            icon:
                Icon(Icons.call, color: context.colorScheme.primary, size: 25),
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
        style: GoogleFonts.cabin(
          color: context.colorScheme.onSurface,
        ),
        decoration: InputDecoration(
          hintText: 'Search contacts...',
          hintStyle: GoogleFonts.cabin(
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

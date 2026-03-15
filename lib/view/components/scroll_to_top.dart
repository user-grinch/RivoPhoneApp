import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:m3e_collection/m3e_collection.dart';

class ScrollToTopButton extends StatefulWidget {
  final ScrollController controller;
  final double showOffset;
  final double bottomPadding;

  const ScrollToTopButton({
    Key? key,
    required this.controller,
    this.showOffset = 100,
    this.bottomPadding = 10,
  }) : super(key: key);

  @override
  State<ScrollToTopButton> createState() => _ScrollToTopButtonState();
}

class _ScrollToTopButtonState extends State<ScrollToTopButton> {
  bool _visible = false;

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_scrollListener);
  }

  void _scrollListener() {
    final shouldShow = widget.controller.offset > widget.showOffset;
    if (shouldShow != _visible) {
      setState(() {
        _visible = shouldShow;
      });
    }
  }

  @override
  void dispose() {
    widget.controller.removeListener(_scrollListener);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedOpacity(
      opacity: _visible ? 1.0 : 0.0,
      duration: const Duration(milliseconds: 300),
      child: Align(
        alignment: Alignment.bottomCenter,
        child: Padding(
          padding: EdgeInsets.only(bottom: widget.bottomPadding),
          child: FabM3E(
            heroTag: null,
            elevation: 0,
            kind: FabM3EKind.secondary,
            shapeFamily: FabM3EShapeFamily.square,
            size: FabM3ESize.small,
            icon: const Icon(FluentIcons.arrow_up_24_regular),
            onPressed: () {
              widget.controller.animateTo(
                widget.controller.position.minScrollExtent,
                duration: const Duration(milliseconds: 500),
                curve: Curves.easeOut,
              );
            },
          ),
        ),
      ),
    );
  }
}

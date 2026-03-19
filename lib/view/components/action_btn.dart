import 'package:flutter/material.dart';

class CallActionButton extends StatefulWidget {
  final String label;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
  final VoidCallback onTap;
  final bool isLarge;
  final bool isSuperLarge;
  final bool toggleable;
  final bool initialOn;

  const CallActionButton({
    super.key,
    required this.label,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.onTap,
    this.isLarge = false,
    this.isSuperLarge = false,
    this.toggleable = true,
    this.initialOn = true,
  });

  @override
  State<CallActionButton> createState() => _CallActionButtonState();
}

class _CallActionButtonState extends State<CallActionButton> {
  bool isOn = true;
  bool isPressed = false;

  @override
  void initState() {
    super.initState();
    isOn = widget.initialOn;
  }

  void _handleTap() {
    if (widget.toggleable) {
      setState(() {
        isOn = !isOn;
      });
    }
    widget.onTap();
  }

  @override
  Widget build(BuildContext context) {
    final showText = widget.isLarge || widget.isSuperLarge;

    BorderRadius currentRadius;
    if (widget.toggleable) {
      currentRadius =
          isOn ? BorderRadius.circular(36) : BorderRadius.circular(24);
    } else {
      currentRadius =
          isPressed ? BorderRadius.circular(24) : BorderRadius.circular(36);
    }

    return GestureDetector(
      onTapDown: (_) {
        if (!widget.toggleable) {
          setState(() => isPressed = true);
        }
      },
      onTapUp: (_) {
        if (!widget.toggleable) {
          setState(() => isPressed = false);
        }
      },
      onTapCancel: () {
        if (!widget.toggleable) {
          setState(() => isPressed = false);
        }
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        curve: Curves.fastOutSlowIn,
        height: 72,
        width: widget.isSuperLarge ? double.infinity : null,
        padding: EdgeInsets.symmetric(horizontal: showText ? 32 : 24),
        decoration: BoxDecoration(
          color: widget.backgroundColor,
          borderRadius: currentRadius,
        ),
        child: InkWell(
          borderRadius: currentRadius,
          onTap: _handleTap,
          child: Row(
            mainAxisSize:
                widget.isSuperLarge ? MainAxisSize.max : MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(widget.icon, color: widget.foregroundColor, size: 28),
              if (showText) ...[
                const SizedBox(width: 12),
                Text(
                  widget.label,
                  style: TextStyle(
                    color: widget.foregroundColor,
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.2,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/view/components/matched_view.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/screen/dialpad/action_btn.dart';
import 'package:revo/view/screen/dialpad/dial_btn.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';

class DialPadView extends ConsumerStatefulWidget {
  const DialPadView({super.key});

  @override
  ConsumerState<DialPadView> createState() => _DialPadViewState();
}

class _DialPadViewState extends ConsumerState<DialPadView> {
  String _number = '';

  late final ScrollController _scrollController;
  late final FocusNode _focusNode;

  @override
  void initState() {
    _scrollController = ScrollController();
    _focusNode = FocusNode();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
    });
    SharedPrefService().onPreferenceChanged.listen((key) {
      if (key == PREF_DIALPAD_LETTERS) {
        if (mounted) setState(() {});
      }
    });
    super.initState();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  final List<String> keys = [
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9',
    '*',
    '0',
    '#'
  ];

  final Map<String, String> subKeys = {
    '2': 'ABC',
    '3': 'DEF',
    '4': 'GHI',
    '5': 'JKL',
    '6': 'MNO',
    '7': 'PQRS',
    '8': 'TUV',
    '9': 'WXYZ',
    '0': '+',
  };

  @override
  Widget build(BuildContext context) {
    final simCards = ref.watch(getSimInfoProvider);
    final colorScheme = context.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: MatchedView(
                  scrollController: _scrollController,
                  searchText: _number,
                ),
              ),
            ),
            Container(
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer.withOpacity(0.2),
                borderRadius:
                    const BorderRadius.vertical(top: Radius.circular(32)),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const SizedBox(height: 10),
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    height: _number.isNotEmpty ? 70 : 30,
                    alignment: Alignment.center,
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      reverse: true,
                      child: Text(
                        _number,
                        style: GoogleFonts.outfit(
                          fontSize: 40,
                          fontWeight: FontWeight.w500,
                          color: colorScheme.onSurface,
                        ),
                      ),
                    ),
                  ),
                  GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: keys.length,
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 3,
                      mainAxisSpacing: 12,
                      crossAxisSpacing: 12,
                      childAspectRatio: 1.6,
                    ),
                    padding:
                        const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
                    itemBuilder: (context, index) {
                      String key = keys[index];
                      return DialPadButton(
                        mainText: key,
                        subText: SharedPrefService()
                                .getBool(PREF_DIALPAD_LETTERS, def: true)
                            ? subKeys[key]
                            : null,
                        onUpdate: (String str) {
                          hapticVibration();
                          setState(() => _number += str);
                        },
                      );
                    },
                  ),
                  const SizedBox(height: 12),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(28, 0, 28, 24),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        SizedBox(
                          width: 56,
                          child: _number.isNotEmpty
                              ? ActionIconButton(
                                  FluentIcons.person_add_24_regular,
                                  size: 48,
                                  onPressed: () {
                                    hapticVibration();
                                    ref
                                        .read(contactServiceProvider.notifier)
                                        .createNewContact(number: _number);
                                  },
                                )
                              : const SizedBox.shrink(),
                        ),
                        DialActionButton(
                          icon: FluentIcons.call_24_filled,
                          label: 'Call',
                          func: () {
                            hapticVibration();
                            simCards.whenData((value) => SimPicker(
                                    context: context,
                                    simCards: value,
                                    number: _number)
                                .show());
                          },
                        ),
                        SizedBox(
                          width: 56,
                          child: _number.isNotEmpty
                              ? ActionIconButton(
                                  FluentIcons.arrow_left_24_regular,
                                  size: 48,
                                  onPressed: () {
                                    hapticVibration();
                                    setState(() {
                                      if (_number.isNotEmpty) {
                                        _number = _number.substring(
                                            0, _number.length - 1);
                                      }
                                    });
                                  },
                                  onLongPress: () {
                                    HapticFeedback.heavyImpact();
                                    setState(() => _number = '');
                                  },
                                )
                              : const SizedBox.shrink(),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

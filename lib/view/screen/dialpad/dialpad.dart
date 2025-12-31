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
import 'package:revo/view/components/matched_view.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/screen/dialpad/action_btn.dart';
import 'package:revo/view/screen/dialpad/dial_btn.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/utils/utils.dart';

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
        setState(() {});
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
    '#',
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

    return Scaffold(
      body: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: MatchedView(
                  scrollController: _scrollController,
                  searchText: _number,
                ),
              ),
            ),
            Container(
              color: context.colorScheme.secondaryContainer.withAlpha(50),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _number.isNotEmpty
                      ? Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 20,
                            vertical: 15,
                          ),
                          child: Text(
                            _number,
                            style: GoogleFonts.outfit(
                              fontSize: 30,
                              color: context.colorScheme.onSurface,
                            ),
                          ),
                        )
                      : SizedBox(height: 30),
                  GridView.builder(
                    shrinkWrap: true,
                    physics: NeverScrollableScrollPhysics(),
                    itemCount: keys.length,
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 3,
                      mainAxisSpacing: 8,
                      crossAxisSpacing: 8,
                      childAspectRatio: 1.75,
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    itemBuilder: (context, index) {
                      String key = keys[index];
                      return DialPadButton(
                        mainText: key,
                        subText: SharedPrefService()
                                .getBool(PREF_DIALPAD_LETTERS, def: true)
                            ? subKeys[key]
                            : null,
                        onUpdate: (String str) {
                          setState(() {
                            _number += str;
                          });
                        },
                      );
                    },
                  ),
                  SizedBox(height: 20),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 25),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        if (_number.isNotEmpty)
                          RoundedIconButton(
                            FluentIcons.person_add_24_regular,
                            size: 40,
                            onTap: () {
                              hapticVibration();
                              ref
                                  .read(contactServiceProvider.notifier)
                                  .createNewContact(number: _number);
                            },
                          ),
                        Spacer(),
                        DialActionButton(
                          icon: FluentIcons.sim_24_regular,
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
                        Spacer(),
                        if (_number.isNotEmpty)
                          RoundedIconButton(
                            FluentIcons.arrow_left_24_regular,
                            size: 40,
                            onTap: () {
                              hapticVibration();
                              setState(() {
                                if (_number.isNotEmpty) {
                                  _number = _number.substring(
                                    0,
                                    _number.length - 1,
                                  );
                                }
                              });
                            },
                            onLongPress: () {
                              HapticFeedback.vibrate();
                              setState(() {
                                if (_number.isNotEmpty) {
                                  _number = '';
                                }
                              });
                            },
                          ),
                      ],
                    ),
                  ),
                  SizedBox(height: 30),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

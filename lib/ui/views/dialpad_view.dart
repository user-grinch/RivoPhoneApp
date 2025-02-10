import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/services/prefservice.dart';
import 'package:revo/ui/popups/sim_choose_popup.dart';
import 'package:revo/ui/views/common/matched_view.dart';
import 'package:revo/ui/views/dialpad_view/action_btn.dart';
import 'package:revo/ui/views/dialpad_view/dial_btn.dart';
import 'package:revo/utils/rounded_icon_btn.dart';
import 'package:revo/utils/utils.dart';

class DialPadView extends StatefulWidget {
  const DialPadView({super.key});

  @override
  State<DialPadView> createState() => _DialPadViewState();
}

class _DialPadViewState extends State<DialPadView> {
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
                  number: _number,
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
                            style: GoogleFonts.raleway(
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
                            context,
                            icon: HugeIcons.strokeRoundedUserAdd01,
                            size: 40,
                            onTap: () {
                              hapticVibration();
                              context
                                  .read<ContactService>()
                                  .createNewContact(number: _number);
                            },
                          ),
                        Spacer(),
                        DialActionButton(
                          icon: HugeIcons.strokeRoundedSimcard01,
                          label: 'Call',
                          func: () {
                            hapticVibration();
                            showDialog(
                              context: context,
                              builder: (context) =>
                                  simChooserDialog(context, _number),
                            );
                          },
                        ),
                        Spacer(),
                        if (_number.isNotEmpty)
                          RoundedIconButton(
                            context,
                            icon: HugeIcons.strokeRoundedArrowLeft01,
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

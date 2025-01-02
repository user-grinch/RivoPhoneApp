import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';

class DialPadView extends StatefulWidget {
  @override
  State<DialPadView> createState() => _DialPadViewState();
}

class _DialPadViewState extends State<DialPadView> {
  String _number = '';

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

  final List<String> matchedContacts = ['John Doe', 'Jane Smith', 'Alex Brown'];

  void updateDialedNumber(String str) {
    setState(() {
      _number += str;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: matchedContacts.map((contact) {
                  return Text(
                    contact,
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  );
                }).toList(),
              ),
            ),
            Expanded(
              child: SizedBox(),
            ),
            Container(
              color: context.colorScheme.surfaceTint.withAlpha(30),
              child: Column(
                children: [
                  _number.isNotEmpty
                      ? Padding(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 30.0, vertical: 15),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Spacer(),
                              Text(
                                _number,
                                style: GoogleFonts.cabin(
                                  fontSize: 30,
                                  color: context.colorScheme.primary,
                                ),
                              ),
                              Spacer(),
                              IconButton(
                                onPressed: () {
                                  setState(() {
                                    if (_number.isNotEmpty) {
                                      _number = _number.substring(
                                          0, _number.length - 1);
                                    }
                                  });
                                },
                                icon: const Icon(Icons.backspace),
                                color: context.colorScheme.primary,
                              ),
                            ],
                          ),
                        )
                      : SizedBox(height: 30),
                  GridView.builder(
                    shrinkWrap: true,
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
                        subText: subKeys[key],
                        onUpdate: updateDialedNumber,
                      );
                    },
                  ),
                  SizedBox(height: 20),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      DialActionButton(icon: Icons.sim_card, label: 'Sim 1'),
                      DialActionButton(icon: Icons.sim_card, label: 'Sim 2'),
                    ],
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

class DialPadButton extends StatelessWidget {
  final String mainText;
  final String? subText;
  final Function(String) onUpdate;

  const DialPadButton({
    required this.mainText,
    this.subText,
    super.key,
    required this.onUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(50)),
        ),
        backgroundColor: context.colorScheme.surface.withAlpha(180),
      ),
      onPressed: () {
        onUpdate(mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            mainText,
            style: const TextStyle(fontSize: 24),
          ),
          if (subText != null)
            Text(
              subText!,
              style: const TextStyle(fontSize: 10),
            ),
        ],
      ),
    );
  }
}

class DialActionButton extends StatelessWidget {
  final IconData icon;
  final String label;

  const DialActionButton({required this.icon, required this.label, super.key});

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(50),
        ),
        backgroundColor: context.colorScheme.primaryContainer,
        elevation: 0,
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 10),
      ),
      onPressed: () {},
      child: Row(
        children: [
          Icon(Icons.sim_card),
          SizedBox(
            width: 2,
          ),
          Text(
            label,
            style: TextStyle(
                fontSize: 18, color: context.colorScheme.onPrimaryContainer),
          ),
        ],
      ),
    );
  }
}

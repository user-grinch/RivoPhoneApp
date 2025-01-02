import 'package:flutter/material.dart';

class RecentsView extends StatelessWidget {
  const RecentsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Expanded(
          child: Center(
            child: Text(
              'No recents',
              style: TextStyle(
                fontSize: 24,
                color: Colors.grey,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/components/radio_tile.dart';
import 'package:revo/view/components/switch_tile.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

class CallView extends ConsumerStatefulWidget {
  const CallView({super.key});

  @override
  ConsumerState<CallView> createState() => _CallViewState();
}

String selectedSim = "SIM 1";

class _CallViewState extends ConsumerState<CallView> {
  bool disableMaterialYou = false;
  bool hideAvatarInitials = false;
  bool showAvatarPictures = true;
  bool iconOnlyBottomNav = false;
  bool enableCustomCallScreen = false;

  @override
  Widget build(BuildContext context) {
    final simInfo = ref.watch(getSimInfoProvider);
    final defSim = ref.watch(defaultSimProvider);

    return Scaffold(
      appBar: AppBarComponent("Call Settings"),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        children: [
          SwitchTileWidget(
              title: "Speed dial",
              subtitle: "Directly call someone by holding a dialpad key",
              value: disableMaterialYou,
              onChanged: (value) {
                setState(() {
                  disableMaterialYou = value;
                });
              },
              isFirst: true),
          MenuTile(
            title: 'Default SIM',
            subtitle: 'Select SIM card for voice calls',
            icon: FluentIcons.sim_24_filled,
            onTap: () {
              simInfo.whenData(
                (e) => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => RadioSelectionPage<int>(
                      title: "Default SIM",
                      initialValue: defSim,
                      options: [
                        (
                          title: "Default",
                          subtitle: "Ask every time",
                          icon: Icons.question_mark,
                          value: 0
                        ),
                        ...e.asMap().entries.map((f) {
                          return (
                            title: "SIM ${(f.key + 1).toString()}",
                            subtitle: f.value.carrierName,
                            icon: Icons.sim_card,
                            value: f.key + 1
                          );
                        }),
                      ],
                      onSelected: (val) =>
                          ref.read(defaultSimProvider.notifier).update(val),
                    ),
                  ),
                ),
              );
            },
            isLast: true,
          ),
          const SizedBox(
            height: 10,
          ),
          SwitchTileWidget(
            title: "T9 Dialing",
            subtitle: "Predicts words from numeric keypad inputs",
            value: enableCustomCallScreen,
            onChanged: (value) {
              setState(() {
                enableCustomCallScreen = value;
              });
            },
            isFirst: true,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

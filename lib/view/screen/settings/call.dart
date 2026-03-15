import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/controller/services/mobile_service.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/components/switch_tile.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

class CallSettings extends ConsumerStatefulWidget {
  const CallSettings({super.key});

  @override
  ConsumerState<CallSettings> createState() => _CallSettingsState();
}

class _CallSettingsState extends ConsumerState<CallSettings> {
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
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text("Default SIM"),
                  content: simInfo.when(
                    data: (data) => Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        RadioListTile<int>(
                          title: const Text("Ask every time"),
                          value: 0,
                          groupValue: defSim,
                          onChanged: (val) {
                            ref
                                .read(defaultSimProvider.notifier)
                                .update(val ?? 0);
                            Navigator.pop(context);
                          },
                        ),
                        ...data.asMap().entries.map((e) => RadioListTile<int>(
                              title: Text("SIM ${e.key + 1}"),
                              subtitle: Text(e.value.carrierName),
                              value: e.key + 1,
                              groupValue: defSim,
                              onChanged: (val) {
                                ref
                                    .read(defaultSimProvider.notifier)
                                    .update(val ?? 0);
                                Navigator.pop(context);
                              },
                            ))
                      ],
                    ),
                    loading: () => const CircularProgressIndicator(),
                    error: (error, stackTrace) =>
                        const Text("Error loading SIM info"),
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

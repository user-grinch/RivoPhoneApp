import 'package:flutter_sim_data/sim_data.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/controller/providers/activity_service.dart';

part 'mobile_service.g.dart';

@Riverpod(keepAlive: true)
Future<List<SimCard>> getSimInfo(Ref ref) async {
  await ActivityService().requestPermissions();

  if (!await Permission.phone.status.isGranted) {
    return [];
  }

  try {
    final data = await SimData().getSimData();
    return data.map(SimCard.fromInternal).toList();
  } catch (_) {
    return [];
  }
}

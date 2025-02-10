import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';

class SharedPrefService {
  static final SharedPrefService _instance = SharedPrefService._internal();

  factory SharedPrefService() {
    return _instance;
  }

  SharedPrefService._internal();

  late SharedPreferences _prefs;
  final StreamController<String> _prefChangesController =
      StreamController.broadcast();

  Stream<String> get onPreferenceChanged => _prefChangesController.stream;

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  Future<void> saveString(String key, String value) async {
    await _prefs.setString(key, value);
    _prefChangesController.add(key);
  }

  String? getString(String key) {
    return _prefs.getString(key);
  }

  Future<void> saveBool(String key, bool value) async {
    await _prefs.setBool(key, value);
    _prefChangesController.add(key);
  }

  bool getBool(String key, {bool def = false}) {
    return _prefs.getBool(key) ?? def;
  }

  Future<void> saveInt(String key, int value) async {
    await _prefs.setInt(key, value);
    _prefChangesController.add(key);
  }

  int? getInt(String key) {
    return _prefs.getInt(key);
  }

  Future<void> saveDouble(String key, double value) async {
    await _prefs.setDouble(key, value);
    _prefChangesController.add(key);
  }

  double? getDouble(String key) {
    return _prefs.getDouble(key);
  }

  Future<void> saveStringList(String key, List<String> value) async {
    await _prefs.setStringList(key, value);
    _prefChangesController.add(key);
  }

  List<String>? getStringList(String key) {
    return _prefs.getStringList(key);
  }

  Future<void> remove(String key) async {
    await _prefs.remove(key);
    _prefChangesController.add(key);
  }

  Future<void> clear() async {
    await _prefs.clear();
    _prefChangesController.add("all");
  }

  /// Dispose the stream when not needed
  void dispose() {
    _prefChangesController.close();
  }
}

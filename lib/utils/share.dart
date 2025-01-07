import 'package:revo/model/contact.dart';

String generateVCardString(Contact contact) {
  String str = '''
BEGIN:VCARD
VERSION:3.0
FN:${contact.fullName}''';

  for (var phone in contact.phones) {
    str += 'TEL:$phone\n';
  }
  str += 'END:VCARD';
  return str;
}

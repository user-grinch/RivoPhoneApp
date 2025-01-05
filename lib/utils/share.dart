import 'package:flutter_contacts/contact.dart';

String generateVCardString(Contact contact) {
  String str = '''
BEGIN:VCARD
VERSION:3.0
FN:${contact.name.first} ${contact.name.middle ?? ''} ${contact.name.last ?? ''}
''';

  for (var phone in contact.phones) {
    str += 'TEL:${phone.normalizedNumber}\n';
  }
  str += 'END:VCARD';
  return str;
}

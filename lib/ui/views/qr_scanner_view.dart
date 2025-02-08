// class SimpleBarcodeScannerView extends StatelessWidget {
//   @override
//   Widget build(BuildContext context) {
//     bool isProcessingScan = false;

//     return Scaffold(
//       appBar: AppBar(
//         leading: IconButton(
//           icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
//           onPressed: () => Navigator.of(context).pop(), // Close scanner screen
//         ),
//         title: const Text('Scan QR to add contact'),
//       ),
//       body: SimpleBarcodeScanner.streamBarcode(
//         context,
//         barcodeAppBar: const BarcodeAppBar(
//           appBarTitle: 'Scan QR to add contact',
//           centerTitle: false,
//           enableBackButton: true,
//           backButtonIcon: Icon(HugeIcons.strokeRoundedArrowLeft01),
//         ),
//         scanType: ScanType.qr,
//         isShowFlashIcon: true,
//         delayMillis: 500,
//       ).listen((event) async {
//         if (isProcessingScan) return;
//         isProcessingScan = true;

//         if (event.startsWith("BEGIN:VCARD") && event.endsWith("END:VCARD")) {
//           await context.read<ContactService>().insertContactFromVCard(event);
//           ScaffoldMessenger.of(context).showSnackBar(
//             const SnackBar(content: Text('Contact added successfully!')),
//           );
//           Navigator.of(context).pop(); // Close scanner window after success
//         } else {
//           ScaffoldMessenger.of(context).showSnackBar(
//             const SnackBar(content: Text('Invalid vCard format!')),
//           );
//         }
//         isProcessingScan = false;
//       }),
//     );
//   }
// }

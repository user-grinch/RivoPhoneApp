import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/popups/number_choose_popup.dart';
import 'package:revo/ui/popups/qr_popup.dart';
import 'package:revo/ui/popups/sim_choose_popup.dart';
import 'package:revo/utils/rounded_icon_btn.dart';
import 'package:revo/utils/share.dart';
import 'package:share_plus/share_plus.dart';

class ContactInfoView extends StatefulWidget {
  final Contact contact;
  const ContactInfoView(this.contact, {super.key});

  @override
  State<ContactInfoView> createState() => _ContactInfoViewState();
}

class _ContactInfoViewState extends State<ContactInfoView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        elevation: 0,
      ),
      floatingActionButton: FloatingActionButton.extended(
        elevation: 1,
        onPressed: () async {
          await context.read<ContactService>().editContact(widget.contact);
        },
        backgroundColor: context.colorScheme.secondaryContainer,
        label: Text(
          "Edit",
          style: TextStyle(color: context.colorScheme.onSecondaryContainer),
        ),
        icon: Icon(HugeIcons.strokeRoundedEdit02,
            color: context.colorScheme.onSecondaryContainer),
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.only(
            top: MediaQuery.of(context).padding.top, left: 16, right: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            _buildProfilePicture(context),
            const SizedBox(height: 16),
            Text(
              widget.contact.fullName,
              style: GoogleFonts.raleway(
                fontSize: 28,
                color: context.colorScheme.onSurface,
              ),
              textAlign: TextAlign.center,
            ),

            Padding(
              padding: const EdgeInsets.all(16),
              child: Wrap(
                alignment: WrapAlignment.center,
                spacing: 20,
                children: [
                  RoundedIconButton(
                    context,
                    size: 45,
                    icon: HugeIcons.strokeRoundedQrCode,
                    text: 'QR Code',
                    onTap: () {
                      showDialog(
                        context: context,
                        builder: (context) => qrCodePopup(
                          context,
                          generateVCardString(widget.contact),
                        ),
                      );
                    },
                  ),
                  RoundedIconButton(
                    context,
                    icon: HugeIcons.strokeRoundedShare08,
                    size: 45,
                    text: 'Share',
                    onTap: () {
                      Share.shareXFiles([
                        XFile.fromData(
                            utf8.encode(generateVCardString(widget.contact)),
                            mimeType: 'text/plain')
                      ], fileNameOverrides: [
                        'contact.vcf'
                      ]);
                    },
                  ),
                  RoundedIconButton(
                    context,
                    icon: HugeIcons.strokeRoundedClock04,
                    size: 45,
                    text: 'Call History',
                    onTap: () {
                      Navigator.of(context).pushNamed(
                        callHistoryRoute,
                        arguments: widget.contact.phones,
                      );
                    },
                  ),
                  RoundedIconButton(
                    context,
                    icon: widget.contact.isStarred
                        ? HugeIcons.strokeRoundedHeartCheck
                        : HugeIcons.strokeRoundedHeartAdd,
                    size: 45,
                    text: 'Favourite',
                    onTap: () {
                      setState(() {
                        widget.contact.isStarred = !widget.contact.isStarred;
                      });
                      context
                          .read<ContactService>()
                          .updateContact(contact: widget.contact);
                    },
                  ),
                ],
              ),
            ),

            _buildContactInfoSection(context),
            const SizedBox(height: 16),

            // External Apps Section
            Card(
              elevation: 0,
              margin: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              color: context.colorScheme.secondaryContainer.withAlpha(100),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "External Apps",
                      style: GoogleFonts.raleway(
                        fontSize: 20,
                        color: context.colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Column(
                      children: [
                        _buildListTile(context, HugeIcons.strokeRoundedTelegram,
                            'Telegram', () {
                          showDialog(
                            context: context,
                            builder: (context) => numberChooserDialog(
                                context, widget.contact.phones,
                                (String num) async {
                              ActivityService().openTelegram(num);
                            }),
                          );
                        }),
                        _buildListTile(context, HugeIcons.strokeRoundedVideo01,
                            'Video Call', () {
                          showDialog(
                            context: context,
                            builder: (context) => numberChooserDialog(
                                context, widget.contact.phones,
                                (String num) async {
                              ActivityService().makeVideoCall(num);
                            }),
                          );
                        }),
                        _buildListTile(context, HugeIcons.strokeRoundedWhatsapp,
                            'WhatsApp', () {
                          showDialog(
                            context: context,
                            builder: (context) => numberChooserDialog(
                                context, widget.contact.phones,
                                (String num) async {
                              ActivityService().openWhatsApp(num);
                            }),
                          );
                        }),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildListTile(
      BuildContext context, IconData icon, String label, VoidCallback onTap) {
    return ListTile(
      leading: Container(
        width: 35,
        height: 35,
        decoration: BoxDecoration(
          color: context.colorScheme.secondaryContainer,
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: context.colorScheme.onSecondaryContainer),
      ),
      title: Text(
        label,
        style: context.textTheme.bodyLarge?.copyWith(
          color: context.colorScheme.onSurface,
        ),
      ),
      onTap: onTap,
    );
  }

  Widget _buildContactInfoSection(BuildContext context) {
    return Card(
      elevation: 0,
      margin: const EdgeInsets.symmetric(vertical: 16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      color: context.colorScheme.secondaryContainer.withAlpha(100),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Phone Numbers",
              style: GoogleFonts.raleway(
                fontSize: 20,
                color: context.colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 16),
            if (widget.contact.phones.isNotEmpty)
              ...widget.contact.phones
                  .map((phone) => _buildPhoneWithActionIcons(context, phone)),
          ],
        ),
      ),
    );
  }

  Widget _buildPhoneWithActionIcons(BuildContext context, var phone) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        children: [
          Expanded(
            child: Text(
              phone,
              style: GoogleFonts.raleway(
                textStyle: context.textTheme.bodyLarge,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          Wrap(
            spacing: 12,
            children: [
              RoundedIconButton(
                context,
                icon: HugeIcons.strokeRoundedCall02,
                onTap: () {
                  simChooserDialog(context, phone);
                },
                size: 36,
              ),
              RoundedIconButton(
                context,
                icon: HugeIcons.strokeRoundedMessage01,
                onTap: () {
                  ActivityService().sendSMS(phone);
                },
                size: 36,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildProfilePicture(BuildContext context) {
    return CircleAvatar(
      backgroundColor: context.colorScheme.secondaryContainer,
      radius: 70,
      backgroundImage: widget.contact.photo != null
          ? MemoryImage(widget.contact.photo!)
          : null,
      child: widget.contact.photo == null
          ? Icon(
              HugeIcons.strokeRoundedUser,
              size: 100,
              color: context.colorScheme.onSecondaryContainer,
            )
          : null,
    );
  }
}

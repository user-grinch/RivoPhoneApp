import 'dart:convert';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/view/components/num_picker.dart';
import 'package:revo/view/components/qr_popup.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/utils/rounded_icon_btn.dart';
import 'package:revo/view/utils/share.dart';
import 'package:share_plus/share_plus.dart';

class ContactInfoView extends ConsumerStatefulWidget {
  final Contact contact;
  const ContactInfoView(this.contact, {super.key});

  @override
  ConsumerState<ContactInfoView> createState() => _ContactInfoViewState();
}

class _ContactInfoViewState extends ConsumerState<ContactInfoView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBarM3E(
        leading: IconButton(
          icon: Icon(FluentIcons.arrow_left_24_regular),
          onPressed: () => Navigator.of(context).pop(),
        ),
        centerTitle: false,
        shapeFamily: AppBarM3EShapeFamily.round,
        density: AppBarM3EDensity.regular,
        actions: [
          IconButton(
            icon: Icon(FluentIcons.edit_24_regular),
            onPressed: () {
              ref
                  .read(contactServiceProvider.notifier)
                  .editContact(widget.contact);
            },
          )
        ],
        elevation: 0,
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
              style: GoogleFonts.outfit(
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
                    icon: FluentIcons.qr_code_24_regular,
                    text: 'QR Code',
                    onTap: () {
                      QrCodePopup(
                          context: context,
                          data: generateVCardString(widget.contact));
                    },
                  ),
                  RoundedIconButton(
                    context,
                    icon: FluentIcons.share_24_regular,
                    size: 45,
                    text: 'Share',
                    onTap: () {
                      SharePlus.instance.share(ShareParams(files: [
                        XFile.fromData(
                            utf8.encode(generateVCardString(widget.contact)),
                            mimeType: 'text/plain')
                      ], fileNameOverrides: [
                        'contact.vcf'
                      ]));
                    },
                  ),
                  RoundedIconButton(
                    context,
                    icon: FluentIcons.history_24_regular,
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
                        ? FluentIcons.star_24_filled
                        : FluentIcons.star_24_regular,
                    size: 45,
                    text: 'Favorite',
                    onTap: () {
                      setState(() {
                        widget.contact.isStarred = !widget.contact.isStarred;
                      });
                      ref
                          .read(contactServiceProvider.notifier)
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
                      style: GoogleFonts.outfit(
                        fontSize: 20,
                        color: context.colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Column(
                      children: [
                        _buildListTile(
                            context, FontAwesomeIcons.telegramPlane, 'Telegram',
                            () {
                          NumberPicker(
                            context: context,
                            numbers: widget.contact.phones,
                            onTap: (String num) async {
                              ActivityService().openTelegram(num);
                            },
                          ).show();
                        }),
                        _buildListTile(
                            context, FluentIcons.video_24_regular, 'Video Call',
                            () {
                          NumberPicker(
                            context: context,
                            numbers: widget.contact.phones,
                            onTap: (String num) async {
                              ActivityService().makeVideoCall(num);
                            },
                          );
                        }),
                        _buildListTile(
                            context, FontAwesomeIcons.whatsapp, 'WhatsApp', () {
                          NumberPicker(
                            context: context,
                            numbers: widget.contact.phones,
                            onTap: (String num) async {
                              ActivityService().openWhatsApp(num);
                            },
                          ).show();
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
              style: GoogleFonts.outfit(
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
    final simCards = ref.watch(getSimInfoProvider);

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        children: [
          Expanded(
            child: Text(
              phone,
              style: GoogleFonts.outfit(
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
                icon: FluentIcons.call_24_regular,
                onTap: () {
                  simCards.whenData((value) => SimPicker(
                          context: context, simCards: value, number: phone)
                      .show());
                },
                size: 36,
              ),
              RoundedIconButton(
                context,
                icon: FluentIcons.chat_24_regular,
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
              FluentIcons.person_24_filled,
              size: 100,
              color: context.colorScheme.onSecondaryContainer,
            )
          : null,
    );
  }
}

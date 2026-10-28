import 'package:flutter/material.dart';
import 'package:cmp_consent/cmp_consent.dart';

class ConsentExample extends StatefulWidget {
  const ConsentExample({super.key});
  @override
  State<ConsentExample> createState() => _ConsentExampleState();
}

class _ConsentExampleState extends State<ConsentExample> {
  final keyInput = TextEditingController(
    text: const String.fromEnvironment('CMP_CODE_CONFIG'),
  );
  final urlInput = TextEditingController(
    text: const String.fromEnvironment(
      'CMP_BASE_URL',
      defaultValue: 'https://cmp.biznext.vn',
    ),
  );
  String message = '';
  bool configured = false;
  @override
  void dispose() {
    keyInput.dispose();
    urlInput.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('CMP Flutter Example')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          controller: keyInput,
          decoration: const InputDecoration(labelText: 'Collection Point key'),
        ),
        TextField(
          controller: urlInput,
          decoration: const InputDecoration(labelText: 'CMP origin URL'),
        ),
        FilledButton(
          onPressed: () async {
            try {
              await CmpConsent.configure(
                codeConfig: keyInput.text,
                baseUrl: urlInput.text,
              );
              if (mounted) {
                setState(() {
                  configured = true;
                  message = 'Đã cấu hình';
                });
              }
            } catch (e) {
              if (mounted) setState(() => message = e.toString());
            }
          },
          child: const Text('Cấu hình'),
        ),
        Text(message),
        for (final embedded in [true, false])
          FilledButton(
            onPressed: !configured
                ? null
                : () async {
                    await Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => ConsentScreen(embedded: embedded),
                      ),
                    );
                  },
            child: Text(
              embedded ? 'Đăng ký với consent nhúng' : 'Màn consent độc lập',
            ),
          ),
        OutlinedButton(
          onPressed: !configured
              ? null
              : () async {
                  await CmpConsent.clear();
                  if (mounted) setState(() => message = 'Đã xóa quyết định');
                },
          child: const Text('Đăng xuất'),
        ),
      ],
    ),
  );
}

class ConsentScreen extends StatefulWidget {
  const ConsentScreen({super.key, required this.embedded});
  final bool embedded;
  @override
  State<ConsentScreen> createState() => _ConsentScreenState();
}

class _ConsentScreenState extends State<ConsentScreen> {
  final controller = ConsentFormController();
  final email = TextEditingController(text: 'demo@example.invalid');
  String message = '';
  @override
  void dispose() {
    email.dispose();
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(widget.embedded ? 'Đăng ký mẫu' : 'Consent độc lập'),
    ),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        TextField(
          controller: email,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(labelText: 'Email'),
        ),
        CmpConsentForm(
          controller: controller,
          embedded: widget.embedded,
          values: {'Email': email.text},
          onSubmitted: (id) => setState(() => message = 'Đã gửi consentId=$id'),
        ),
        ListenableBuilder(
          listenable: controller,
          builder: (context, _) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Trạng thái: ${controller.status['state']} ${controller.status['message'] ?? ''}',
              ),
              if (widget.embedded)
                FilledButton(
                  onPressed:
                      controller.ready &&
                          controller.allRequiredGranted &&
                          !controller.submitting
                      ? () async {
                          try {
                            final id = await controller.submit({
                              'Email': email.text,
                            });
                            if (mounted) {
                              setState(
                                () => message =
                                    'Đã gửi consentId=$id. Tiếp tục nghiệp vụ tại đây.',
                              );
                            }
                          } catch (e) {
                            if (mounted) setState(() => message = e.toString());
                          }
                        }
                      : null,
                  child: const Text('Gửi consent trước khi đăng ký'),
                ),
              OutlinedButton(
                onPressed: controller.submitting ? null : controller.reload,
                child: const Text('Tải lại'),
              ),
            ],
          ),
        ),
        Text(message),
      ],
    ),
  );
}

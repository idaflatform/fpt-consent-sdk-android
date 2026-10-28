import 'package:cmp_consent/cmp_consent.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets(
    'status recovers initial native height and rounds up to avoid clipping',
    (tester) async {
      debugDefaultTargetPlatformOverride = TargetPlatform.android;
      const formChannel = MethodChannel('vn.fpt.cmp/form/0');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      messenger.setMockMethodCallHandler(SystemChannels.platform_views, (
        call,
      ) async {
        if (call.method == 'create') return 0;
        if (call.method == 'resize') {
          final args = Map<String, dynamic>.from(call.arguments as Map);
          return {'width': args['width'], 'height': args['height']};
        }
        return null;
      });
      messenger.setMockMethodCallHandler(formChannel, (call) async {
        if (call.method == 'status') {
          return {
            'state': 'ready',
            'height': 312.3,
            'allRequiredGranted': true,
          };
        }
        return null;
      });
      final controller = ConsentFormController();
      try {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(body: CmpConsentForm(controller: controller)),
          ),
        );
        await tester.pumpAndSettle();
        expect(controller.ready, isTrue);
        expect(controller.allRequiredGranted, isTrue);
        expect(tester.getSize(find.byType(AndroidView)).height, 313);
        await tester.pumpWidget(const SizedBox());
        await tester.pumpAndSettle();
      } finally {
        controller.dispose();
        messenger.setMockMethodCallHandler(formChannel, null);
        messenger.setMockMethodCallHandler(SystemChannels.platform_views, null);
        debugDefaultTargetPlatformOverride = null;
      }
    },
  );
}

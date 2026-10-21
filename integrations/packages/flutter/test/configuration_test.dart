import 'package:cmp_consent/cmp_consent.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final calls = <MethodCall>[];
  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(CmpConsent.channel, (call) async {
          calls.add(call);
          return null;
        });
  });
  tearDown(
    () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(CmpConsent.channel, null),
  );
  test('invalid configuration never reaches native SDK', () async {
    for (final url in [
      'file:///tmp',
      'https://example.com/api',
      'https://user:password@example.com',
      'https://example.com?token=private',
    ]) {
      await expectLater(
        CmpConsent.configure(codeConfig: 'demo', baseUrl: url),
        throwsArgumentError,
      );
    }
    await expectLater(
      CmpConsent.configure(codeConfig: '  '),
      throwsArgumentError,
    );
    expect(calls, isEmpty);
  });
  test(
    'host configuration is forwarded without embedding a customer key',
    () async {
      await CmpConsent.configure(
        codeConfig: ' cp_demo::t_demo ',
        baseUrl: 'https://cmp.example.invalid',
      );
      expect(calls.single.method, 'configure');
      expect(calls.single.arguments, {
        'codeConfig': 'cp_demo::t_demo',
        'baseUrl': 'https://cmp.example.invalid',
      });
    },
  );
  test(
    'unattached form rejects submit rather than silently continuing business flow',
    () async {
      final form = ConsentFormController();
      await expectLater(
        form.submit({'Email': 'demo@example.invalid'}),
        throwsA(
          isA<PlatformException>().having((e) => e.code, 'code', 'disposed'),
        ),
      );
      form.dispose();
    },
  );
}

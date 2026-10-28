import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CmpConsent {
  static const channel = MethodChannel('vn.fpt.cmp/control');
  static Future<void> configure({
    required String codeConfig,
    String baseUrl = 'https://cmp.biznext.vn',
  }) async {
    final uri = Uri.tryParse(baseUrl);
    if (codeConfig.trim().isEmpty ||
        uri == null ||
        !['https', 'http'].contains(uri.scheme) ||
        uri.host.isEmpty ||
        (uri.path.isNotEmpty && uri.path != '/') ||
        uri.hasQuery ||
        uri.hasFragment ||
        uri.userInfo.isNotEmpty) {
      throw ArgumentError(
        'Provide codeConfig and an HTTP(S) origin without path or credentials.',
      );
    }
    await channel.invokeMethod<void>('configure', {
      'codeConfig': codeConfig.trim(),
      'baseUrl': baseUrl,
    });
  }

  static Future<void> clear() => channel.invokeMethod<void>('clear');
  static Future<bool> isGranted(String purposeKey) async =>
      await channel.invokeMethod<bool>('isGranted', {
        'purposeKey': purposeKey,
      }) ??
      false;
  static Future<String?> savedConsentId() =>
      channel.invokeMethod<String>('savedConsentId');
}

class ConsentFormController extends ChangeNotifier {
  MethodChannel? _channel;
  Map<String, dynamic> _status = const {'state': 'loading'};
  bool _busy = false;
  bool _disposed = false;
  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  Map<String, dynamic> get status => Map.unmodifiable(_status);
  bool get ready => _status['state'] == 'ready';
  bool get allRequiredGranted => _status['allRequiredGranted'] == true;
  bool get submitting => _busy;
  void _update(Map<String, dynamic> status) {
    _status = status;
    _notify();
  }

  Future<String> submit(Map<String, String> values) async {
    final channel = _channel;
    if (channel == null) {
      throw PlatformException(
        code: 'disposed',
        message: 'Form is not attached.',
      );
    }
    if (_busy) {
      throw PlatformException(
        code: 'busy',
        message: 'Submission already in progress.',
      );
    }
    if (!ready) {
      throw PlatformException(
        code: 'not_ready',
        message: 'Consent form is not ready.',
      );
    }
    _busy = true;
    _notify();
    try {
      final sent = await channel.invokeMapMethod<String, dynamic>('submit', {
        'values': values,
      });
      final id = sent?['consentId'];
      if (id is! String || id.isEmpty || sent?['ok'] != true) {
        throw PlatformException(
          code: 'submit_failed',
          message: 'SDK did not return a successful consentId.',
        );
      }
      return id;
    } finally {
      _busy = false;
      _notify();
    }
  }

  Future<void> reload() async {
    if (_busy) {
      throw PlatformException(
        code: 'busy',
        message: 'Submission already in progress.',
      );
    }
    final channel = _channel;
    if (channel == null) {
      throw PlatformException(
        code: 'disposed',
        message: 'Form is not attached.',
      );
    }
    await channel.invokeMethod<void>('reload');
  }
}

/// Native UI for standalone consent or a consent section inside a host form.
class CmpConsentForm extends StatefulWidget {
  const CmpConsentForm({
    super.key,
    required this.controller,
    this.embedded = true,
    this.values = const {},
    this.onSubmitted,
  });
  final ConsentFormController controller;
  final bool embedded;
  final Map<String, String> values;
  final ValueChanged<String>? onSubmitted;
  @override
  State<CmpConsentForm> createState() => _CmpConsentFormState();
}

class _CmpConsentFormState extends State<CmpConsentForm> {
  static const viewType = 'vn.fpt.cmp/form';
  MethodChannel? _channel;
  double height = 180;
  @override
  void didUpdateWidget(CmpConsentForm old) {
    super.didUpdateWidget(old);
    assert(
      identical(old.controller, widget.controller),
      'Keep the controller stable for the view lifetime.',
    );
    _channel?.invokeMethod<void>('values', {'values': widget.values});
  }

  Future<void> _created(int id) async {
    final channel = MethodChannel('$viewType/$id');
    _channel = channel;
    widget.controller._channel = channel;
    channel.setMethodCallHandler((call) async {
      if (!mounted) return;
      switch (call.method) {
        case 'onStatus':
          _apply(Map<String, dynamic>.from(call.arguments as Map));
          break;
        case 'onHeight':
          _height(call.arguments);
          break;
        case 'onSubmitted':
          final data = Map<String, dynamic>.from(call.arguments as Map);
          if (data['ok'] == true && data['consentId'] is String) {
            widget.onSubmitted?.call(data['consentId'] as String);
          }
          break;
      }
    });
    try {
      final status = await channel.invokeMapMethod<String, dynamic>('status');
      if (mounted && status != null) _apply(status);
    } on PlatformException catch (e) {
      if (mounted) {
        widget.controller._update({
          'state': 'failed',
          'message': e.message,
          'code': e.code,
        });
      }
    }
  }

  void _height(dynamic value) {
    if (value is num &&
        value.isFinite &&
        value > 0 &&
        value.ceilToDouble() != height) {
      setState(() => height = value.ceilToDouble());
    }
  }

  void _apply(Map<String, dynamic> status) {
    _height(status['height']);
    widget.controller._update(status);
  }

  @override
  void dispose() {
    final channel = _channel;
    channel?.setMethodCallHandler(null);
    channel?.invokeMethod<void>('dispose').catchError((Object _) {});
    if (identical(widget.controller._channel, channel)) {
      widget.controller._channel = null;
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (kIsWeb ||
        ![
          TargetPlatform.android,
          TargetPlatform.iOS,
        ].contains(defaultTargetPlatform)) {
      return const Text('CMP supports Android and iOS.');
    }
    final params = {'embedded': widget.embedded, 'values': widget.values};
    final view = defaultTargetPlatform == TargetPlatform.android
        ? AndroidView(
            viewType: viewType,
            creationParams: params,
            creationParamsCodec: const StandardMessageCodec(),
            onPlatformViewCreated: _created,
          )
        : UiKitView(
            viewType: viewType,
            creationParams: params,
            creationParamsCodec: const StandardMessageCodec(),
            onPlatformViewCreated: _created,
          );
    return SizedBox(height: height, child: view);
  }
}

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'config.dart';

class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({this.token});

  String? token;
  void Function()? onUnauthorized;

  Uri _uri(String path, [Map<String, dynamic>? query]) {
    final base = AppConfig.baseUrl.endsWith('/')
        ? AppConfig.baseUrl.substring(0, AppConfig.baseUrl.length - 1)
        : AppConfig.baseUrl;
    final uri = Uri.parse('$base$path');
    if (query == null || query.isEmpty) return uri;
    final params = <String, String>{};
    query.forEach((key, value) {
      if (value == null) return;
      if (value is String && value.isEmpty) return;
      params[key] = value.toString();
    });
    return uri.replace(queryParameters: {...uri.queryParameters, ...params});
  }

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        if (token != null && token!.isNotEmpty) 'Authorization': 'Bearer $token',
      };

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) {
    return _send(() => http.get(_uri(path, query), headers: _headers));
  }

  Future<dynamic> post(String path, {Object? body, Map<String, dynamic>? query}) {
    return _send(() => http.post(
          _uri(path, query),
          headers: _headers,
          body: body == null ? null : jsonEncode(body),
        ));
  }

  Future<dynamic> put(String path, {Object? body}) {
    return _send(() => http.put(
          _uri(path),
          headers: _headers,
          body: body == null ? null : jsonEncode(body),
        ));
  }

  Future<dynamic> delete(String path) {
    return _send(() => http.delete(_uri(path), headers: _headers));
  }

  Future<dynamic> _send(Future<http.Response> Function() request) async {
    try {
      final response = await request().timeout(const Duration(seconds: 25));
      return _parse(response);
    } on TimeoutException {
      throw ApiException('请求超时，请检查网络');
    } on SocketException {
      throw ApiException('网络不可用，已保留本地记录');
    } on http.ClientException catch (e) {
      throw ApiException('网络异常：${e.message}');
    }
  }

  dynamic _parse(http.Response response) {
    final text = utf8.decode(response.bodyBytes);
    dynamic body;
    try {
      body = text.isEmpty ? null : jsonDecode(text);
    } catch (_) {
      body = null;
    }

    if (response.statusCode == 401) {
      onUnauthorized?.call();
      throw ApiException('登录已过期，请重新登录', statusCode: 401);
    }

    if (body is Map && body.containsKey('code')) {
      final code = body['code'];
      if (code == 0) return body['data'];
      throw ApiException(
        (body['message'] ?? '请求失败').toString(),
        statusCode: response.statusCode,
      );
    }

    if (response.statusCode >= 200 && response.statusCode < 300) return body;
    throw ApiException('HTTP ${response.statusCode}', statusCode: response.statusCode);
  }

  Future<Map<String, dynamic>> upload(
    String filePath, {
    String bizType = 'app',
    String field = 'file',
  }) async {
    final request = http.MultipartRequest('POST', _uri('/system/upload'));
    if (token != null && token!.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    request.fields['bizType'] = bizType;
    request.files.add(await http.MultipartFile.fromPath(field, filePath));
    final streamed = await request.send().timeout(const Duration(seconds: 60));
    final response = await http.Response.fromStream(streamed);
    final data = _parse(response);
    if (data is Map<String, dynamic>) return data;
    return <String, dynamic>{'raw': data};
  }
}

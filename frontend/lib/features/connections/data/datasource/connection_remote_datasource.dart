import 'package:dio/dio.dart';
import '../model/connection_model.dart';
import '../model/connection_create_request.dart';
import '../model/connection_create_response.dart';
import '../model/connection_oauth_url_request.dart';
import '../model/connection_oauth_url_response.dart';
import '../model/connection_test_response.dart';
import '../model/connection_refresh_response.dart';
import '../model/connection_rotate_key_response.dart';

/// Remote data source for connections
class ConnectionRemoteDataSource {
  final Dio _dio;

  ConnectionRemoteDataSource(this._dio);

  /// Fetch all connections
  Future<List<ConnectionModel>> fetchConnections() async {
    try {
      final response = await _dio.get('/api/v1/connections');

      if (response.data['success'] == true) {
        final List<dynamic> data = response.data['data'];
        return data.map((json) => ConnectionModel.fromJson(json)).toList();
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Fetch connection by ID
  Future<ConnectionModel> fetchConnectionById(int id) async {
    try {
      final response = await _dio.get('/api/v1/connections/$id');

      if (response.data['success'] == true) {
        return ConnectionModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Create new connection (API Key)
  Future<ConnectionCreateResponse> createConnection(
    ConnectionCreateRequest request,
  ) async {
    try {
      final response = await _dio.post(
        '/api/v1/connections',
        data: request.toJson(),
      );

      if (response.data['success'] == true) {
        return ConnectionCreateResponse.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Delete connection
  Future<void> deleteConnection(int id) async {
    try {
      final response = await _dio.delete('/api/v1/connections/$id');

      if (response.data['success'] != true) {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Test connection
  Future<ConnectionTestResponse> testConnection(int id) async {
    try {
      final response = await _dio.post('/api/v1/connections/$id/test');

      if (response.data['success'] == true) {
        return ConnectionTestResponse.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Refresh OAuth token
  Future<ConnectionRefreshResponse> refreshConnection(int id) async {
    try {
      final response = await _dio.post('/api/v1/connections/$id/refresh');

      if (response.data['success'] == true) {
        return ConnectionRefreshResponse.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Rotate API Key
  Future<ConnectionRotateKeyResponse> rotateKey(int id) async {
    try {
      final response = await _dio.post('/api/v1/connections/$id/rotate-key');

      if (response.data['success'] == true) {
        return ConnectionRotateKeyResponse.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Request OAuth authorization URL
  Future<ConnectionOAuthUrlResponse> requestOAuthUrl(
    ConnectionOAuthUrlRequest request,
  ) async {
    try {
      final response = await _dio.post(
        '/api/v1/connections/oauth-url',
        data: request.toJson(),
      );

      if (response.data['success'] == true) {
        return ConnectionOAuthUrlResponse.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }
}

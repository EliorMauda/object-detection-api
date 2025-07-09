# Object Detection API Documentation

A REST API service for detecting objects in images using Hugging Face's DETR (DEtection TRansformer) model with Cloudinary storage integration.

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Examples](#examples)
- [Dashboard & Analytics](#dashboard--analytics)

## Overview

The Object Detection API provides endpoints for:
- Object detection in uploaded images
- Object detection from image URLs
- Real-time analytics and monitoring
- Detection history management
- System health monitoring

### Key Features

- **AI-Powered Detection**: Uses Facebook's DETR ResNet-101 model
- **Cloud Storage**: Automatic image storage via Cloudinary
- **Real-time Analytics**: Comprehensive dashboard metrics
- **Cross-Platform**: Supports web, mobile, and API clients
- **Scalable**: Built with Spring Boot for enterprise use

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Cloudinary account
- Hugging Face API token

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd object-detection-api
   ```

2. **Configure environment variables**
   ```bash
   export CLOUDINARY_CLOUD_NAME=your_cloud_name
   export CLOUDINARY_API_KEY=your_api_key
   export CLOUDINARY_API_SECRET=your_api_secret
   export HUGGINGFACE_API_TOKEN=your_token
   ```

3. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`

### Quick Test

```bash
curl -X GET http://localhost:8080/api/detect/health
```

## API Reference

### Base URL
```
Production: https://your-api-domain.com
Local: http://localhost:8080
```

### Authentication
Currently, the API uses Hugging Face API tokens for ML model access. No additional authentication is required for API endpoints.

---

## Detection Endpoints

### POST /api/detect
Detect objects in an uploaded image file.

**Request:**
```http
POST /api/detect
Content-Type: multipart/form-data

image: [image file]
```

**Response:**
```json
{
  "imageUrl": "https://res.cloudinary.com/...",
  "detectedObjects": [
    {
      "label": "person",
      "confidence": 0.95,
      "box": {
        "xMin": 100.5,
        "yMin": 200.3,
        "xMax": 300.7,
        "yMax": 450.9
      }
    }
  ],
  "processingTimeMs": 1250
}
```

### POST /api/detect/url
Detect objects in an image from URL.

**Request:**
```json
{
  "url": "https://example.com/image.jpg"
}
```

**Response:**
```json
{
  "imageUrl": "https://res.cloudinary.com/...",
  "detectedObjects": [...],
  "processingTimeMs": 980
}
```

### GET /api/detect/{detectionId}
Retrieve a specific detection record.

**Response:**
```json
{
  "id": "det_123456",
  "timestamp": "2025-06-06T10:30:00",
  "objects": [...],
  "processingTime": 1200,
  "device": "iPhone Safari",
  "imageUrl": "https://res.cloudinary.com/...",
  "fileName": "photo.jpg"
}
```

### GET /api/detect
Get all detection records with pagination and filtering.

**Query Parameters:**
- `page` (int): Page number (default: 0)
- `size` (int): Page size (default: 20, max: 100)
- `category` (string): Filter by object category
- `device` (string): Filter by device type
- `search` (string): Search in filename/device

**Response:**
```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

### DELETE /api/detect/{detectionId}
Delete a detection record.

**Response:**
```json
{
  "success": true,
  "message": "Detection deleted successfully",
  "detectionId": "det_123456",
  "deletedAt": "2025-06-06T10:35:00",
  "deletedBy": "iPhone Safari"
}
```

---

## Dashboard Endpoints

### GET /api/dashboard/metrics
Get real-time dashboard metrics.

**Response:**
```json
{
  "activeSessions": 42,
  "apiCalls": 1250,
  "responseTime": 850,
  "errorRate": 2.1,
  "lastUpdated": "2025-06-06T10:30:00"
}
```

### GET /api/dashboard/chart-data
Get chart data for API usage over time.

**Query Parameters:**
- `timeframe` (string): hour, day, week, month

**Response:**
```json
{
  "labels": ["09:00", "12:00", "15:00", "18:00"],
  "data": [45, 67, 52, 38]
}
```

### GET /api/dashboard/system-status
Get system health status.

**Response:**
```json
[
  {
    "service": "API Server",
    "status": "Online",
    "statusClass": "success",
    "load": 45,
    "uptime": "7d 12h 24m",
    "lastUpdate": "Just now"
  }
]
```

---

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `PORT` | Server port | No | 8080 |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | Yes | - |
| `CLOUDINARY_API_KEY` | Cloudinary API key | Yes | - |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | Yes | - |
| `HUGGINGFACE_API_TOKEN` | Hugging Face API token | Yes | - |

### Application Properties

```properties
# Server configuration
server.port=${PORT:8080}

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# API endpoints
huggingface.api.url=https://api-inference.huggingface.co/models/facebook/detr-resnet-101
```

---

## Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request (invalid input) |
| 404 | Resource not found |
| 413 | File too large (>10MB) |
| 500 | Internal server error |

### Error Response Format

```json
{
  "error": "Description of the error",
  "processingTimeMs": 150
}
```

### Common Errors

- **File too large**: Files must be under 10MB
- **Invalid URL**: URLs must start with http:// or https://
- **Processing timeout**: Hugging Face API timeout
- **Invalid image format**: Unsupported image type

---

## Examples

### Upload Image (cURL)

```bash
curl -X POST http://localhost:8080/api/detect \
  -H "Content-Type: multipart/form-data" \
  -H "X-Client-Type: API Client" \
  -F "image=@/path/to/image.jpg"
```

### Detect from URL (JavaScript)

```javascript
const response = await fetch('http://localhost:8080/api/detect/url', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Type': 'Web Portal'
  },
  body: JSON.stringify({
    url: 'https://example.com/image.jpg'
  })
});

const result = await response.json();
console.log('Detected objects:', result.detectedObjects);
```

### Python Integration

```python
import requests

# Upload file
with open('image.jpg', 'rb') as f:
    response = requests.post(
        'http://localhost:8080/api/detect',
        files={'image': f},
        headers={'X-Client-Type': 'Python Script'}
    )

result = response.json()
for obj in result['detectedObjects']:
    print(f"Found {obj['label']} with {obj['confidence']:.2%} confidence")
```

### Java/Android Integration

```java
OkHttpClient client = new OkHttpClient();

RequestBody fileBody = RequestBody.create(
    MediaType.parse("image/jpeg"), 
    imageFile
);

MultipartBody requestBody = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("image", "image.jpg", fileBody)
    .build();

Request request = new Request.Builder()
    .url("http://localhost:8080/api/detect")
    .post(requestBody)
    .addHeader("X-Client-Type", "Android App")
    .build();

Response response = client.newCall(request).execute();
```

---

## Dashboard & Analytics

### Real-time Metrics

The API provides comprehensive analytics through dashboard endpoints:

- **Usage Statistics**: API calls, response times, error rates
- **Object Categories**: Distribution of detected object types
- **Device Analytics**: Client device and browser breakdown
- **Performance Monitoring**: System load and health status

### Device Detection

The API automatically detects client information from request headers:

- **Web Browsers**: Chrome, Safari, Firefox, Edge
- **Mobile Devices**: iPhone, Android, iPad
- **API Clients**: OkHttp, cURL, Postman
- **Custom Headers**: X-Client-Type, X-Device-Info

### Monitoring Integration

For production monitoring, the API exposes:

- Health check endpoint: `/api/detect/health`
- Metrics endpoint: `/api/dashboard/metrics`
- Error logging with structured format
- Performance timing for all operations

---

## API Documentation (Swagger)

Interactive API documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

This provides a complete interface for testing all endpoints with example requests and responses.

---

## Support

For issues, questions, or contributions:

- **Issues**: Create an issue in the GitHub repository
- **Documentation**: Check this documentation and inline code comments
- **API Changes**: Review the changelog for version updates

### Rate Limits

Currently, there are no rate limits implemented. For production use, consider implementing:

- Request rate limiting per IP/client
- File upload frequency limits
- Hugging Face API quota management

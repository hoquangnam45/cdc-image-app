# CDC Image Platform - Diagrams

This directory contains draw.io compatible diagrams for the CDC Image Platform system architecture and data flows.

## How to Use

1. **Open draw.io**: Go to [app.diagrams.net](https://app.diagrams.net) or use the desktop version
2. **Import diagrams**: Click "File" → "Open from" → "Device" and select any `.drawio` file
3. **Edit and export**: Modify as needed and export to PNG, SVG, PDF, etc.

## Available Diagrams

### 1. System Architecture (`system-architecture.drawio`)
- **Purpose**: High-level overview of all system components
- **Shows**: 
  - Frontend (Vue.js)
  - Backend services (Auth, Image, Event Handler)
  - Google Cloud Platform services (KMS, Storage, Pub/Sub, PostgreSQL)
  - Common module dependencies
  - Service communication patterns

### 2. Authentication Flow (`authentication-flow.drawio`)
- **Purpose**: Detailed JWT authentication process with KMS integration
- **Shows**:
  - Login/registration flow
  - KMS token signing process
  - Public key caching for validation
  - Automatic token refresh mechanism
  - Database interactions for user and token management

### 3. Image Upload Flow (`image-upload-flow.drawio`)
- **Purpose**: Complete image upload and processing workflow
- **Shows**:
  - Presigned URL generation
  - Direct upload to Google Cloud Storage
  - Event-driven processing via Pub/Sub
  - Image validation and thumbnail generation
  - Database state transitions throughout the process

### 4. Database Schema (`database-schema.drawio`)
- **Purpose**: Complete database structure and relationships
- **Shows**:
  - All tables with fields and data types
  - Primary and foreign key relationships
  - Enum definitions (IMAGE_STATUS, JOB_STATUS)
  - Common query patterns
  - Key design patterns and principles

## Key Features Illustrated

### Architecture Highlights
- **Microservices Design**: Clean separation of concerns
- **Event-Driven Processing**: Scalable async operations
- **Cloud-Native**: Proper use of GCP managed services
- **Security-First**: JWT with KMS integration

### Data Flow Patterns
- **Status-Driven State Machine**: Images progress through defined states
- **Direct Upload**: Eliminates server bottlenecks
- **Referential Integrity**: Foreign key constraints ensure consistency
- **Audit Trail**: Complete lifecycle tracking

### Security Model
- **Centralized Key Management**: Google KMS for JWT operations
- **Distributed Validation**: Public key caching for performance
- **Automatic Token Refresh**: Seamless user experience
- **HTTP-Only Cookies**: Secure token storage

## Customization

These diagrams are fully editable in draw.io. You can:
- Modify colors and styling
- Add new components or flows
- Export to different formats (PNG, SVG, PDF)
- Create additional views or drill-down diagrams
- Update as the system evolves

## Technical Notes

- All diagrams use standard UML and system architecture notation
- Colors are consistent across diagrams for component types
- Database diagrams use entity-relationship notation
- Sequence diagrams follow UML standards
- File format is draw.io XML for maximum compatibility


# CDC Image Platform - Mermaid Diagrams

## 1. Image Upload and Processing Flow (Sequence Diagram)

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant ImageService
    participant Database
    participant PubSub as Google Pub/Sub
    participant BackgroundJob as Background Job
    participant GCS as Google Cloud Storage

    Note over User, GCS: Phase 1: Image Upload Request
    User->>Frontend: Select and upload image
    Frontend->>ImageService: POST /api/image/upload<br/>[filenames] + JWT
    
    ImageService->>GCS: Generate presigned URLs<br/>SignUrl(bucket, object, PUT)
    GCS-->>ImageService: Presigned URLs
    
    ImageService->>Database: INSERT INTO user_image<br/>VALUES(uuid, user_id, null,<br/>'PENDING', filename, now())
    Database-->>ImageService: Success
    
    ImageService-->>Frontend: Return upload URLs<br/>[{id, filename, url, expiry}]
    Frontend-->>User: Show upload progress
    
    User->>GCS: Direct upload files<br/>PUT to presigned URLs
    GCS-->>User: Upload complete

    Note over User, GCS: Phase 2: Event-Driven Processing
    GCS->>PubSub: Publish upload event<br/>(Cloud Audit Log)
    PubSub->>BackgroundJob: Event message<br/>Image uploaded notification
    
    BackgroundJob->>Database: UPDATE user_image<br/>SET status = 'RUNNING'<br/>WHERE id = ?
    
    BackgroundJob->>GCS: Download original image<br/>for processing
    GCS-->>BackgroundJob: Image data
    
    Note over BackgroundJob: Validate & Process Image<br/>- Check MIME type<br/>- Extract dimensions<br/>- Compress image<br/>- Generate thumbnails
    
    BackgroundJob->>Database: INSERT INTO uploaded_image<br/>VALUES(uuid, width, height,<br/>file_size, gcs_path, mime_type,<br/>md5_hash, 'UPLOADED', now())
    Database-->>BackgroundJob: New image ID
    
    BackgroundJob->>GCS: Upload compressed image<br/>and thumbnails
    GCS-->>BackgroundJob: Upload complete
    
    BackgroundJob->>Database: UPDATE user_image SET<br/>uploaded_image_id = new_uuid,<br/>status = 'UPLOADED'<br/>WHERE id = ?
    
    BackgroundJob->>Database: INSERT INTO generated_image<br/>VALUES(thumbnails metadata)
    
    Note over User, GCS: Phase 3: Completion
    Frontend->>ImageService: GET /api/image/list<br/>(periodic refresh)
    ImageService->>Database: SELECT user images<br/>with processing status
    Database-->>ImageService: Image list with status
    ImageService-->>Frontend: Updated image list
    Frontend-->>User: Show processed images
```

## 2. System Architecture Flow (Flowchart)

```mermaid
flowchart TD
    User([User]) --> Frontend[Frontend<br/>Vue.js]
    Frontend --> ImageService[Image Service<br/>Port 8082]
    
    ImageService --> Database[(PostgreSQL<br/>Database)]
    ImageService --> GCS[Google Cloud<br/>Storage]
    
    GCS --> PubSub[Google Pub/Sub<br/>Event Queue]
    PubSub --> BackgroundJob[Background Job<br/>Image Event Handler]
    
    BackgroundJob --> Database
    BackgroundJob --> GCS
    BackgroundJob --> ProcessingEngine[Image Processing<br/>Engine]
    
    ProcessingEngine --> Compression[Image Compression]
    ProcessingEngine --> Thumbnails[Thumbnail Generation]
    ProcessingEngine --> Validation[Image Validation]
    
    Database --> UserTable[user_image table<br/>PENDING → RUNNING → UPLOADED]
    Database --> UploadedTable[uploaded_image table<br/>Original metadata]
    Database --> GeneratedTable[generated_image table<br/>Thumbnail metadata]
    
    style User fill:#e1d5e7
    style Frontend fill:#e1d5e7
    style ImageService fill:#fff2cc
    style BackgroundJob fill:#f8cecc
    style Database fill:#f5f5f5
    style GCS fill:#f5f5f5
    style PubSub fill:#f5f5f5
    style ProcessingEngine fill:#d5e8d4
```

## 3. Image Processing State Diagram

```mermaid
stateDiagram-v2
    [*] --> PENDING: User uploads image
    
    PENDING --> RUNNING: Background job<br/>picks up event
    
    RUNNING --> UPLOADED: Processing<br/>successful
    RUNNING --> INVALID: Validation<br/>failed
    RUNNING --> EXPIRED: Processing<br/>timeout
    
    UPLOADED --> [*]: Image ready<br/>for use
    INVALID --> [*]: Image rejected
    EXPIRED --> [*]: Processing failed
    
    note right of PENDING
        • user_image record created
        • Presigned URL generated
        • Waiting for upload
    end note
    
    note right of RUNNING
        • Download from GCS
        • Validate image format
        • Extract metadata
        • Compress image
        • Generate thumbnails
    end note
    
    note right of UPLOADED
        • uploaded_image record created
        • generated_image records created
        • user_image.uploaded_image_id updated
        • Ready for display
    end note
```

## 4. Database Entity Relationship (Simplified)

```mermaid
erDiagram
    USER {
        uuid id PK
        varchar username
        varchar email
        varchar password_hash
        timestamp created_at
    }
    
    USER_IMAGE {
        uuid id PK
        uuid user_id FK
        uuid uploaded_image_id FK
        enum status
        varchar file_name
        timestamp created_at
        timestamp expired_at
    }
    
    UPLOADED_IMAGE {
        uuid id PK
        int width
        int height
        int file_size
        varchar file_path
        varchar file_type
        varchar file_hash
        enum status
        timestamp created_at
    }
    
    GENERATED_IMAGE {
        uuid id PK
        uuid image_id FK
        uuid configuration_id FK
        int width
        int height
        varchar file_path
        timestamp created_at
    }
    
    PROCESSING_JOB_CONFIGURATION {
        uuid id PK
        int width
        int height
        decimal scale
        boolean keep_ratio
        int quality
        varchar description
    }
    
    USER ||--o{ USER_IMAGE : "uploads"
    USER_IMAGE }o--|| UPLOADED_IMAGE : "references"
    UPLOADED_IMAGE ||--o{ GENERATED_IMAGE : "generates"
    PROCESSING_JOB_CONFIGURATION ||--o{ GENERATED_IMAGE : "configures"
```

## 5. Event-Driven Architecture Flow

```mermaid
flowchart LR
    subgraph "Upload Phase"
        A[User Upload] --> B[Image Service]
        B --> C[Store Metadata<br/>Status: PENDING]
        B --> D[Generate Presigned URL]
        D --> E[Direct Upload to GCS]
    end
    
    subgraph "Event Processing"
        E --> F[GCS Audit Log]
        F --> G[Pub/Sub Event]
        G --> H[Background Job<br/>Event Handler]
    end
    
    subgraph "Image Processing"
        H --> I[Update Status<br/>RUNNING]
        H --> J[Download Image]
        J --> K[Validate & Process]
        K --> L[Compress Image]
        K --> M[Generate Thumbnails]
        L --> N[Upload to GCS]
        M --> N
        N --> O[Update Database<br/>Status: UPLOADED]
    end
    
    subgraph "Database Updates"
        C --> P[(user_image)]
        I --> P
        O --> P
        O --> Q[(uploaded_image)]
        O --> R[(generated_image)]
    end
    
    style A fill:#e1d5e7
    style B fill:#fff2cc
    style H fill:#f8cecc
    style P fill:#f5f5f5
    style Q fill:#f5f5f5
    style R fill:#f5f5f5
```

## 6. Detailed Processing Pipeline

```mermaid
flowchart TD
    Start([Image Upload Event]) --> Download[Download Image<br/>from GCS]
    
    Download --> Validate{Validate Image}
    Validate -->|Invalid| SetInvalid[Set Status: INVALID]
    Validate -->|Valid| Extract[Extract Metadata<br/>Width, Height, Size, Type]
    
    Extract --> Process[Process Image]
    
    subgraph "Processing Steps"
        Process --> Compress[Compress Original<br/>Optimize file size]
        Process --> Thumb1[Generate Thumbnail<br/>150x150]
        Process --> Thumb2[Generate Thumbnail<br/>300x300]
        Process --> Thumb3[Generate Thumbnail<br/>600x600]
    end
    
    Compress --> UploadOrig[Upload Compressed<br/>to GCS]
    Thumb1 --> UploadThumb1[Upload Thumbnail<br/>to GCS]
    Thumb2 --> UploadThumb2[Upload Thumbnail<br/>to GCS]
    Thumb3 --> UploadThumb3[Upload Thumbnail<br/>to GCS]
    
    UploadOrig --> UpdateDB[Update Database]
    UploadThumb1 --> UpdateDB
    UploadThumb2 --> UpdateDB
    UploadThumb3 --> UpdateDB
    
    UpdateDB --> Complete[Set Status: UPLOADED]
    SetInvalid --> End([End])
    Complete --> End
    
    style Start fill:#e1d5e7
    style Process fill:#d5e8d4
    style UpdateDB fill:#f5f5f5
    style Complete fill:#d4edda
    style SetInvalid fill:#f8d7da
```

## How to Use These Diagrams

1. **Copy the Mermaid code** from any section above
2. **Paste into any Mermaid-compatible tool**:
   - GitHub/GitLab (in README.md files)
   - Mermaid Live Editor: https://mermaid.live
   - VS Code with Mermaid extension
   - Notion, Obsidian, or other documentation tools
3. **Customize as needed** by modifying the code
4. **Export** to PNG, SVG, or PDF formats

## Key Benefits of This Flow

- ✅ **Asynchronous Processing**: Non-blocking user experience
- ✅ **Event-Driven**: Scalable and decoupled architecture  
- ✅ **Direct Upload**: Eliminates server bottlenecks
- ✅ **Status Tracking**: Complete visibility into processing state
- ✅ **Error Handling**: Graceful failure modes with status updates
- ✅ **Thumbnail Generation**: Automatic optimization for different use cases


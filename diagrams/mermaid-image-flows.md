# Image Upload and Viewing Flows - Mermaid Diagrams

## 1. Image Upload Flow (Complete Process)

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant LoadBalancer as Google Cloud<br/>Load Balancer
    participant ImageService
    participant GCS as Google Cloud<br/>Storage
    participant Database
    participant PubSub as Google Pub/Sub
    participant BackgroundJob as Background Job<br/>(Event Handler)

    Note over User, BackgroundJob: Phase 1: Upload Request
    User->>Frontend: Select images<br/>["img1.jpg", "img2.png"]
    Frontend->>LoadBalancer: POST /api/image/upload<br/>[filenames] + JWT
    LoadBalancer->>ImageService: Route to healthy<br/>Image Service instance
    
    ImageService->>GCS: Generate presigned URLs<br/>SignUrl(bucket, object, PUT)
    GCS-->>ImageService: Presigned URLs with expiry
    
    ImageService->>Database: Store image metadata<br/>status = 'PENDING'
    Database-->>ImageService: Success
    
    ImageService-->>LoadBalancer: Upload URLs response<br/>[{id, filename, url, expiry}]
    LoadBalancer-->>Frontend: Response with URLs
    Frontend-->>User: Show upload interface

    Note over User, BackgroundJob: Phase 2: Direct Upload
    User->>GCS: Direct upload files<br/>PUT to presigned URLs
    GCS-->>User: Upload complete confirmation

    Note over User, BackgroundJob: Phase 3: Event-Driven Processing
    GCS->>PubSub: Publish upload event<br/>(Cloud Audit Log)
    PubSub->>BackgroundJob: Event message<br/>Image uploaded notification
    
    BackgroundJob->>Database: Update status = 'RUNNING'
    BackgroundJob->>GCS: Download original image
    GCS-->>BackgroundJob: Image data
    
    Note over BackgroundJob: Process Image<br/>• Validate format<br/>• Extract metadata<br/>• Compress image<br/>• Generate thumbnails
    
    BackgroundJob->>GCS: Upload processed images<br/>and thumbnails
    GCS-->>BackgroundJob: Upload complete
    
    BackgroundJob->>Database: Store processed metadata<br/>status = 'UPLOADED'
    Database-->>BackgroundJob: Success

    Note over User, BackgroundJob: Phase 4: Completion Notification
    Frontend->>LoadBalancer: GET /api/image/list<br/>(periodic check)
    LoadBalancer->>ImageService: Route request
    ImageService->>Database: Query user images<br/>with current status
    Database-->>ImageService: Updated image list
    ImageService-->>LoadBalancer: Image list response
    LoadBalancer-->>Frontend: Response data
    Frontend-->>User: Show processed images
```

## 2. Image Viewing Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant LoadBalancer as Google Cloud<br/>Load Balancer
    participant ImageService
    participant Database
    participant GCS as Google Cloud<br/>Storage

    Note over User, GCS: Image Gallery/Dashboard View
    User->>Frontend: Navigate to dashboard<br/>or image gallery
    Frontend->>LoadBalancer: GET /api/image/list<br/>+ JWT token
    LoadBalancer->>ImageService: Route to healthy<br/>service instance
    
    ImageService->>Database: Query user images<br/>JOIN with thumbnails
    Database-->>ImageService: Image metadata<br/>+ thumbnail info
    
    loop For each image
        ImageService->>GCS: Generate download URLs<br/>for image + thumbnails
        GCS-->>ImageService: Presigned download URLs
    end
    
    ImageService-->>LoadBalancer: Image list with URLs<br/>[{id, filename, status, urls, thumbnails}]
    LoadBalancer-->>Frontend: Response with image data
    
    Frontend-->>User: Display image gallery<br/>with thumbnails

    Note over User, GCS: View Full Resolution Image
    User->>Frontend: Click on image<br/>to view full size
    Frontend->>GCS: Direct download<br/>using presigned URL
    GCS-->>Frontend: Full resolution image
    Frontend-->>User: Display full image<br/>in viewer/modal
```

## 3. Image Upload Architecture Flow

```mermaid
flowchart TD
    User([User]) --> Frontend[Frontend<br/>Vue.js]
    Frontend --> LoadBalancer[Google Cloud<br/>Load Balancer]
    
    LoadBalancer --> ImageService1[Image Service<br/>Instance 1]
    LoadBalancer --> ImageService2[Image Service<br/>Instance 2]
    LoadBalancer --> ImageServiceN[Image Service<br/>Instance N]
    
    ImageService1 --> Database[(PostgreSQL<br/>Database)]
    ImageService2 --> Database
    ImageServiceN --> Database
    
    ImageService1 --> GCS[Google Cloud<br/>Storage]
    ImageService2 --> GCS
    ImageServiceN --> GCS
    
    GCS --> PubSub[Google Pub/Sub<br/>Event Queue]
    PubSub --> BackgroundJob[Background Job<br/>Event Handler]
    
    BackgroundJob --> Database
    BackgroundJob --> GCS
    BackgroundJob --> ProcessingEngine[Image Processing<br/>Engine]
    
    subgraph "Processing Pipeline"
        ProcessingEngine --> Validation[Image Validation<br/>MIME type, size]
        ProcessingEngine --> Compression[Image Compression<br/>Optimize file size]
        ProcessingEngine --> Thumbnails[Thumbnail Generation<br/>Multiple sizes]
    end
    
    subgraph "Database Tables"
        Database --> UserImageTable[user_image<br/>PENDING → RUNNING → UPLOADED]
        Database --> UploadedImageTable[uploaded_image<br/>Original metadata]
        Database --> GeneratedImageTable[generated_image<br/>Thumbnail metadata]
    end
    
    style User fill:#e1d5e7
    style Frontend fill:#e1d5e7
    style LoadBalancer fill:#4285f4
    style ImageService1 fill:#fff2cc
    style ImageService2 fill:#fff2cc
    style ImageServiceN fill:#fff2cc
    style BackgroundJob fill:#f8cecc
    style ProcessingEngine fill:#d5e8d4
    style Database fill:#f5f5f5
    style GCS fill:#f5f5f5
    style PubSub fill:#f5f5f5
```

## 4. Image Processing State Flow

```mermaid
stateDiagram-v2
    [*] --> UploadRequest: User selects images
    
    UploadRequest --> GeneratingURLs: Request presigned URLs
    GeneratingURLs --> PendingUpload: URLs generated, status PENDING
    
    PendingUpload --> Uploading: User uploads files
    Uploading --> UploadComplete: Files uploaded to GCS
    
    UploadComplete --> EventTriggered: GCS audit log event
    EventTriggered --> Processing: Background job starts, status RUNNING
    
    Processing --> Validating: Download and validate
    Validating --> Invalid: Validation failed, status INVALID
    Validating --> Compressing: Validation passed
    
    Compressing --> GeneratingThumbnails: Compress original
    GeneratingThumbnails --> UploadingProcessed: Generate thumbnails
    UploadingProcessed --> Complete: Upload to GCS, status UPLOADED
    
    Complete --> [*]: Ready for viewing
    Invalid --> [*]: Processing failed
    
    note right of PendingUpload
        user_image record created
        Presigned URLs generated
        Waiting for file upload
    end note
    
    note right of Processing
        Download from GCS
        Validate image format
        Extract metadata
        Process image
    end note
    
    note right of Complete
        uploaded_image record
        generated_image records
        Ready for display
    end note
```

## 5. Image Viewing Architecture

```mermaid
flowchart LR
    subgraph "User Request"
        A[User Views<br/>Gallery]
    end
    
    subgraph "Load Balanced Services"
        B[Google Cloud<br/>Load Balancer]
        C[Image Service<br/>Instance 1]
        D[Image Service<br/>Instance 2]
        E[Image Service<br/>Instance N]
    end
    
    subgraph "Data Layer"
        F[(PostgreSQL<br/>Database)]
        G[Google Cloud<br/>Storage]
    end
    
    subgraph "Response Processing"
        H[Query Image<br/>Metadata]
        I[Generate Download<br/>URLs]
        J[Return Image<br/>List + URLs]
    end
    
    A --> B
    B --> C
    B --> D
    B --> E
    
    C --> H
    D --> H
    E --> H
    
    H --> F
    H --> I
    I --> G
    I --> J
    
    J --> B
    B --> A
    
    style A fill:#e1d5e7
    style B fill:#4285f4
    style C fill:#fff2cc
    style D fill:#fff2cc
    style E fill:#fff2cc
    style F fill:#f5f5f5
    style G fill:#f5f5f5
    style H fill:#d5e8d4
    style I fill:#d5e8d4
    style J fill:#d5e8d4
```

## 6. Complete Image Lifecycle

```mermaid
flowchart TD
    Start([User Selects Images]) --> Upload[Upload Request]
    
    subgraph "Upload Phase"
        Upload --> URLs[Generate Presigned URLs]
        URLs --> Store[Store Metadata<br/>Status: PENDING]
        Store --> DirectUpload[Direct Upload to GCS]
    end
    
    subgraph "Processing Phase"
        DirectUpload --> Event[GCS Event Trigger]
        Event --> Background[Background Job Processing]
        Background --> Validate[Validate Image]
        Validate --> Process[Compress & Generate Thumbnails]
        Process --> UpdateDB[Update Database<br/>Status: UPLOADED]
    end
    
    subgraph "Viewing Phase"
        UpdateDB --> Gallery[User Views Gallery]
        Gallery --> Query[Query Image List]
        Query --> GenerateURLs[Generate Download URLs]
        GenerateURLs --> Display[Display Images]
    end
    
    subgraph "Error Handling"
        Validate --> Invalid[Mark as INVALID]
        Process --> Failed[Processing Failed]
        Invalid --> End1[End]
        Failed --> End2[End]
    end
    
    Display --> ViewFull[View Full Resolution]
    ViewFull --> End3([End])
    
    style Start fill:#e1d5e7
    style Upload fill:#fff2cc
    style Background fill:#f8cecc
    style Gallery fill:#d5e8d4
    style Invalid fill:#f8d7da
    style Failed fill:#f8d7da
```

## Key Benefits of This Image Flow

- 🚀 **High Performance**: Direct upload to GCS eliminates server bottlenecks
- ⚖️ **Load Balanced**: Multiple service instances handle requests efficiently
- 🔄 **Asynchronous Processing**: Non-blocking user experience during image processing
- 📱 **Responsive**: Automatic thumbnail generation for different screen sizes
- 🛡️ **Secure**: Presigned URLs with expiration for controlled access
- 📊 **Status Tracking**: Complete visibility into processing pipeline
- 🎯 **Scalable**: Event-driven architecture handles high volume
- 💾 **Optimized Storage**: Compressed images and multiple thumbnail sizes

## How to Use

1. Copy any Mermaid code block above
2. Paste into:
   - GitHub/GitLab markdown files
   - [Mermaid Live Editor](https://mermaid.live)
   - Documentation tools (Notion, Obsidian, etc.)
   - VS Code with Mermaid extension
3. The diagrams will render automatically
4. Export to PNG/SVG if needed for presentations

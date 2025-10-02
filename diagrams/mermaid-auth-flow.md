# Authentication Flow - Mermaid Diagrams

## 1. Login/Registration Flow with KMS Integration

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant LoadBalancer as Google Cloud<br/>Load Balancer
    participant AuthService
    participant KMS as Google KMS
    participant Database

    Note over User, Database: User Login/Registration Flow
    User->>Frontend: Enter credentials<br/>(username, password)
    Frontend->>LoadBalancer: POST /api/auth/login<br/>or /api/auth/register
    LoadBalancer->>AuthService: Route to healthy<br/>Auth Service instance
    
    AuthService->>Database: Validate user credentials<br/>or create new user
    Database-->>AuthService: User data
    
    Note over AuthService: Create JWT claims<br/>(user_id, username, email, etc.)
    
    AuthService->>KMS: Sign JWT with private key<br/>AsymmetricSign(claims)
    KMS-->>AuthService: Signed JWT token
    
    AuthService->>Database: Store refresh token<br/>with expiration
    Database-->>AuthService: Success
    
    AuthService-->>LoadBalancer: JWT + HTTP-only cookies<br/>(access & refresh tokens)
    LoadBalancer-->>Frontend: Response with tokens
    Frontend-->>User: Authentication success<br/>Redirect to dashboard
```

## 2. JWT Validation Flow (Microservices)

```mermaid
sequenceDiagram
    participant User
    participant LoadBalancer as Google Cloud<br/>Load Balancer
    participant ImageService
    participant KMS as Google KMS
    participant Cache as Public Key Cache

    Note over User, Cache: API Request with JWT Validation
    User->>LoadBalancer: API request with JWT<br/>Authorization: Bearer <token>
    LoadBalancer->>ImageService: Route to healthy<br/>service instance
    
    ImageService->>Cache: Check cached public key
    
    alt Public key not cached
        ImageService->>KMS: Fetch public key
        KMS-->>ImageService: Public key
        ImageService->>Cache: Cache public key
    end
    
    Note over ImageService: Validate JWT signature<br/>using cached public key
    
    alt JWT valid
        ImageService-->>LoadBalancer: API response<br/>with requested data
        LoadBalancer-->>User: Response data
    else JWT invalid
        ImageService-->>LoadBalancer: 401 Unauthorized
        LoadBalancer-->>User: 401 Unauthorized
    end
```

## 3. Automatic Token Refresh Flow

```mermaid
sequenceDiagram
    participant Frontend
    participant Timer as Refresh Timer
    participant LoadBalancer as Google Cloud<br/>Load Balancer
    participant AuthService
    participant KMS as Google KMS
    participant Database

    Note over Frontend, Database: Automatic Token Refresh (60s before expiry)
    Timer->>Frontend: Token expires soon<br/>(60 seconds warning)
    
    Frontend->>LoadBalancer: POST /api/auth/refresh<br/>(with refresh cookie)
    LoadBalancer->>AuthService: Route to healthy<br/>Auth Service instance
    
    AuthService->>Database: Validate refresh token<br/>and get user data
    Database-->>AuthService: User data
    
    AuthService->>KMS: Sign new JWT<br/>with updated expiry
    KMS-->>AuthService: New signed JWT
    
    AuthService->>Database: Store new refresh token<br/>Delete old token
    Database-->>AuthService: Success
    
    AuthService-->>LoadBalancer: New JWT + cookies<br/>Updated expiration times
    LoadBalancer-->>Frontend: Response with new tokens
    
    Frontend->>Timer: Schedule next refresh<br/>(based on new expiry)
```

## 4. Complete Authentication Architecture

```mermaid
flowchart TD
    User([User]) --> Frontend[Frontend<br/>Vue.js + Pinia]
    
    Frontend --> LoadBalancer[Google Cloud<br/>Load Balancer]
    
    LoadBalancer --> AuthService[Auth Service<br/>Port 8081]
    LoadBalancer --> ImageService[Image Service<br/>Port 8082]
    LoadBalancer --> EventHandler[Event Handler<br/>Service]
    
    AuthService --> KMS[Google KMS<br/>JWT Signing]
    AuthService --> Database[(PostgreSQL<br/>User & Token Data)]
    
    ImageService --> KMSCache[KMS Public Key<br/>Cache]
    EventHandler --> KMSCache
    
    KMSCache --> KMS
    
    subgraph "Authentication Flow"
        Login[Login/Register] --> JWTSign[JWT Signing<br/>with KMS]
        JWTSign --> TokenStore[Store Refresh<br/>Token]
        TokenStore --> Success[Auth Success]
    end
    
    subgraph "Validation Flow"
        APIRequest[API Request] --> ValidateJWT[Validate JWT<br/>with Public Key]
        ValidateJWT --> Authorized[Authorized<br/>Request]
    end
    
    subgraph "Refresh Flow"
        Timer[Refresh Timer] --> RefreshToken[Refresh JWT]
        RefreshToken --> NewJWT[New JWT<br/>+ Cookies]
        NewJWT --> RescheduleTimer[Reschedule<br/>Timer]
    end
    
    style User fill:#e1d5e7
    style Frontend fill:#e1d5e7
    style LoadBalancer fill:#4285f4
    style AuthService fill:#d5e8d4
    style KMS fill:#f5f5f5
    style Database fill:#f5f5f5
    style Login fill:#fff2cc
    style APIRequest fill:#f8cecc
    style Timer fill:#dae8fc
```

## 5. JWT Token Lifecycle

```mermaid
stateDiagram-v2
    [*] --> LoginRequest: User enters credentials
    
    LoginRequest --> ValidatingUser: Check user exists
    ValidatingUser --> CreatingJWT: User valid
    ValidatingUser --> LoginFailed: Invalid credentials
    
    CreatingJWT --> SigningWithKMS: Create JWT claims
    SigningWithKMS --> TokenSigned: KMS signs token
    
    TokenSigned --> StoringRefreshToken: Store refresh token
    StoringRefreshToken --> AuthSuccess: Send JWT + cookies
    
    AuthSuccess --> TokenActive: User authenticated
    
    TokenActive --> RefreshNeeded: 60s before expiry
    RefreshNeeded --> RefreshingToken: Auto refresh
    RefreshingToken --> TokenActive: New JWT issued
    
    TokenActive --> TokenExpired: No refresh
    TokenExpired --> LoginRequest: Re-authentication needed
    
    LoginFailed --> [*]
    AuthSuccess --> LogoutRequest: User logs out
    LogoutRequest --> [*]
```

## 6. Microservice JWT Validation Pattern

```mermaid
flowchart LR
    subgraph "Client Request"
        A[API Request<br/>with JWT]
    end
    
    subgraph "Service Validation"
        B{Public Key<br/>Cached?}
        C[Fetch from KMS]
        D[Use Cached Key]
        E[Validate JWT<br/>Signature]
        F{JWT Valid?}
    end
    
    subgraph "Response"
        G[Process Request]
        H[Return 401<br/>Unauthorized]
    end
    
    A --> B
    B -->|No| C
    B -->|Yes| D
    C --> E
    D --> E
    E --> F
    F -->|Valid| G
    F -->|Invalid| H
    
    style A fill:#e1d5e7
    style B fill:#fff2cc
    style C fill:#f5f5f5
    style E fill:#d5e8d4
    style G fill:#d4edda
    style H fill:#f8d7da
```

## Key Benefits of This Authentication Flow

- 🔐 **Centralized Key Management**: Google KMS handles all cryptographic operations
- ⚡ **Performance Optimized**: Public key caching reduces KMS API calls
- 🔄 **Seamless UX**: Automatic token refresh prevents login interruptions
- 🛡️ **Security First**: JWT tokens signed with enterprise-grade KMS
- 📈 **Scalable**: Each microservice validates independently
- 🎯 **Stateless**: No server-side session storage required
- ⚖️ **Load Balanced**: Google Cloud Load Balancer ensures high availability and distributes traffic
- 🚀 **High Availability**: Multiple service instances behind load balancer for fault tolerance

## How to Use

1. Copy any Mermaid code block above
2. Paste into:
   - GitHub/GitLab markdown files
   - [Mermaid Live Editor](https://mermaid.live)
   - Documentation tools (Notion, Obsidian, etc.)
   - VS Code with Mermaid extension
3. The diagrams will render automatically
4. Export to PNG/SVG if needed for presentations

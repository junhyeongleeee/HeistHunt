# Room Creation Flow Diagram

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         App.kt                              │
│  - Creates roomViewModel instance                           │
│  - Passes to OperationScreen                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   OperationScreen                           │
│  - Observes roomDetailState                                │
│  - Handles view switching (MAIN/QR_SCANNER/WAITING)       │
│  - LaunchedEffect for auto-navigation                     │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
┌──────────────┬──────────────┬──────────────────┐
│MainAction    │QRScanner     │RoomWaiting       │
│Screen        │Screen        │Screen            │
│              │              │                  │
│-Create button│-Camera UI    │-QRCodeDisplay    │
│-Loading state│-Scan logic   │-Participants     │
│-Error dialog │              │-Start button     │
└──────────────┴──────────────┴──────────────────┘
                                      │
                                      ▼
                            ┌─────────────────────┐
                            │  QRCodeDisplay      │
                            │  Component          │
                            │                     │
                            │  - QRose library    │
                            │  - Error fallback   │
                            └─────────────────────┘
```

## State Flow

```
┌────────────────┐
│  RoomViewModel │
│                │
│  States:       │
│  - isLoading   │◄─────── User clicks "작전 설계하기"
│  - room        │
│  - error       │         RoomViewModel.createRoom()
│  - shouldNav   │                    │
└────────────────┘                    │
        │                             ▼
        │                    ┌──────────────────┐
        │                    │ RoomRepository   │
        │                    │                  │
        │                    │ ApiClient        │
        │                    └────────┬─────────┘
        │                             │
        │                             ▼
        │                    POST /api/rooms
        │                    {
        │                      "name": "...",
        │                      "settings": {...}
        │                    }
        │                             │
        │                             ▼
        │                    ┌──────────────────┐
        │                    │  Server          │
        │                    │                  │
        │                    │  - Generates code│
        │                    │  - Creates room  │
        │                    │  - Returns Room  │
        │                    └────────┬─────────┘
        │                             │
        │                             ▼
        │                    Response: Room {
        │                      id, code, name,
        │                      participants: [...]
        │                    }
        │                             │
        │◄────────────────────────────┘
        │
        │ Update state:
        │ - isLoading = false
        │ - room = Room(...)
        │ - shouldNavigateToWaiting = true
        │
        ▼
┌────────────────┐
│  OperationUI   │
│                │
│  LaunchedEffect observes shouldNav
│  ↓
│  currentView = WAITING
│  ↓
│  roomViewModel.resetNavigationState()
│  ↓
│  RoomWaitingScreen shows:
│  - QR code (room.code)
│  - Participant list
└────────────────┘
```

## Data Flow

```
User Action
    │
    ▼
┌─────────────────────┐
│   UI Layer          │
│                     │
│ - OperationScreen   │
│ - MainActionScreen  │
│ - RoomWaitingScreen │
└──────────┬──────────┘
           │ collectAsState()
           │
           ▼
┌─────────────────────┐
│  ViewModel Layer    │
│                     │
│ - RoomViewModel     │
│ - StateFlow<State>  │
└──────────┬──────────┘
           │ createRoom()
           │
           ▼
┌─────────────────────┐
│ Repository Layer    │
│                     │
│ - RoomRepository    │
└──────────┬──────────┘
           │ suspend fun
           │
           ▼
┌─────────────────────┐
│  Network Layer      │
│                     │
│ - ApiClient         │
│ - Ktor HttpClient   │
└──────────┬──────────┘
           │ HTTP
           │
           ▼
┌─────────────────────┐
│   Server            │
│                     │
│ - Ktor Server       │
│ - PostgreSQL        │
└─────────────────────┘
```

## QR Code Generation

```
room.code (e.g., "ABCD1234")
           │
           ▼
┌──────────────────────────┐
│  QRCodeDisplay           │
│                          │
│  rememberQrCodePainter() │
│         │                │
│         ▼                │
│  ┌────────────────┐      │
│  │ QRose Library  │      │
│  │                │      │
│  │ - Encode data  │      │
│  │ - Apply styles │      │
│  │ - Generate img │      │
│  └───────┬────────┘      │
│          │               │
│          ▼               │
│   QR Code Painter        │
│          │               │
│          ▼               │
│    Image() renders       │
└──────────────────────────┘
```

## Error Handling Flow

```
Network Error
    │
    ▼
┌─────────────────────────┐
│  Repository catches     │
│  Result.failure()       │
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐
│  ViewModel updates      │
│  error = exception.msg  │
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐
│  UI shows AlertDialog   │
│                         │
│  "OK" button calls      │
│  clearError()           │
└─────────────────────────┘
           │
           ▼
┌─────────────────────────┐
│  User can retry         │
└─────────────────────────┘
```

## Component Responsibilities

### OperationScreen
- **Owns**: Current view state (MAIN/QR_SCANNER/WAITING)
- **Observes**: roomDetailState from ViewModel
- **Decides**: When to switch views
- **Handles**: Navigation between sub-screens

### MainActionScreen
- **Displays**: Action buttons
- **Shows**: Loading state during room creation
- **Handles**: Error display (AlertDialog)
- **Triggers**: Room creation action

### RoomWaitingScreen
- **Displays**: Room details
- **Shows**: QR code, participants, settings
- **Handles**: Start game action (future)
- **Provides**: Back navigation

### QRCodeDisplay
- **Generates**: QR code from string
- **Styles**: Custom colors and shapes
- **Handles**: Generation errors
- **Fallback**: Text display if QR fails

### RoomViewModel
- **Manages**: Room state
- **Calls**: Repository methods
- **Updates**: UI state via StateFlow
- **Provides**: Actions (create, join, leave)

### RoomRepository
- **Abstracts**: Network calls
- **Returns**: Result<T> for error handling
- **Transforms**: DTOs to models
- **Manages**: API communication

export type ScanVerdict = 'ALLOWED' | 'RESTRICTED' | 'AMBIGUOUS';

export interface DetectedSign {
  id: string;
  title: string;
  subtitle?: string;
  applicableDaysHours?: string;
  restrictions?: string;
  exceptions?: string;
  ruleText?: string;
  isRestrictingNow?: boolean;
  isUncertain?: boolean;
  statusBadge?: string;
  rawText?: string;
  croppedImageUri?: string | null;
  confidence?: number;
  isDemo?: boolean;
}

export interface SignBoundingBox {
  id: string;
  left: number;
  top: number;
  right: number;
  bottom: number;
  label: string;
  ocrText?: string;
  confidence?: number;
  sourceWidth?: number;
  sourceHeight?: number;
}

export interface ScanResult {
  id: number;
  timestamp: number;
  locationName: string;
  cityState: string;
  verdict: ScanVerdict;
  statusChipText: string;
  allowedUntilTime: string;
  timeRemaining: string;
  parkingRules: string[];
  explanation: string;
  detectedSigns: DetectedSign[];
  zoneType: string;
  paymentInfo: string;
  vehicleApplicability: string;
  imageUri?: string | null;
  isDemo?: boolean;
}

export interface ActiveParkingSession {
  id: number;
  scanResultId: number;
  locationName: string;
  startTime: number;
  endTime: number;
  allowedUntilTime: string;
  reminderMinutesBefore: number;
  notes: string;
  timerBasis: string;
  parkingRuleSummary: string;
  isActive: boolean;
  maxAllowedEndTimeMillis?: number | null;
  timerMode: string;
  isDemo?: boolean;
}

export interface ParkingSpot {
  id: number;
  latitude: number;
  longitude: number;
  timestamp: number;
  accuracy?: number | null;
  locationName: string;
  sessionId?: number | null;
  isActive: boolean;
  isDemo?: boolean;
}

export interface SavedPlace {
  id: number;
  name: string;
  address: string;
  parkingNote?: string;
  timestamp: number;
  latitude?: number | null;
  longitude?: number | null;
  scanResultId?: number | null;
  parkingRuleSummary?: string;
  parkingSchedule?: string;
  parkingVerdict?: string;
  signImageUri?: string;
  lastCheckedAt?: number;
  reminderEnabled?: boolean;
  reminderMinutesBefore?: number;
  reminderScheduleText?: string;
}

export interface UserProfile {
  name: string;
  gender: string;
  email: string;
  isPro: boolean;
  pushNotificationsEnabled: boolean;
}

export interface ChatMessage {
  id: string;
  text: string;
  isUser: boolean;
  timestamp: number;
}

export interface SampleSignPreset {
  id: string;
  title: string;
  previewDescription: string;
  simulatedVerdict: ScanVerdict;
  locationName: string;
  cityState?: string;
  allowedUntil: string;
  timeRemaining?: string;
  rules: string[];
  explanation: string;
  detectedSigns: DetectedSign[];
  zoneType?: string;
  paymentInfo?: string;
  vehicleApplicability?: string;
}

export interface CurbNote {
  id: number;
  targetType: 'SAVED_PLACE' | 'SCAN_RESULT';
  targetId: number;
  text: string;
  createdAt: number;
  updatedAt: number;
}

export interface InAppNotification {
  id: string;
  title: string;
  body: string;
  timestamp: number;
  isRead: boolean;
  sessionId?: number;
}

export interface ScanUsageInfo {
  scansUsedThisMonth: number;
  maxFreeScans: number;
}

export interface ChatUsageInfo {
  messagesUsedToday: number;
  maxFreeMessages: number;
}

export type AppRoute =
  | 'splash'
  | 'welcome'
  | 'name_setup'
  | 'permissions'
  | 'home'
  | 'scan'
  | 'scan_output'
  | 'parking_details'
  | 'contextual_copilot'
  | 'ask_curb'
  | 'activity'
  | 'you'
  | 'account_info'
  | 'notifications'
  | 'notification_settings'
  | 'payment_subscription'
  | 'curb_pro_paywall'
  | 'help_support'
  | 'privacy_policy'
  | 'terms_of_service'
  | 'about_curb'
  | 'saved_places'
  | 'parking_timer'
  | 'find_my_car';

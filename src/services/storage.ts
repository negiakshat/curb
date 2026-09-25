import {
  UserProfile,
  ActiveParkingSession,
  ParkingSpot,
  ScanResult,
  SavedPlace,
  CurbNote,
  InAppNotification,
  ScanUsageInfo,
  ChatUsageInfo
} from '../types';

const STORAGE_KEYS = {
  USER_PROFILE: 'curb_user_profile',
  ACTIVE_SESSION: 'curb_active_session',
  SAVED_PARKING_SPOT: 'curb_saved_parking_spot',
  SCAN_HISTORY: 'curb_scan_history',
  SAVED_PLACES: 'curb_saved_places',
  NOTES: 'curb_notes',
  NOTIFICATIONS: 'curb_notifications',
  SCAN_USAGE: 'curb_scan_usage',
  CHAT_USAGE: 'curb_chat_usage',
  ONBOARDING_COMPLETED: 'curb_onboarding_completed'
};

const DEFAULT_PROFILE: UserProfile = {
  name: 'Alex',
  gender: 'Not specified',
  email: 'alex.driver@example.com',
  isPro: false,
  pushNotificationsEnabled: true
};

const DEFAULT_NOTIFICATIONS: InAppNotification[] = [
  {
    id: 'notif_welcome',
    title: 'Welcome to Curb!',
    body: 'Point your camera at any parking sign to get an instant, clear parking verdict.',
    timestamp: Date.now() - 3600000,
    isRead: false
  },
  {
    id: 'notif_timer_tip',
    title: 'Smart Parking Timers',
    body: 'When parking is allowed, tap Start Parking Timer to count down before street sweepers or tow hours.',
    timestamp: Date.now() - 7200000,
    isRead: true
  }
];

export const storage = {
  getUserProfile(): UserProfile {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.USER_PROFILE);
      return data ? JSON.parse(data) : DEFAULT_PROFILE;
    } catch {
      return DEFAULT_PROFILE;
    }
  },

  setUserProfile(profile: UserProfile): void {
    localStorage.setItem(STORAGE_KEYS.USER_PROFILE, JSON.stringify(profile));
  },

  getActiveSession(): ActiveParkingSession | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION);
      if (!data) return null;
      const session: ActiveParkingSession = JSON.parse(data);
      return session.isActive ? session : null;
    } catch {
      return null;
    }
  },

  setActiveSession(session: ActiveParkingSession | null): void {
    if (session) {
      localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION);
    }
  },

  getSavedParkingSpot(): ParkingSpot | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SAVED_PARKING_SPOT);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  setSavedParkingSpot(spot: ParkingSpot | null): void {
    if (spot) {
      localStorage.setItem(STORAGE_KEYS.SAVED_PARKING_SPOT, JSON.stringify(spot));
    } else {
      localStorage.removeItem(STORAGE_KEYS.SAVED_PARKING_SPOT);
    }
  },

  getScanHistory(): ScanResult[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SCAN_HISTORY);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  addScanResult(scan: ScanResult): void {
    try {
      const history = this.getScanHistory();
      const filtered = history.filter(s => s.id !== scan.id);
      filtered.unshift(scan);
      // Keep up to 50 recent scans
      localStorage.setItem(STORAGE_KEYS.SCAN_HISTORY, JSON.stringify(filtered.slice(0, 50)));
    } catch (e) {
      console.error(e);
    }
  },

  getSavedPlaces(): SavedPlace[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SAVED_PLACES);
      if (data) return JSON.parse(data);
      // Seed initial sample saved spot
      const initial: SavedPlace[] = [
        {
          id: 1,
          name: 'Financial District Office',
          address: '555 California St, San Francisco, CA',
          parkingNote: '2h meter until 6pm. Free after 6pm and all weekend.',
          timestamp: Date.now() - 86400000 * 2,
          latitude: 37.7925,
          longitude: -122.4044,
          parkingRuleSummary: '2 Hour Parking 8 AM to 6 PM',
          parkingVerdict: 'ALLOWED',
          reminderEnabled: true,
          reminderMinutesBefore: 15
        }
      ];
      this.setSavedPlaces(initial);
      return initial;
    } catch {
      return [];
    }
  },

  setSavedPlaces(places: SavedPlace[]): void {
    localStorage.setItem(STORAGE_KEYS.SAVED_PLACES, JSON.stringify(places));
  },

  saveSavedPlace(place: SavedPlace): SavedPlace {
    const places = this.getSavedPlaces();
    const existingIndex = places.findIndex(p => p.id === place.id);
    if (existingIndex >= 0) {
      places[existingIndex] = place;
    } else {
      const newPlace = { ...place, id: place.id || Date.now() };
      places.unshift(newPlace);
      this.setSavedPlaces(places);
      return newPlace;
    }
    this.setSavedPlaces(places);
    return place;
  },

  deleteSavedPlace(id: number): void {
    const places = this.getSavedPlaces().filter(p => p.id !== id);
    this.setSavedPlaces(places);
  },

  getNotes(): CurbNote[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTES);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  saveNote(targetType: 'SAVED_PLACE' | 'SCAN_RESULT', targetId: number, text: string): CurbNote {
    const notes = this.getNotes();
    const existingIdx = notes.findIndex(n => n.targetType === targetType && n.targetId === targetId);
    let updatedNote: CurbNote;

    if (existingIdx >= 0) {
      updatedNote = {
        ...notes[existingIdx],
        text,
        updatedAt: Date.now()
      };
      notes[existingIdx] = updatedNote;
    } else {
      updatedNote = {
        id: Date.now(),
        targetType,
        targetId,
        text,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };
      notes.unshift(updatedNote);
    }
    localStorage.setItem(STORAGE_KEYS.NOTES, JSON.stringify(notes));
    return updatedNote;
  },

  deleteNote(targetType: 'SAVED_PLACE' | 'SCAN_RESULT', targetId: number): void {
    const notes = this.getNotes().filter(n => !(n.targetType === targetType && n.targetId === targetId));
    localStorage.setItem(STORAGE_KEYS.NOTES, JSON.stringify(notes));
  },

  getNotifications(): InAppNotification[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      if (data) return JSON.parse(data);
      this.setNotifications(DEFAULT_NOTIFICATIONS);
      return DEFAULT_NOTIFICATIONS;
    } catch {
      return DEFAULT_NOTIFICATIONS;
    }
  },

  setNotifications(notifications: InAppNotification[]): void {
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(notifications));
  },

  addNotification(title: string, body: string, sessionId?: number): InAppNotification {
    const notifications = this.getNotifications();
    const newNotif: InAppNotification = {
      id: `notif_${Date.now()}`,
      title,
      body,
      timestamp: Date.now(),
      isRead: false,
      sessionId
    };
    notifications.unshift(newNotif);
    this.setNotifications(notifications.slice(0, 30));
    return newNotif;
  },

  markNotificationsAsRead(): void {
    const updated = this.getNotifications().map(n => ({ ...n, isRead: true }));
    this.setNotifications(updated);
  },

  getScanUsage(): ScanUsageInfo {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SCAN_USAGE);
      const now = new Date();
      const currentMonthKey = `${now.getFullYear()}_${now.getMonth()}`;
      if (data) {
        const parsed = JSON.parse(data);
        if (parsed.monthKey === currentMonthKey) {
          return { scansUsedThisMonth: parsed.used || 0, maxFreeScans: 10 };
        }
      }
      return { scansUsedThisMonth: 0, maxFreeScans: 10 };
    } catch {
      return { scansUsedThisMonth: 0, maxFreeScans: 10 };
    }
  },

  incrementScanUsage(): void {
    const now = new Date();
    const currentMonthKey = `${now.getFullYear()}_${now.getMonth()}`;
    const usage = this.getScanUsage();
    localStorage.setItem(STORAGE_KEYS.SCAN_USAGE, JSON.stringify({
      monthKey: currentMonthKey,
      used: usage.scansUsedThisMonth + 1
    }));
  },

  getChatUsage(): ChatUsageInfo {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CHAT_USAGE);
      const todayKey = new Date().toISOString().slice(0, 10);
      if (data) {
        const parsed = JSON.parse(data);
        if (parsed.dateKey === todayKey) {
          return { messagesUsedToday: parsed.used || 0, maxFreeMessages: 15 };
        }
      }
      return { messagesUsedToday: 0, maxFreeMessages: 15 };
    } catch {
      return { messagesUsedToday: 0, maxFreeMessages: 15 };
    }
  },

  incrementChatUsage(): void {
    const todayKey = new Date().toISOString().slice(0, 10);
    const usage = this.getChatUsage();
    localStorage.setItem(STORAGE_KEYS.CHAT_USAGE, JSON.stringify({
      dateKey: todayKey,
      used: usage.messagesUsedToday + 1
    }));
  },

  isOnboardingCompleted(): boolean {
    return localStorage.getItem(STORAGE_KEYS.ONBOARDING_COMPLETED) === 'true';
  },

  setOnboardingCompleted(val: boolean): void {
    localStorage.setItem(STORAGE_KEYS.ONBOARDING_COMPLETED, String(val));
  },

  clearAllData(): void {
    localStorage.clear();
  }
};

export interface UserLocation {
  latitude: number;
  longitude: number;
  accuracy: number;
  locationName: string;
  cityState: string;
}

export function calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3; // Earth radius in meters
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return Math.round(R * c);
}

export function formatDistance(meters: number): string {
  if (meters < 1000) {
    const feet = Math.round(meters * 3.28084);
    return `${feet} ft (${meters} m)`;
  }
  const miles = (meters / 1609.34).toFixed(1);
  return `${miles} miles (${(meters / 1000).toFixed(1)} km)`;
}

export function estimateWalkingMinutes(meters: number): number {
  // Average human walking speed: ~80 meters per minute
  return Math.max(1, Math.round(meters / 80));
}

export async function getCurrentUserLocation(): Promise<UserLocation> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      // Default downtown location fallback
      resolve({
        latitude: 37.7925,
        longitude: -122.4044,
        accuracy: 12,
        locationName: 'Montgomery & Pine St',
        cityState: 'San Francisco, CA'
      });
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        const accuracy = Math.round(pos.coords.accuracy || 15);

        try {
          // Attempt reverse geocoding via Nominatim
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&zoom=18&addressdetails=1`,
            { headers: { 'User-Agent': 'CurbParkingApp/1.0' } }
          );
          if (res.ok) {
            const data = await res.json();
            const road = data.address?.road || data.address?.suburb || 'Parked Spot';
            const city = data.address?.city || data.address?.town || 'San Francisco';
            const state = data.address?.state_code || data.address?.state || 'CA';
            resolve({
              latitude: lat,
              longitude: lng,
              accuracy,
              locationName: road,
              cityState: `${city}, ${state}`
            });
            return;
          }
        } catch {
          // fallback
        }

        resolve({
          latitude: lat,
          longitude: lng,
          accuracy,
          locationName: `${lat.toFixed(4)}, ${lng.toFixed(4)}`,
          cityState: 'Current Location'
        });
      },
      () => {
        // Permission denied or error fallback
        resolve({
          latitude: 37.7925,
          longitude: -122.4044,
          accuracy: 15,
          locationName: 'Montgomery St & California',
          cityState: 'San Francisco, CA'
        });
      },
      { enableHighAccuracy: true, timeout: 6000 }
    );
  });
}

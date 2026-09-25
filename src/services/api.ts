import { ScanResult, SampleSignPreset, ChatMessage } from '../types';

export const api = {
  async getPresets(): Promise<SampleSignPreset[]> {
    try {
      const res = await fetch('/api/presets');
      if (!res.ok) throw new Error('Failed to fetch presets');
      const data = await res.json();
      return data.presets || [];
    } catch (err) {
      console.error(err);
      return [];
    }
  },

  async scanSign(options: {
    imageBase64?: string;
    presetId?: string;
    locationName?: string;
    cityState?: string;
  }): Promise<ScanResult> {
    const res = await fetch('/api/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(options)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: 'Scan request failed' }));
      throw new Error(err.error || 'Failed to process parking sign');
    }

    return await res.json();
  },

  async sendChatMessage(
    query: string,
    history: ChatMessage[],
    scanContext?: ScanResult | null
  ): Promise<string> {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query,
        history,
        scanContext
      })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: 'Chat failed' }));
      throw new Error(err.error || 'Failed to get answer');
    }

    const data = await res.json();
    return data.reply;
  }
};

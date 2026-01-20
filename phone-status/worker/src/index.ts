export interface Env {
  STATUS_KV: KVNamespace;
  API_KEY: string;
}

interface PhoneStatus {
  app: string;
  appName?: string;
  battery: number;
  charging: boolean;
  batteryTemp?: number;
  wifi: boolean;
  wifiName?: string;
  bluetooth: boolean;
  screen: boolean;
  networkType?: string;
  location?: string;
  device?: string;
  music?: string;
  musicArtist?: string;
  volume?: number;
  brightness?: number;
  storage?: string;
  memory?: string;
  steps?: number;
  screenTime?: number;
  gameTime?: number;
  mood?: string;
  isLateNight?: boolean;
  statusMessage?: string;
  topApps?: Array<{name: string; minutes: number}>;
  timestamp: number;
}

function toBool(val: unknown): boolean {
  if (typeof val === 'boolean') return val;
  if (typeof val === 'string') return val.toLowerCase() === 'true' || val === '1';
  if (typeof val === 'number') return val === 1;
  return false;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const headers = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      'Content-Type': 'application/json',
    };
    
    if (request.method === 'OPTIONS') return new Response(null, { headers });

    const url = new URL(request.url);

    if (url.pathname === '/test') {
      return new Response(JSON.stringify({ version: 'v7-fun', time: Date.now() }), { headers });
    }

    if (request.method === 'POST' && url.pathname === '/update') {
      try {
        const authHeader = request.headers.get('Authorization');
        if (authHeader !== `Bearer ${env.API_KEY}`) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers });
        }

        const text = await request.text();
        if (!text) return new Response(JSON.stringify({ error: 'Empty body' }), { status: 400, headers });

        let data: any;
        try { data = JSON.parse(text); } 
        catch (e) { return new Response(JSON.stringify({ error: 'JSON parse failed' }), { status: 400, headers }); }

        let location = data.location;
        if (!location && data.lat && data.lon) {
          try {
            const geoRes = await fetch(
              `https://nominatim.openstreetmap.org/reverse?lat=${data.lat}&lon=${data.lon}&format=json&accept-language=zh`,
              { headers: { 'User-Agent': 'PhoneStatus/1.0' } }
            );
            if (geoRes.ok) {
              const geo: any = await geoRes.json();
              if (geo.address) {
                // 优先取市级：city > state (省会) > county
                const city = geo.address.city || geo.address.state || geo.address.county || '';
                // 确保是"市"结尾，如果是县则尝试取上级
                if (city && !city.endsWith('县')) {
                  location = city;
                } else {
                  // 县级的话，尝试从 display_name 提取市
                  const display = geo.display_name || '';
                  const match = display.match(/([^,]+市)/);
                  location = match ? match[1] : city;
                }
              }
            }
          } catch (e) {}
        }

        const status: PhoneStatus = {
          app: String(data.app || 'unknown'),
          appName: data.appName ? String(data.appName) : undefined,
          battery: Number(data.battery) || 0,
          charging: toBool(data.charging),
          batteryTemp: data.batteryTemp ? Number(data.batteryTemp) : undefined,
          wifi: toBool(data.wifi),
          wifiName: data.wifiName ? String(data.wifiName) : undefined,
          bluetooth: toBool(data.bluetooth),
          screen: toBool(data.screen),
          networkType: data.networkType ? String(data.networkType) : undefined,
          location: location ? String(location) : undefined,
          device: data.device ? String(data.device) : undefined,
          music: data.music ? String(data.music) : undefined,
          musicArtist: data.musicArtist ? String(data.musicArtist) : undefined,
          volume: typeof data.volume === 'number' ? data.volume : undefined,
          brightness: typeof data.brightness === 'number' ? data.brightness : undefined,
          storage: data.storage ? String(data.storage) : undefined,
          memory: data.memory ? String(data.memory) : undefined,
          steps: typeof data.steps === 'number' ? data.steps : undefined,
          screenTime: typeof data.screenTime === 'number' ? data.screenTime : undefined,
          gameTime: typeof data.gameTime === 'number' ? data.gameTime : undefined,
          mood: data.mood ? String(data.mood) : undefined,
          isLateNight: toBool(data.isLateNight),
          statusMessage: data.statusMessage ? String(data.statusMessage) : undefined,
          topApps: Array.isArray(data.topApps) ? data.topApps : undefined,
          timestamp: Date.now(),
        };

        await env.STATUS_KV.put('current_status', JSON.stringify(status));
        return new Response(JSON.stringify({ success: true }), { headers });
      } catch (e: any) {
        return new Response(JSON.stringify({ error: 'Server error', detail: e.message }), { status: 500, headers });
      }
    }

    if (url.pathname === '/status') {
      const status = await env.STATUS_KV.get('current_status');
      if (!status) return new Response(JSON.stringify({ error: 'No status yet' }), { status: 404, headers });
      return new Response(status, { headers });
    }

    if (url.pathname === '/visit') {
      const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
      const ua = request.headers.get('User-Agent') || 'unknown';
      const city = (request as any).cf?.city || '';
      const now = Date.now();
      
      const visitor = { ip: ip.substring(0, 3) + '***', ua, city, lastVisit: now, visits: 1 };
      await env.STATUS_KV.put(`visitor:${ip}`, JSON.stringify(visitor), { expirationTtl: 604800 });

      const recentKey = 'recent_visitors';
      const recentData = await env.STATUS_KV.get(recentKey);
      let recentList: any[] = recentData ? JSON.parse(recentData) : [];
      const idx = recentList.findIndex((v: any) => v.ip === visitor.ip);
      if (idx >= 0) recentList[idx] = visitor;
      else recentList.unshift(visitor);
      recentList = recentList.slice(0, 50);
      await env.STATUS_KV.put(recentKey, JSON.stringify(recentList));

      const today = new Date().toISOString().split('T')[0];
      const todayKey = `daily:${today}`;
      const todaySet = await env.STATUS_KV.get(todayKey);
      let todayVisitors: string[] = todaySet ? JSON.parse(todaySet) : [];
      if (!todayVisitors.includes(ip)) {
        todayVisitors.push(ip);
        await env.STATUS_KV.put(todayKey, JSON.stringify(todayVisitors), { expirationTtl: 172800 });
      }

      const totalKey = 'total_visitors';
      const totalSet = await env.STATUS_KV.get(totalKey);
      let totalVisitors: string[] = totalSet ? JSON.parse(totalSet) : [];
      if (!totalVisitors.includes(ip)) {
        totalVisitors.push(ip);
        await env.STATUS_KV.put(totalKey, JSON.stringify(totalVisitors));
      }

      return new Response(JSON.stringify({ success: true, visitor }), { headers });
    }

    if (url.pathname === '/visitors') {
      const today = new Date().toISOString().split('T')[0];
      const recentData = await env.STATUS_KV.get('recent_visitors');
      const todayData = await env.STATUS_KV.get(`daily:${today}`);
      const totalData = await env.STATUS_KV.get('total_visitors');
      
      const recentList: any[] = recentData ? JSON.parse(recentData) : [];
      const todayVisitors: string[] = todayData ? JSON.parse(todayData) : [];
      const totalVisitors: string[] = totalData ? JSON.parse(totalData) : [];
      const now = Date.now();
      const onlineCount = recentList.filter((v: any) => now - v.lastVisit < 300000).length;

      return new Response(JSON.stringify({
        online: onlineCount,
        today: todayVisitors.length,
        total: totalVisitors.length,
        recentVisitors: recentList.slice(0, 20),
      }), { headers });
    }

    return new Response(JSON.stringify({ error: 'Not found' }), { status: 404, headers });
  },
};

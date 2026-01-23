import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';

const app = express();
const PORT = Number(process.env.PORT) || 9529;
const API_KEY = process.env.API_KEY || 'dwill-status-2026-x7k9m2p4q8r1';

// 数据存储路径（本地开发时使用相对路径，Docker 中通过环境变量覆盖）
const DATA_DIR = path.resolve(process.env.DATA_DIR || './data');
const STATUS_FILE = path.join(DATA_DIR, 'status.json');
const VISITORS_FILE = path.join(DATA_DIR, 'visitors.json');

// 请求去重：防止短时间内重复处理相同请求
let lastUpdateTime = 0;
const UPDATE_THROTTLE_MS = 5000; // 至少间隔 5 秒

// 确保数据目录存在
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// ==================== 类型定义 ====================

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
  topApps?: Array<{ name: string; minutes: number }>;
  timestamp: number;
}

interface Visitor {
  ip: string;
  ua: string;
  city: string;
  lastVisit: number;
  visits: number;
}

interface VisitorData {
  recent: Visitor[];
  today: { date: string; ips: string[] };
  total: string[];
}

// ==================== 数据存储工具 ====================

function readJSON<T>(filePath: string, defaultValue: T): T {
  try {
    if (fs.existsSync(filePath)) {
      const data = fs.readFileSync(filePath, 'utf-8');
      return JSON.parse(data);
    }
  } catch (e) {
    console.error(`Error reading ${filePath}:`, e);
  }
  return defaultValue;
}

function writeJSON<T>(filePath: string, data: T): void {
  try {
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
  } catch (e) {
    console.error(`Error writing ${filePath}:`, e);
  }
}

function toBool(val: unknown): boolean {
  if (typeof val === 'boolean') return val;
  if (typeof val === 'string') return val.toLowerCase() === 'true' || val === '1';
  if (typeof val === 'number') return val === 1;
  return false;
}

// ==================== 中间件 ====================

app.use(cors());
app.use(express.json());

// 请求日志
app.use((req: Request, _res: Response, next: NextFunction) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// ==================== API 路由 ====================

// 测试接口
app.get('/api/test', (_req: Request, res: Response) => {
  res.json({ version: 'v1-self-hosted', time: Date.now() });
});

// 上报手机状态
app.post('/api/update', async (req: Request, res: Response) => {
  try {
    // 验证 API Key
    const authHeader = req.headers.authorization;
    if (authHeader !== `Bearer ${API_KEY}`) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    // 请求节流：防止短时间内重复处理
    const now = Date.now();
    if (now - lastUpdateTime < UPDATE_THROTTLE_MS) {
      res.json({ ok: true, throttled: true });
      return;
    }
    lastUpdateTime = now;

    const data = req.body;
    if (!data || Object.keys(data).length === 0) {
      res.status(400).json({ error: 'Empty body' });
      return;
    }

    // 位置信息：手机端已完成地理编码，直接使用
    // 流程：手机端 GPS → Android Geocoder → 高德 API（手机端调用）
    const location = data.location || '';
    const locationSource = location ? '手机端解析' : '无位置数据';

    // 构建状态对象
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

    // 保存到文件
    writeJSON(STATUS_FILE, status);
    
    // 详细调试信息
    console.log(`\n========== 状态上报 ==========`);
    console.log(`App包名: ${status.app}`);
    console.log(`App名称: ${status.appName || '未设置'}`);
    console.log(`正在播放: ${status.music || '无'}`);
    console.log(`播放艺术家: ${status.musicArtist || '无'}`);
    console.log(`电量: ${status.battery}% 充电: ${status.charging}`);
    console.log(`位置: ${status.location || '未获取'} (${locationSource || '无位置数据'})`);
    console.log(`设备: ${status.device || '未知'}`);
    console.log(`================================\n`);

    res.json({ success: true });
  } catch (e: any) {
    console.error('Update error:', e);
    res.status(500).json({ error: 'Server error', detail: e.message });
  }
});

// 获取当前状态
app.get('/api/status', (_req: Request, res: Response) => {
  const status = readJSON<PhoneStatus | null>(STATUS_FILE, null);
  if (!status) {
    res.status(404).json({ error: 'No status yet' });
    return;
  }
  res.json(status);
});

// 记录访客
app.get('/api/visit', (req: Request, res: Response) => {
  const ip = (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim() 
    || req.socket.remoteAddress 
    || 'unknown';
  const ua = req.headers['user-agent'] || 'unknown';
  const city = (req.headers['cf-ipcity'] as string) || ''; // Cloudflare 头，可能为空
  const now = Date.now();

  // 读取现有数据
  const visitorData = readJSON<VisitorData>(VISITORS_FILE, {
    recent: [],
    today: { date: '', ips: [] },
    total: []
  });

  // 创建访客记录（隐藏部分 IP）
  const maskedIp = ip.substring(0, Math.min(ip.length, 6)) + '***';
  const visitor: Visitor = { ip: maskedIp, ua, city, lastVisit: now, visits: 1 };

  // 更新最近访客列表
  const idx = visitorData.recent.findIndex(v => v.ip === maskedIp);
  if (idx >= 0) {
    visitorData.recent[idx] = visitor;
  } else {
    visitorData.recent.unshift(visitor);
  }
  visitorData.recent = visitorData.recent.slice(0, 50);

  // 更新今日访客
  const today = new Date().toISOString().split('T')[0];
  if (visitorData.today.date !== today) {
    visitorData.today = { date: today, ips: [] };
  }
  if (!visitorData.today.ips.includes(ip)) {
    visitorData.today.ips.push(ip);
  }

  // 更新总访客
  if (!visitorData.total.includes(ip)) {
    visitorData.total.push(ip);
  }

  // 保存
  writeJSON(VISITORS_FILE, visitorData);

  res.json({ success: true, visitor });
});

// 获取访客统计
app.get('/api/visitors', (_req: Request, res: Response) => {
  const visitorData = readJSON<VisitorData>(VISITORS_FILE, {
    recent: [],
    today: { date: '', ips: [] },
    total: []
  });

  const today = new Date().toISOString().split('T')[0];
  const now = Date.now();

  // 在线人数（5分钟内活跃）
  const onlineCount = visitorData.recent.filter(v => now - v.lastVisit < 300000).length;

  // 今日访客数
  const todayCount = visitorData.today.date === today ? visitorData.today.ips.length : 0;

  res.json({
    online: onlineCount,
    today: todayCount,
    total: visitorData.total.length,
    recentVisitors: visitorData.recent.slice(0, 20),
  });
});

// ==================== 静态文件托管 ====================

// 托管前端文件（本地开发时指向 ../../frontend，Docker 中通过环境变量覆盖）
const FRONTEND_DIR = path.resolve(process.env.FRONTEND_DIR || '../../frontend');
app.use(express.static(FRONTEND_DIR));

// SPA 回退
app.get('*', (_req: Request, res: Response) => {
  res.sendFile(path.join(FRONTEND_DIR, 'index.html'));
});

// ==================== 启动服务器 ====================

// 监听 0.0.0.0 以接受来自局域网其他设备的连接
app.listen(PORT, '0.0.0.0', () => {
  console.log(`
╔═══════════════════════════════════════════════════════╗
║           📱 Phone Status Server Started              ║
╠═══════════════════════════════════════════════════════╣
║  Host:     0.0.0.0 (接受所有网络连接)                  ║
║  Port:     ${String(PORT).padEnd(42)}║
║  API Key:  ${API_KEY.substring(0, 10)}...${' '.repeat(30)}║
║  Data:     ${DATA_DIR.padEnd(42)}║
║  Frontend: ${FRONTEND_DIR.padEnd(42)}║
╚═══════════════════════════════════════════════════════╝
  `);
});

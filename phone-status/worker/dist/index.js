// src/index.ts
function getCorsHeaders(request) {
  const origin = request.headers.get("Origin") || "";
  const allowedOrigins = [
    "https://status.dwill.top",
    "http://127.0.0.1",
    "http://localhost",
    "null"
    // 本地文件
  ];
  const isAllowed = allowedOrigins.some((o) => origin.startsWith(o) || origin === "null");
  return {
    "Access-Control-Allow-Origin": isAllowed ? origin || "*" : "https://status.dwill.top",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization"
  };
}
var src_default = {
  async fetch(request, env) {
    const headers = getCorsHeaders(request);
    if (request.method === "OPTIONS") {
      return new Response(null, { headers });
    }
    const url = new URL(request.url);
    if (request.method === "POST" && url.pathname === "/update") {
      const authHeader = request.headers.get("Authorization");
      if (authHeader !== `Bearer ${env.API_KEY}`) {
        return new Response(JSON.stringify({ error: "Unauthorized" }), {
          status: 401,
          headers: { ...headers, "Content-Type": "application/json" }
        });
      }
      try {
        const data = await request.json();
        let location = data.location;
        if (!location && data.lat && data.lon) {
          try {
            const geoRes = await fetch(
              `https://nominatim.openstreetmap.org/reverse?lat=${data.lat}&lon=${data.lon}&format=json&accept-language=zh`,
              { headers: { "User-Agent": "PhoneStatus/1.0" } }
            );
            if (geoRes.ok) {
              const geo = await geoRes.json();
              const addr = geo.address;
              if (addr) {
                const city = addr.city || addr.town || addr.county || "";
                const district = addr.district || addr.suburb || "";
                location = district ? `${city}${district}` : city;
              }
            }
          } catch (e) {
          }
        }
        const status = {
          app: data.app || "unknown",
          appName: data.appName,
          battery: data.battery ?? 0,
          charging: data.charging ?? false,
          wifi: data.wifi ?? false,
          wifiName: data.wifiName,
          bluetooth: data.bluetooth ?? false,
          screen: data.screen ?? true,
          location,
          device: data.device,
          timestamp: Date.now()
        };
        await env.STATUS_KV.put("current_status", JSON.stringify(status));
        return new Response(JSON.stringify({ success: true }), {
          headers: { ...headers, "Content-Type": "application/json" }
        });
      } catch (e) {
        return new Response(JSON.stringify({ error: "Invalid JSON" }), {
          status: 400,
          headers: { ...headers, "Content-Type": "application/json" }
        });
      }
    }
    if (request.method === "GET" && url.pathname === "/status") {
      const status = await env.STATUS_KV.get("current_status");
      if (!status) {
        return new Response(JSON.stringify({ error: "No status yet" }), {
          status: 404,
          headers: { ...headers, "Content-Type": "application/json" }
        });
      }
      return new Response(status, {
        headers: { ...headers, "Content-Type": "application/json" }
      });
    }
    if (request.method === "GET" && url.pathname === "/visit") {
      const ip = request.headers.get("CF-Connecting-IP") || request.headers.get("X-Forwarded-For") || "unknown";
      const ua = request.headers.get("User-Agent") || "unknown";
      let country = request.headers.get("CF-IPCountry") || "";
      let city = request.headers.get("CF-City") || "";
      if (!city && ip !== "unknown") {
        try {
          const geoRes = await fetch(`http://ip-api.com/json/${ip}?fields=status,country,city&lang=zh-CN`);
          if (geoRes.ok) {
            const geo = await geoRes.json();
            if (geo.status === "success") {
              country = geo.country || country;
              city = geo.city || city;
            }
          }
        } catch (e) {
        }
      }
      const now = Date.now();
      const visitorKey = `visitor:${ip}`;
      const existingVisitor = await env.STATUS_KV.get(visitorKey);
      let visitor;
      if (existingVisitor) {
        visitor = JSON.parse(existingVisitor);
        visitor.lastVisit = now;
        visitor.visits += 1;
        visitor.ua = ua;
      } else {
        visitor = {
          ip: ip.substring(0, 3) + "***",
          // 隐藏部分 IP
          ua,
          country,
          city,
          firstVisit: now,
          lastVisit: now,
          visits: 1
        };
      }
      await env.STATUS_KV.put(visitorKey, JSON.stringify(visitor), { expirationTtl: 86400 * 7 });
      await env.STATUS_KV.put(`online:${ip}`, "1", { expirationTtl: 300 });
      const recentKey = "recent_visitors";
      const recentData = await env.STATUS_KV.get(recentKey);
      let recentList = recentData ? JSON.parse(recentData) : [];
      const existingIndex = recentList.findIndex((v) => v.ip === visitor.ip);
      if (existingIndex >= 0) {
        recentList[existingIndex] = visitor;
      } else {
        recentList.unshift(visitor);
      }
      recentList = recentList.slice(0, 50);
      await env.STATUS_KV.put(recentKey, JSON.stringify(recentList));
      const today = (/* @__PURE__ */ new Date()).toISOString().split("T")[0];
      const todayKey = `daily:${today}`;
      const todaySet = await env.STATUS_KV.get(todayKey);
      let todayVisitors = todaySet ? JSON.parse(todaySet) : [];
      if (!todayVisitors.includes(ip)) {
        todayVisitors.push(ip);
        await env.STATUS_KV.put(todayKey, JSON.stringify(todayVisitors), { expirationTtl: 86400 * 2 });
      }
      const totalKey = "total_visitors";
      const totalSet = await env.STATUS_KV.get(totalKey);
      let totalVisitors = totalSet ? JSON.parse(totalSet) : [];
      if (!totalVisitors.includes(ip)) {
        totalVisitors.push(ip);
        await env.STATUS_KV.put(totalKey, JSON.stringify(totalVisitors));
      }
      return new Response(JSON.stringify({ success: true, visitor }), {
        headers: { ...headers, "Content-Type": "application/json" }
      });
    }
    if (request.method === "GET" && url.pathname === "/visitors") {
      const today = (/* @__PURE__ */ new Date()).toISOString().split("T")[0];
      const recentData = await env.STATUS_KV.get("recent_visitors");
      const todayData = await env.STATUS_KV.get(`daily:${today}`);
      const totalData = await env.STATUS_KV.get("total_visitors");
      const recentList = recentData ? JSON.parse(recentData) : [];
      const todayVisitors = todayData ? JSON.parse(todayData) : [];
      const totalVisitors = totalData ? JSON.parse(totalData) : [];
      const now = Date.now();
      const onlineCount = recentList.filter((v) => now - v.lastVisit < 3e5).length;
      const stats = {
        online: onlineCount,
        today: todayVisitors.length,
        total: totalVisitors.length,
        recentVisitors: recentList.slice(0, 20)
      };
      return new Response(JSON.stringify(stats), {
        headers: { ...headers, "Content-Type": "application/json" }
      });
    }
    return new Response(JSON.stringify({ error: "Not found" }), {
      status: 404,
      headers: { ...headers, "Content-Type": "application/json" }
    });
  }
};
export {
  src_default as default
};

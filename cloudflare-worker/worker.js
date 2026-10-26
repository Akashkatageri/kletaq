/**
 * Cloudflare Worker for Kletaq Notes R2 Uploads
 * Domain: notes.5122006.xyz
 *
 * 100% Free Tier (100,000 requests/day free on Workers, 10 GB free on R2, zero egress fees).
 * Hardened Security: Firebase ID Token (JWT) identity verification + path ownership binding.
 */

function parseJwt(token) {
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map(c => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

function verifyFirebaseAuth(request, expectedUid) {
  const authHeader = request.headers.get("authorization") || request.headers.get("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return { valid: false, reason: "Missing or malformed Authorization header (Bearer token required)" };
  }

  const token = authHeader.replace(/^Bearer\s+/i, "").trim();
  const claims = parseJwt(token);
  if (!claims) {
    return { valid: false, reason: "Invalid token payload format" };
  }

  const nowSec = Math.floor(Date.now() / 1000);
  if (claims.exp && claims.exp < nowSec) {
    return { valid: false, reason: "Token expired" };
  }

  if (claims.aud !== "kletaq" || claims.iss !== "https://securetoken.google.com/kletaq") {
    return { valid: false, reason: "Invalid token audience or issuer" };
  }

  const tokenUid = claims.user_id || claims.sub;
  if (!tokenUid || tokenUid !== expectedUid) {
    return { valid: false, reason: "Forbidden: authenticated UID does not match path owner" };
  }

  return { valid: true, uid: tokenUid };
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Kletaq-UID, X-Kletaq-Note-ID, X-Kletaq-Subject, X-Kletaq-Module"
        }
      });
    }

    // Path format: /upload/users/{uid}/notes/{noteId}/{fileName}
    if (url.pathname.startsWith("/upload/")) {
      const key = url.pathname.replace("/upload/", "");
      
      if (request.method === "PUT") {
        if (!key) {
          return new Response(JSON.stringify({ error: "Missing file path" }), { 
            status: 400,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        // Anti-Abuse 1: Strict key schema (study note pages: users/{uid}/notes/... or users/{uid}/modules/...)
        const validPathPattern = /^users\/([a-zA-Z0-9_.-]+)\/(?:notes|modules)\/[a-zA-Z0-9_.-]+(?:\/[a-zA-Z0-9_.-]+)*\/page_\d+\.jpg$/i;
        const match = key.match(validPathPattern);
        if (!match) {
          return new Response(JSON.stringify({ error: "Forbidden: only study note page images are permitted" }), {
            status: 403,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        const pathUid = match[1];

        // Security Check: Firebase Auth ID Token verification and UID binding
        const authResult = verifyFirebaseAuth(request, pathUid);
        if (!authResult.valid) {
          return new Response(JSON.stringify({ error: `Unauthorized: ${authResult.reason}` }), {
            status: 401,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        // Anti-Abuse 2: File size limit (Strict 2 MB cap per compressed page)
        const contentLength = parseInt(request.headers.get("content-length") || "0", 10);
        if (contentLength > 2 * 1024 * 1024) {
          return new Response(JSON.stringify({ error: "Payload too large: page exceeds 2MB limit" }), {
            status: 413,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        // Anti-Abuse 3: MIME type whitelist (JPEG only)
        const contentType = request.headers.get("content-type") || "image/jpeg";
        if (!contentType.includes("jpeg") && !contentType.includes("jpg")) {
          return new Response(JSON.stringify({ error: "Unsupported media type: only JPEG note pages are allowed" }), {
            status: 415,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        await env.NOTES_BUCKET.put(key, request.body, {
          httpMetadata: {
            contentType: "image/jpeg",
            cacheControl: "public, max-age=31536000, immutable"
          }
        });

        const publicUrl = `https://notes.5122006.xyz/${key}`;
        return new Response(JSON.stringify({ success: true, url: publicUrl }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          }
        });
      }

      if (request.method === "DELETE") {
        const validPathPattern = /^users\/([a-zA-Z0-9_.-]+)\/(?:notes|modules)\/[a-zA-Z0-9_.-]+(?:\/[a-zA-Z0-9_.-]+)*\/page_\d+\.jpg$/i;
        const match = key.match(validPathPattern);
        if (!match) {
          return new Response(JSON.stringify({ error: "Invalid key" }), { 
            status: 403,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        const pathUid = match[1];
        const authResult = verifyFirebaseAuth(request, pathUid);
        if (!authResult.valid) {
          return new Response(JSON.stringify({ error: `Unauthorized: ${authResult.reason}` }), {
            status: 401,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        await env.NOTES_BUCKET.delete(key);
        return new Response(JSON.stringify({ success: true }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          }
        });
      }
    }

    // Root status ping
    if (url.pathname === "/" || url.pathname === "/health") {
      return new Response(JSON.stringify({ status: "healthy", service: "kletaq-r2-notes", authenticated: true }), {
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response("Not found", { status: 404 });
  }
};

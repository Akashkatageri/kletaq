/**
 * Cloudflare Worker for Kletaq Notes R2 Uploads
 * Domain: notes.5122006.xyz
 *
 * 100% Free Tier (100,000 requests/day free on Workers, 10 GB free on R2, zero egress fees).
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, X-Kletaq-UID, X-Kletaq-Note-ID"
        }
      });
    }

    // Path format: /upload/users/{uid}/notes/{noteId}/{fileName}
    if (url.pathname.startsWith("/upload/")) {
      const key = url.pathname.replace("/upload/", "");
      
      if (request.method === "PUT") {
        if (!key) {
          return new Response("Missing file path", { status: 400 });
        }

        const contentType = request.headers.get("content-type") || "image/jpeg";
        await env.NOTES_BUCKET.put(key, request.body, {
          httpMetadata: {
            contentType: contentType,
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
      return new Response(JSON.stringify({ status: "healthy", service: "kletaq-r2-notes" }), {
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response("Not found", { status: 404 });
  }
};

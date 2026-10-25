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
          "Access-Control-Allow-Headers": "Content-Type, X-Kletaq-UID, X-Kletaq-Note-ID, X-Kletaq-Subject, X-Kletaq-Module"
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

        // Anti-Abuse 1: Strict key schema (study note pages only: users/{uid}/notes/{noteId}/page_XXX.jpg)
        const validPathPattern = /^users\/[a-zA-Z0-9_.-]+\/notes\/[a-zA-Z0-9_.-]+\/page_\d+\.jpg$/i;
        if (!validPathPattern.test(key)) {
          return new Response(JSON.stringify({ error: "Forbidden: only study note page images are permitted" }), {
            status: 403,
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
        // Enforce same key restriction on delete
        const validPathPattern = /^users\/[a-zA-Z0-9_.-]+\/notes\/[a-zA-Z0-9_.-]+\/page_\d+\.jpg$/i;
        if (!validPathPattern.test(key)) {
          return new Response("Invalid key", { status: 403 });
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
      return new Response(JSON.stringify({ status: "healthy", service: "kletaq-r2-notes" }), {
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response("Not found", { status: 404 });
  }
};

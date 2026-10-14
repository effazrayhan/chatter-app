const blocked = [/^localhost$/i, /^127\./, /^10\./, /^192\.168\./, /^169\.254\./, /^172\.(1[6-9]|2\d|3[01])\./];

Deno.serve(async (request) => {
  if (request.method !== "POST") return new Response("method not allowed", { status: 405 });
  const { url } = await request.json() as { url?: string };
  if (!url) return Response.json({ error: "url required" }, { status: 400 });
  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return Response.json({ error: "invalid url" }, { status: 400 });
  }
  if (!/^https?:$/.test(parsed.protocol) || blocked.some((pattern) => pattern.test(parsed.hostname))) {
    return Response.json({ error: "blocked url" }, { status: 400 });
  }
  const response = await fetch(parsed, { signal: AbortSignal.timeout(3000), headers: { "user-agent": "PulseLinkPreview/1.0" } });
  if (!response.ok) return Response.json({ error: "fetch failed" }, { status: 502 });
  const html = (await response.text()).slice(0, 1_000_000);
  const value = (property: string) => html.match(new RegExp(`<meta[^>]+(?:property|name)=["']${property}["'][^>]+content=["']([^"']*)`, "i"))?.[1] ?? null;
  return Response.json({ url, title: value("og:title"), description: value("og:description"), image: value("og:image") });
});

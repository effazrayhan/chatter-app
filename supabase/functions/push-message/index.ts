const corsHeaders = { "Access-Control-Allow-Origin": "*" };

type WebhookRecord = {
  id: string;
  chat_id: string;
  sender_id: string;
  body: string | null;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const serviceAccount = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";

async function accessToken(): Promise<string> {
  const account = JSON.parse(serviceAccount) as { client_email: string; private_key: string };
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${account.client_email}:${account.private_key}`,
    }),
  });
  if (!response.ok) throw new Error("FCM token request failed");
  const json = await response.json() as { access_token: string };
  return json.access_token;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") return new Response("method not allowed", { status: 405 });
  const payload = await request.json() as { record: WebhookRecord };
  const record = payload.record;
  const headers = { apikey: serviceKey, Authorization: `Bearer ${serviceKey}` };
  const membersResponse = await fetch(`${supabaseUrl}/rest/v1/chat_members?chat_id=eq.${record.chat_id}&user_id=neq.${record.sender_id}&select=user_id`, { headers });
  const members = await membersResponse.json() as Array<{ user_id: string }>;
  const ids = members.map((member) => member.user_id).join(",");
  if (!ids) return new Response("ok", { headers: corsHeaders });
  const devicesResponse = await fetch(`${supabaseUrl}/rest/v1/devices?user_id=in.(${ids})&select=fcm_token`, { headers });
  const devices = await devicesResponse.json() as Array<{ fcm_token: string }>;
  if (!devices.length) return new Response("ok", { headers: corsHeaders });
  const token = await accessToken();
  const projectId = JSON.parse(serviceAccount).project_id as string;
  await Promise.all(devices.map((device) => fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ message: { token: device.fcm_token, data: { chatId: record.chat_id, messageId: record.id, preview: record.body?.slice(0, 120) ?? "" } } }),
  })));
  return new Response("ok", { headers: corsHeaders });
});

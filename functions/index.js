const { onRequest } = require("firebase-functions/v2/https");
const { defineString, defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// Definición de Parámetros y Secretos (Estándar 2024+)
const WHATSAPP_TOKEN = defineSecret("WHATSAPP_TOKEN");
const PHONE_NUMBER_ID = defineString("PHONE_NUMBER_ID");
const VERIFY_TOKEN = defineString("VERIFY_TOKEN");
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

/**
 * WhatsApp Webhook
 */
exports.webhook = onRequest({ secrets: ["WHATSAPP_TOKEN", "GEMINI_API_KEY"] }, async (req, res) => {
  // Webhook Verification (GET)
  if (req.method === "GET") {
    const mode = req.query["hub.mode"];
    const token = req.query["hub.verify_token"];
    const challenge = req.query["hub.challenge"];

    if (mode && token) {
      if (mode === "subscribe" && token === VERIFY_TOKEN.value()) {
        console.log("WEBHOOK_VERIFIED");
        return res.status(200).send(challenge);
      } else {
        return res.sendStatus(403);
      }
    }
  }

  // Handle Messages (POST)
  if (req.method === "POST") {
    const body = req.body;

    if (body.object === "whatsapp_business_account") {
      if (
        body.entry &&
        body.entry[0].changes &&
        body.entry[0].changes[0].value.messages &&
        body.entry[0].changes[0].value.messages[0]
      ) {
        const message = body.entry[0].changes[0].value.messages[0];
        const from = message.from;
        const msgBody = message.text ? message.text.body : "";

        if (msgBody) {
          await processMessage(from, msgBody);
        }
      }
      return res.sendStatus(200);
    } else {
      return res.sendStatus(404);
    }
  }

  return res.sendStatus(405);
});

/**
 * Procesa el mensaje con IA
 */
async function processMessage(from, userQuery) {
  try {
    // 1. Verificar si la IA está encendida
    const configDoc = await db.doc("global_config/whatsapp_ai").get();
    if (!configDoc.exists || !configDoc.data().enabled) {
      console.log("Asistente IA desactivado.");
      return;
    }

    // 2. Obtener contexto de Firestore
    const productsSnapshot = await db.collection("products").get();
    const inventorySnapshot = await db.collection("inventory").get();

    const stockMap = {};
    inventorySnapshot.forEach(doc => {
      const data = doc.data();
      if (!stockMap[data.productId]) stockMap[data.productId] = 0;
      stockMap[data.productId] += data.stock;
    });

    let context = "Catálogo de productos y existencias totales:\n";
    productsSnapshot.forEach(doc => {
      const data = doc.data();
      const stock = stockMap[doc.id] || 0;
      context += `- ${data.name}: Precio $${data.price3}, Stock: ${stock} ${data.unit === 'KG' ? 'kg' : 'piezas'}\n`;
    });

    // 3. Generar respuesta con Gemini
    const genAI = new GoogleGenerativeAI(GEMINI_API_KEY.value());
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
    const prompt = `
      Eres el asistente inteligente de la tienda "Plazita".
      Tu objetivo es ayudar a los clientes por WhatsApp con información de precios y disponibilidad.

      Reglas:
      - Responde siempre en español de forma amable y breve.
      - Usa el precio P3 (Público) proporcionado.
      - Si el stock es 0, di que no hay existencias.
      - Si no sabes la respuesta, ofrece que un humano lo atienda pronto.

      Contexto de la tienda:
      ${context}

      Mensaje del cliente: "${userQuery}"
      Asistente:
    `;

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    // 4. Enviar vía WhatsApp
    await sendWhatsAppMessage(from, text);

  } catch (error) {
    console.error("Error en proceso de mensaje:", error);
  }
}

async function sendWhatsAppMessage(to, text) {
  try {
    await axios({
      method: "POST",
      url: `https://graph.facebook.com/v17.0/${PHONE_NUMBER_ID.value()}/messages`,
      data: {
        messaging_product: "whatsapp",
        to: to,
        text: { body: text },
      },
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${WHATSAPP_TOKEN.value()}`,
      },
    });
  } catch (error) {
    console.error("Error enviando WhatsApp:", error.response ? error.response.data : error.message);
  }
}

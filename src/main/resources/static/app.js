const chatForm = document.getElementById("chatForm");
const conversation = document.getElementById("conversation");
const messageTemplate = document.getElementById("messageTemplate");
const statusBlock = document.getElementById("statusBlock");
const seedOrdersBtn = document.getElementById("seedOrdersBtn");
const ingestPoliciesBtn = document.getElementById("ingestPoliciesBtn");
const sendBtn = document.getElementById("sendBtn");
const questionInput = document.getElementById("question");

const BASE_HEADERS = {
  "Content-Type": "application/json"
};

function setStatus(text) {
  statusBlock.textContent = text;
}

function addMessage(role, text, type) {
  const fragment = messageTemplate.content.cloneNode(true);
  const article = fragment.querySelector(".message");
  const roleNode = fragment.querySelector(".role");
  const textNode = fragment.querySelector(".text");

  article.classList.add(type);
  roleNode.textContent = role;
  textNode.textContent = text;

  conversation.appendChild(fragment);
  conversation.scrollTop = conversation.scrollHeight;
}

function disableChat(disabled) {
  sendBtn.disabled = disabled;
}

async function postJson(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: BASE_HEADERS,
    body: body ? JSON.stringify(body) : undefined
  });

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const errorMessage = payload?.message || payload?.error || `HTTP ${response.status}`;
    throw new Error(errorMessage);
  }

  return payload;
}

chatForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const customerId = document.getElementById("customerId").value.trim();
  const orderId = document.getElementById("orderId").value.trim();
  const question = questionInput.value.trim();
  const confirmCancellation = document.getElementById("confirmCancellation").checked;

  if (!customerId || !orderId || !question) {
    setStatus("Please fill customer ID, order ID, and question.");
    return;
  }

  addMessage("You", question, "user");
  questionInput.value = "";
  disableChat(true);
  setStatus("Assistant is thinking...");

  try {
    const payload = {
      customerId,
      orderId,
      question,
      confirmCancellation
    };

    const result = await postJson("/api/support/query", payload);
    addMessage("Assistant", result.answer || "No answer returned.", "assistant");
    setStatus("Response received.");
  } catch (error) {
    addMessage("System", `Request failed: ${error.message}`, "system");
    setStatus("Request failed.");
  } finally {
    disableChat(false);
  }
});

seedOrdersBtn.addEventListener("click", async () => {
  seedOrdersBtn.disabled = true;
  setStatus("Seeding default orders...");

  try {
    const result = await postJson("/api/support/orders/seed");
    addMessage("System", `Seed complete: ${result.ordersSeeded ?? 0} orders processed.`, "system");
    setStatus("Orders seeded.");
  } catch (error) {
    addMessage("System", `Seed failed: ${error.message}`, "system");
    setStatus("Seed failed.");
  } finally {
    seedOrdersBtn.disabled = false;
  }
});

ingestPoliciesBtn.addEventListener("click", async () => {
  ingestPoliciesBtn.disabled = true;
  setStatus("Ingesting policy documents...");

  try {
    const result = await postJson("/api/support/policies/ingest?directory=./policies");
    addMessage(
      "System",
      `Policies ingested: ${result.filesIngested ?? 0} files, ${result.chunksIngested ?? 0} chunks.`,
      "system"
    );
    setStatus("Policies ingested.");
  } catch (error) {
    addMessage("System", `Policy ingestion failed: ${error.message}`, "system");
    setStatus("Policy ingestion failed.");
  } finally {
    ingestPoliciesBtn.disabled = false;
  }
});

document.querySelectorAll(".chip").forEach((button) => {
  button.addEventListener("click", () => {
    questionInput.value = button.dataset.question || "";
    questionInput.focus();
  });
});

addMessage(
  "System",
  "Welcome to Order Support Assistant. This workspace is designed for business users to quickly check order status, review policy guidance, and decide next best actions with confidence.",
  "system"
);

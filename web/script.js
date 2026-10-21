const form = document.querySelector("#notification-form");
const channelCards = [...document.querySelectorAll(".channel-card")];
const recipientInput = document.querySelector("#recipient");
const messageInput = document.querySelector("#message");
const characterCount = document.querySelector("#character-count");
const submitButton = document.querySelector("#submit-button");
const connectionStatus = document.querySelector("#connection-status");
const connectionLabel = connectionStatus.querySelector(".connection-label");
const result = document.querySelector("#result");

const channelNames = {
  EMAIL: "E-mail",
  SMS: "SMS",
  WHATSAPP: "WhatsApp",
};

let selectedChannel = "EMAIL";

function updateRecipientField() {
  if (selectedChannel === "EMAIL") {
    recipientInput.type = "email";
    recipientInput.inputMode = "email";
    recipientInput.autocomplete = "email";
    recipientInput.placeholder = "nome@exemplo.com";
    recipientInput.maxLength = 254;
    return;
  }

  recipientInput.type = "tel";
  recipientInput.inputMode = "tel";
  recipientInput.autocomplete = "tel";
  recipientInput.placeholder = "+55 (11) 99999-9999";
  recipientInput.maxLength = 30;
}

function setConnectionState(connected) {
  connectionStatus.classList.toggle("is-connected", connected);
  connectionStatus.classList.toggle("is-disconnected", !connected);
  connectionLabel.textContent = connected ? "API conectada" : "API indisponível";
}

async function checkApiHealth() {
  try {
    const response = await fetch("/api/health");
    const data = await response.json();
    setConnectionState(response.ok && data.sucesso === true);
  } catch {
    setConnectionState(false);
  }
}

function showResult({ success, status, message, channel, recipient }) {
  result.hidden = false;
  result.classList.toggle("is-success", success);
  result.classList.toggle("is-error", !success);
  document.querySelector("#result-icon").textContent = success ? "✓" : "!";
  document.querySelector("#result-title").textContent = success ? "Notificação processada" : "Não foi possível enviar";
  document.querySelector("#result-status").textContent = status;
  document.querySelector("#result-message").textContent = message;
  document.querySelector("#result-channel").textContent = channelNames[channel] || channel || "—";
  document.querySelector("#result-recipient").textContent = recipient || "—";
}

channelCards.forEach((card) => {
  card.addEventListener("click", () => {
    selectedChannel = card.dataset.channel;
    channelCards.forEach((item) => {
      const isSelected = item === card;
      item.classList.toggle("is-selected", isSelected);
      item.setAttribute("aria-pressed", String(isSelected));
    });
    updateRecipientField();
    result.hidden = true;
  });
});

messageInput.addEventListener("input", () => {
  characterCount.textContent = `${messageInput.value.length} / 1000`;
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  result.hidden = true;

  if (!form.reportValidity()) {
    return;
  }
  if (!recipientInput.value.trim() || !messageInput.value.trim()) {
    showResult({
      success: false,
      status: "Dados inválidos",
      message: "Preencha o destinatário e a mensagem antes de continuar.",
      channel: selectedChannel,
      recipient: recipientInput.value.trim(),
    });
    return;
  }

  const recipient = recipientInput.value.trim();
  const message = messageInput.value;
  submitButton.disabled = true;
  submitButton.classList.add("is-loading");
  submitButton.querySelector(".button-text").textContent = "Enviando...";

  try {
    const response = await fetch("/api/notificacoes", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        canal: selectedChannel,
        destinatario: recipient,
        mensagem: message,
      }),
    });
    const data = await response.json();
    const success = response.ok && data.sucesso === true;
    setConnectionState(true);
    showResult({
      success,
      status: success ? "Sucesso" : `Falha · HTTP ${response.status}`,
      message: data.mensagem || "A API retornou uma resposta sem detalhes.",
      channel: data.canal || selectedChannel,
      recipient: data.destinatario || recipient,
    });
  } catch {
    setConnectionState(false);
    showResult({
      success: false,
      status: "API indisponível",
      message: "Não foi possível obter uma resposta do servidor. Verifique a conexão e tente novamente.",
      channel: selectedChannel,
      recipient,
    });
  } finally {
    submitButton.disabled = false;
    submitButton.classList.remove("is-loading");
    submitButton.querySelector(".button-text").textContent = "Enviar notificação";
  }
});

updateRecipientField();
checkApiHealth();

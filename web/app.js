const SAMPLE = `public class Demo :> Base :>> Printable, Comparable {
  int count = 0;
  str label ?= null;

  public void main() {
    int x = 5..10;
    Complex c = $(1.0, -2.5);
    Frac f = [3|4];
    Date d = [2026|07|30];

    if (x > 0) {
      print "ready";
    } else {
      return;
    }

    for (int i = 0; i < 3; i++) {
      count += i;
    }
  }
}
`;

let mode = "parse";
let errorMarks = [];
let meEspresso = false;

const eggLines = {
  brew: "moves like X-presso · sleep deferred",
  tag: "say you can't sleep — lex me in your dreams",
  footer: "that's that me · X-presso",
  toasts: [
    "That's that me — X-presso.",
    "Sleep optional. Tokens mandatory.",
    "One more shot? Bold of you.",
  ],
  ok: [
    "Mounted that parse like it was nothing.",
    "Clean shot. They'll talk.",
    "That's that me · analysis done.",
  ],
};

const editor = CodeMirror.fromTextArea(document.getElementById("source"), {
  lineNumbers: true,
  mode: "text/x-java",
  indentUnit: 2,
  tabSize: 2,
  lineWrapping: true,
  viewportMargin: Infinity,
});
editor.setValue(SAMPLE);

const els = {
  status: document.getElementById("status"),
  statusText: document.getElementById("status-text"),
  run: document.getElementById("btn-run"),
  tokens: document.getElementById("tokens"),
  tokenCount: document.getElementById("token-count"),
  tokensEmpty: document.getElementById("tokens-empty"),
  tree: document.getElementById("tree"),
  treeEmpty: document.getElementById("tree-empty"),
  errors: document.getElementById("errors"),
  errorCount: document.getElementById("error-count"),
  errorsEmpty: document.getElementById("errors-empty"),
  panelTokens: document.getElementById("panel-tokens"),
  panelTree: document.getElementById("panel-tree"),
  panelErrors: document.getElementById("panel-errors"),
};

function setStatus(state, text) {
  if (meEspresso && state === "ok") {
    text = eggLines.ok[Math.floor(Math.random() * eggLines.ok.length)];
  }
  if (meEspresso && state === "busy") {
    text = "Mounting that shot…";
  }
  els.status.dataset.state = state;
  els.statusText.textContent = text;
}

function setMode(next) {
  mode = next;
  document.querySelectorAll(".seg").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.mode === next);
  });
}

function switchTab(name) {
  document.querySelectorAll(".tab").forEach((tab) => {
    const on = tab.dataset.tab === name;
    tab.classList.toggle("active", on);
    tab.setAttribute("aria-selected", on ? "true" : "false");
  });
  ["tokens", "tree", "errors"].forEach((id) => {
    const panel = document.getElementById(`panel-${id}`);
    const on = id === name;
    panel.classList.toggle("active", on);
    panel.hidden = !on;
  });
}

function clearErrorMarks() {
  errorMarks.forEach((m) => editor.removeLineClass(m, "background", "cm-error-line"));
  errorMarks = [];
}

function jumpTo(line, column) {
  const ln = Math.max(0, (line || 1) - 1);
  const ch = Math.max(0, (column || 1) - 1);
  editor.setCursor({ line: ln, ch });
  editor.focus();
  editor.scrollIntoView({ line: ln, ch }, 80);
}

async function run() {
  clearErrorMarks();
  els.run.disabled = true;
  setStatus("busy", "Pulling shot… grinding tokens");

  try {
    const res = await fetch("/api/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ source: editor.getValue(), mode }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      setStatus("err", err.error || "Shot failed — machine hiccup");
      renderErrors([{ kind: "server", message: err.error || "Request failed", line: 1, column: 1 }]);
      switchTab("errors");
      return;
    }

    render(await res.json());
  } catch (e) {
    setStatus("err", "Machine offline — is the server running?");
    renderErrors([{ kind: "server", message: String(e.message || e), line: 1, column: 1 }]);
    switchTab("errors");
  } finally {
    els.run.disabled = false;
  }
}

function render(data) {
  const tokens = (data.tokens || []).filter(
    (t) => t.type !== "WHITESPACE" && t.type !== "COMMENT" && t.type !== "EOF"
  );
  const errors = [...(data.lexicalErrors || []), ...(data.syntaxErrors || [])];

  renderTokens(tokens);
  renderTree(data.parseTree, mode === "lex");
  renderErrors(errors);

  if (errors.length) {
    const lex = (data.lexicalErrors || []).length;
    const syn = (data.syntaxErrors || []).length;
    const parts = [];
    if (lex) parts.push(`${lex} lexical`);
    if (syn) parts.push(`${syn} syntax`);
    setStatus("err", `Spilled shot — ${errors.length} issue${errors.length === 1 ? "" : "s"} (${parts.join(", ")})`);
    switchTab("errors");
  } else {
    setStatus(
      "ok",
      mode === "lex"
        ? `Ristretto clean · ${tokens.length} beans`
        : `Doppio perfect · ${tokens.length} beans, tree ready`
    );
    switchTab(mode === "lex" ? "tokens" : "tree");
  }
}

function renderTokens(tokens) {
  els.tokens.innerHTML = "";
  els.tokenCount.textContent = String(tokens.length);
  els.tokenCount.dataset.zero = tokens.length ? "false" : "true";
  els.panelTokens.classList.toggle("has-content", tokens.length > 0);
  els.tokensEmpty.hidden = tokens.length > 0;

  const frag = document.createDocumentFragment();
  tokens.forEach((t) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="type">${escapeHtml(t.type)}</td>
      <td class="lexeme" title="${escapeAttr(t.lexeme)}">${escapeHtml(displayLexeme(t.lexeme))}</td>
      <td class="pos">${t.line}</td>
      <td class="pos">${t.column}</td>`;
    tr.addEventListener("click", () => jumpTo(t.line, t.column));
    frag.appendChild(tr);
  });
  els.tokens.appendChild(frag);
}

function renderTree(node, lexOnly) {
  els.tree.innerHTML = "";
  if (lexOnly || !node) {
    els.panelTree.classList.remove("has-content");
    els.treeEmpty.hidden = false;
    els.treeEmpty.textContent = lexOnly
      ? "Switch to Doppio to brew a parse tree."
      : "No brew tree.";
    return;
  }
  els.panelTree.classList.add("has-content");
  els.treeEmpty.hidden = true;
  els.tree.appendChild(treeNode(node));
}

function treeNode(node) {
  const children = node.children || [];
  const label = `${escapeHtml(node.kind)}${
    node.value != null ? ` <span class="val">${escapeHtml(String(node.value))}</span>` : ""
  }`;

  if (!children.length) {
    const div = document.createElement("div");
    div.className = "leaf";
    div.innerHTML = label;
    return div;
  }

  const details = document.createElement("details");
  details.open = ["Program", "ClassDecl", "ClassBody", "MethodDecl", "Block"].includes(node.kind);
  const summary = document.createElement("summary");
  summary.innerHTML = label;
  details.appendChild(summary);
  children.forEach((c) => details.appendChild(treeNode(c)));
  return details;
}

function renderErrors(errors) {
  clearErrorMarks();
  els.errors.innerHTML = "";
  els.errorCount.textContent = String(errors.length);
  els.errorCount.dataset.zero = errors.length ? "false" : "true";
  els.errorCount.classList.toggle("warn", errors.length > 0);
  els.panelErrors.classList.toggle("has-content", errors.length > 0);
  els.errorsEmpty.hidden = errors.length > 0;

  errors.forEach((e) => {
    const line = e.line || 1;
    errorMarks.push(editor.addLineClass(Math.max(0, line - 1), "background", "cm-error-line"));

    const li = document.createElement("li");
    li.innerHTML = `
      <div class="meta">
        <span class="kind">${escapeHtml(e.kind || "error")}</span>
        <span>L${line}:C${e.column || 1}</span>
      </div>
      <div class="msg">${escapeHtml(e.message || "")}</div>
      ${e.suggestion ? `<div class="hint">${escapeHtml(e.suggestion)}</div>` : ""}`;
    li.addEventListener("click", () => jumpTo(line, e.column));
    els.errors.appendChild(li);
  });
}

function displayLexeme(s) {
  return String(s ?? "").replace(/\n/g, "\\n").replace(/\t/g, "\\t");
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function escapeAttr(s) {
  return escapeHtml(s).replace(/'/g, "&#39;");
}

document.querySelectorAll(".seg").forEach((btn) => {
  btn.addEventListener("click", () => setMode(btn.dataset.mode));
});

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => switchTab(tab.dataset.tab));
});

els.run.addEventListener("click", run);
document.getElementById("btn-sample").addEventListener("click", () => {
  editor.setValue(SAMPLE);
  run();
});

editor.on("keydown", (_cm, e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
    e.preventDefault();
    run();
  }
});

const isMac = /Mac|iPhone|iPad/.test(navigator.platform || "");
document.querySelector("#btn-run kbd").textContent = isMac ? "⌘↵" : "Ctrl+↵";

/* —— Easter egg: tap the cup 7× (Espresso-era pop homage, original lines) —— */
const CUP_TAPS_NEEDED = 7;
let cupTaps = 0;
let cupTimer = null;

const cup = document.getElementById("easter-cup");
const toastEl = document.getElementById("toast");
const sparkleLayer = document.getElementById("sparkle-layer");
const brewLine = document.getElementById("brew-line");
const brandTag = document.getElementById("brand-tag");
const footerLine = document.getElementById("footer-line");

function showToast(message) {
  toastEl.hidden = false;
  toastEl.textContent = message;
  toastEl.classList.add("show");
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => {
    toastEl.classList.remove("show");
    setTimeout(() => {
      toastEl.hidden = true;
    }, 280);
  }, 2800);
}

function burstSparkles(origin) {
  const rect = origin.getBoundingClientRect();
  const cx = rect.left + rect.width / 2;
  const cy = rect.top + rect.height / 2;
  for (let i = 0; i < 18; i++) {
    const s = document.createElement("span");
    s.className = "sparkle";
    const angle = (Math.PI * 2 * i) / 18 + Math.random() * 0.4;
    const dist = 60 + Math.random() * 90;
    s.style.left = `${cx}px`;
    s.style.top = `${cy}px`;
    s.style.setProperty("--dx", `${Math.cos(angle) * dist}px`);
    s.style.setProperty("--dy", `${Math.sin(angle) * dist - 40}px`);
    sparkleLayer.appendChild(s);
    setTimeout(() => s.remove(), 1100);
  }
}

function activateMeEspresso() {
  if (meEspresso) return;
  meEspresso = true;
  document.body.classList.add("me-espresso");
  brewLine.textContent = eggLines.brew;
  brandTag.textContent = eggLines.tag;
  footerLine.textContent = eggLines.footer;
  burstSparkles(cup);
  showToast(eggLines.toasts[Math.floor(Math.random() * eggLines.toasts.length)]);
  cup.title = "that's that me";
}

cup.addEventListener("click", () => {
  cup.classList.remove("is-tapped");
  void cup.offsetWidth;
  cup.classList.add("is-tapped");

  if (meEspresso) {
    burstSparkles(cup);
    showToast(eggLines.toasts[Math.floor(Math.random() * eggLines.toasts.length)]);
    return;
  }

  cupTaps += 1;
  clearTimeout(cupTimer);
  cupTimer = setTimeout(() => {
    cupTaps = 0;
  }, 2500);

  if (cupTaps >= CUP_TAPS_NEEDED) {
    cupTaps = 0;
    activateMeEspresso();
  } else if (cupTaps >= 4) {
    showToast(`Keep going… ${CUP_TAPS_NEEDED - cupTaps} more`);
  }
});

run();

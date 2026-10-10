/* =====================================================
   REQUIRED FIELD VALIDATION
   =====================================================
   Every required input has  data-required  and its label
   has a red asterisk (*). validateRequired() highlights
   empty fields, shows a short message under each one and
   returns false if something is missing.
===================================================== */

function isVisible(element) {
    return !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length);
}

function fieldLabel(input) {
    if (input.dataset.label) return input.dataset.label;
    const label = input.id ? document.querySelector('label[for="' + input.id + '"]') : null;
    return label ? label.textContent.replace("*", "").trim() : "This field";
}

function setFieldError(input, message) {
    if (!input) return;

    input.classList.add("input-error");
    input.setAttribute("aria-invalid", "true");

    let note = input.nextElementSibling;
    if (!note || !note.classList.contains("field-error-text")) {
        note = document.createElement("small");
        note.className = "field-error-text";
        input.insertAdjacentElement("afterend", note);
    }
    note.textContent = message;
}

function clearFieldError(input) {
    if (!input) return;

    input.classList.remove("input-error");
    input.removeAttribute("aria-invalid");

    const note = input.nextElementSibling;
    if (note && note.classList.contains("field-error-text")) note.remove();
}

function clearFormErrors(container) {
    container.querySelectorAll(".input-error").forEach(clearFieldError);
}

/**
 * Checks every visible [data-required] field inside container.
 * Returns true when all are filled in.
 */
function validateRequired(container) {
    let firstMissing = null;

    container.querySelectorAll("[data-required]").forEach(function(input) {
        if (!isVisible(input) || input.disabled) {
            clearFieldError(input);
            return;
        }

        const empty = input.type === "checkbox" ? !input.checked : String(input.value || "").trim() === "";

        if (empty) {
            setFieldError(input, fieldLabel(input) + " is required.");
            if (!firstMissing) firstMissing = input;
        } else if (input.classList.contains("input-error") && input.dataset.serverError !== "1") {
            clearFieldError(input);
        }
    });

    if (firstMissing) firstMissing.focus();
    return !firstMissing;
}

/**
 * Shows errors returned by the API: [{field, message}]
 * fieldMap turns API field names into input ids, e.g. { email: "newEmail" }.
 */
function showServerErrors(errors, fieldMap) {
    let first = null;
    (errors || []).forEach(function(error) {
        const input = document.getElementById(fieldMap[error.field] || "");
        if (input && isVisible(input)) {
            setFieldError(input, error.message);
            if (!first) first = input;
        }
    });
    if (first) first.focus();
}

/* Remove the red highlight as soon as the user fixes a field. */
document.addEventListener("input", function(event) {
    if (event.target.classList && event.target.classList.contains("input-error")) {
        clearFieldError(event.target);
    }
});
document.addEventListener("change", function(event) {
    if (event.target.classList && event.target.classList.contains("input-error")) {
        clearFieldError(event.target);
    }
});

/* Message helper: type = "success" | "error" | "warning" | "" */
function showMessage(element, text, type) {
    if (!element) return;
    element.textContent = text || "";
    element.classList.remove("is-success", "is-error", "is-warning");
    if (type) element.classList.add("is-" + type);
}

/* Escape text before putting it into innerHTML. */
function escapeHtml(value) {
    return String(value === null || value === undefined ? "" : value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

// Mensajes del login (sin datos del usuario). Solo texto, sin innerHTML,
// para respetar la CSP igual que el resto de la interfaz.
(function () {
  "use strict";
  var p = new URLSearchParams(location.search);
  var msg = document.getElementById("login-mensaje");
  if (!msg) return;
  if (p.has("error")) {
    msg.textContent = "Usuario o contraseña incorrectos.";
    msg.className = "estado estado--error";
    msg.hidden = false;
  } else if (p.has("salida")) {
    msg.textContent = "Cerraste sesión.";
    msg.className = "estado estado--ok";
    msg.hidden = false;
  }
})();

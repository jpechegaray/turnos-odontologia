/* Marca provisional. Para renombrar el producto, cambiar SOLO BRAND_NAME. */
(function () {
  'use strict';
  var BRAND_NAME = 'Turnos Odontología (provisional)'; // (TBD) nombre definitivo
  var LOGO_ALT = 'Logo provisional de ' + BRAND_NAME;

  function bind() {
    var i, els = document.querySelectorAll('[data-brand-name]');
    for (i = 0; i < els.length; i++) els[i].textContent = BRAND_NAME;
    var imgs = document.querySelectorAll('img[data-brand-logo]');
    for (i = 0; i < imgs.length; i++) imgs[i].setAttribute('alt', LOGO_ALT);
  }

  window.BRAND = Object.freeze({ name: BRAND_NAME, logoAlt: LOGO_ALT });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();

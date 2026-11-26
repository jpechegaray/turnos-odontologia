/* Marca del producto. Para renombrarlo, cambiar SOLO BRAND_NAME (y BRAND_SUBTITULO si hace falta). */
(function () {
  'use strict';
  var BRAND_NAME = 'OdontologIA';
  var BRAND_SUBTITULO = 'Sistema de gestión';
  var LOGO_ALT = 'Logo de ' + BRAND_NAME;

  function bind() {
    var i, els = document.querySelectorAll('[data-brand-name]');
    for (i = 0; i < els.length; i++) els[i].textContent = BRAND_NAME;
    var subs = document.querySelectorAll('[data-brand-subtitulo]');
    for (i = 0; i < subs.length; i++) subs[i].textContent = BRAND_SUBTITULO;
    var imgs = document.querySelectorAll('img[data-brand-logo]');
    for (i = 0; i < imgs.length; i++) imgs[i].setAttribute('alt', LOGO_ALT);
  }

  window.BRAND = Object.freeze({ name: BRAND_NAME, subtitulo: BRAND_SUBTITULO, logoAlt: LOGO_ALT });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();

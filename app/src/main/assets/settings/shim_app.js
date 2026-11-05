// Menghubungkan popup.js (kode ekstensi) ke penyimpanan aplikasi lewat AndroidBridge.
(function () {
  if (!window.AndroidBridge) return;
  var B = window.AndroidBridge;
  window.chrome = {
    storage: {
      local: {
        get: function (defaults, cb) {
          var s = {};
          try { s = JSON.parse(B.getAll() || '{}'); } catch (e) {}
          cb(Object.assign({}, defaults, s));
        },
        set: function (obj, cb) { B.setAll(JSON.stringify(obj)); if (cb) cb(); },
        clear: function (cb) { B.clearAll(); if (cb) cb(); }
      },
      onChanged: { addListener: function () {} }
    }
  };
})();

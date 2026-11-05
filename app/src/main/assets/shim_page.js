// Menyediakan chrome.storage palsu untuk content.js di dalam WebView.
(function () {
  var listeners = [];
  window.chrome = window.chrome || {};
  window.chrome.storage = {
    local: {
      get: function (d, cb) { cb(Object.assign({}, d, window.__LMSC_SETTINGS || {})); }
    },
    onChanged: { addListener: function (f) { listeners.push(f); } }
  };
  window.__lmscUpdate = function (s) {
    window.__LMSC_SETTINGS = s;
    listeners.forEach(function (f) { try { f({}); } catch (e) {} });
  };
})();

(function () {
  var re = /https?:\/\/[^\s"'<>]+export_execute\.php\?[^\s"'<>]*authtoken=[0-9a-f]+[^\s"'<>]*/i;
  var t = document.body ? (document.body.innerText || document.body.textContent || '') : '';
  var m = t.match(re);
  if (!m) {
    var els = document.querySelectorAll('input,textarea,a');
    for (var i = 0; i < els.length; i++) {
      var v = (els[i].value || els[i].href || '') + '';
      var mm = v.match(re);
      if (mm) { m = mm; break; }
    }
  }
  return m ? m[0] : '';
})()

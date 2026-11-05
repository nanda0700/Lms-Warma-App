(function () {
  function pick(name, val) {
    var e = document.querySelector('input[name="' + name + '"][value="' + val + '"]');
    if (e) { e.checked = true; e.dispatchEvent(new Event('change', { bubbles: true })); }
  }
  pick('preset_what', 'all');
  pick('preset_time', 'recentupcoming');
  var b = document.querySelector('input[name="generateurl"],button[name="generateurl"]');
  if (b) b.click();
  return '';
})()

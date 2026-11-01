(function () {
  var a = document.getElementById("dl");
  var b = document.getElementById("dl2");
  var size = document.getElementById("size");
  var mb = document.getElementById("mb");
  fetch("https://api.github.com/repos/web-coder-lab/not-bypass/releases/latest")
    .then(function (res) { return res.ok ? res.json() : null; })
    .then(function (data) {
      if (!data || !data.assets) return;
      var apk = data.assets.find(function (x) { return /\.apk$/.test(x.name); });
      if (!apk) return;
      a.href = apk.browser_download_url;
      b.href = apk.browser_download_url;
      var n = Math.round(apk.size / 1048576);
      size.textContent = apk.name + " · " + n + " MB";
      mb.textContent = n + " MB";
    })
    .catch(function () {});
})();

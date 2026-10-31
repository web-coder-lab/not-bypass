(function () {
  var link = document.getElementById("dl");
  if (!link) return;
  fetch("https://api.github.com/repos/web-coder-lab/not-bypass/releases/latest")
    .then(function (res) { return res.ok ? res.json() : null; })
    .then(function (data) {
      if (!data || !data.assets) return;
      var apk = data.assets.find(function (a) { return /\.apk$/.test(a.name); });
      if (apk) {
        link.href = apk.browser_download_url;
        link.textContent = "Download " + apk.name;
      }
    })
    .catch(function () {});
})();

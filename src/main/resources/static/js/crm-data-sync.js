(function () {
	var SCROLL_KEY = 'crm-reload-scroll';
	var INTERVAL_MS = 15000;

	try {
		var raw = sessionStorage.getItem(SCROLL_KEY);
		if (raw) {
			sessionStorage.removeItem(SCROLL_KEY);
			var o = JSON.parse(raw);
			var here = location.pathname + location.search;
			if (o && o.path === here && typeof o.y === 'number') {
				window.addEventListener('load', function () {
					requestAnimationFrame(function () {
						requestAnimationFrame(function () {
							window.scrollTo(o.x || 0, o.y || 0);
						});
					});
				});
			}
		}
	} catch (e) {
	}

	function saveScrollAndReload() {
		try {
			sessionStorage.setItem(
					SCROLL_KEY,
					JSON.stringify({
						path: location.pathname + location.search,
						x: window.scrollX || 0,
						y: window.scrollY || 0
					}));
		} catch (e2) {
		}
		window.location.reload();
	}

	setInterval(function () {
		if (document.visibilityState === 'hidden') return;
		saveScrollAndReload();
	}, INTERVAL_MS);
})();

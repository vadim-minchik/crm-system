(function () {
	var base = typeof window.__CRM_DATA_REVISION__ === 'number' ? window.__CRM_DATA_REVISION__ : 0;
	var lastKnown = base;
	var pendingStale = false;
	var INTERVAL_MS = 8000;

	function shouldBlockReload() {
		return document.visibilityState === 'hidden' || document.querySelector('.modal.show');
	}

	function tryFlushStale() {
		if (!pendingStale || shouldBlockReload()) return;
		pendingStale = false;
		window.location.reload();
	}

	async function tick() {
		try {
			var res = await fetch('/api/data-revision', {
				credentials: 'same-origin',
				headers: { Accept: 'application/json' }
			});
			if (!res.ok) return;
			var data = await res.json();
			var rev = data.revision;
			if (typeof rev !== 'number' || rev <= lastKnown) return;
			if (shouldBlockReload()) {
				pendingStale = true;
				return;
			}
			lastKnown = rev;
			window.location.reload();
		} catch (e) {
			/* сеть / парсинг — пропускаем тик */
		}
	}

	document.addEventListener('visibilitychange', tryFlushStale);
	document.addEventListener('hidden.bs.modal', tryFlushStale);

	setTimeout(tick, 2000);
	setInterval(tick, INTERVAL_MS);
})();

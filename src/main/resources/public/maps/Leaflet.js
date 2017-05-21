define([ 'dojo/_base/declare' ], function (declare) {
	return declare([], {
		map: null,

		constructor: function (options, node) {
			var map = L.map(node, {
				center: [ options.center.lng, options.center.lat ],
				zoom: options.zoom
			});
			this.map = Promise.resolve(map);

			if (options.tiles === 'thunderforest' || !options.token) {
				L.tileLayer('http://{s}.tile.thunderforest.com/landscape/{z}/{x}/{y}.png', {
					detectRetina: true,
				}).addTo(map);
			}
			else {
				L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/outdoors-v10/tiles/256/{z}/{x}/{y}?access_token=' + options.token, {
					detectRetina: true,
				}).addTo(map);
			}
		},

		addMarker: function (id, geojson) {
			return this.map.then(function (map) {
				L.geoJSON(geojson).addTo(map);
			});
		}
	});
});


define([ 'dojo/_base/declare' ], function (declare) {
	return declare([], {
		map: null,

		constructor: function (options, node) {
			mapboxgl.accessToken = mapboxToken;
			var map = new mapboxgl.Map({
				container: node,
				style: 'mapbox://styles/mapbox/outdoors-v10',
				center: [ options.center.lat, options.center.lng ],
				zoom: options.zoom
			});

			this.map = new Promise(function (resolve) {
				map.on('load', function () {
					map.addSource('contours', {
						type: 'vector',
						url: 'mapbox://mapbox.mapbox-terrain-v2'
					});
					resolve(map);
				});
			});
		},

		addMarker: function (id, geojson) {
			return this.map.then(function (map) {
				map.addLayer({
					id: id,
					type: 'fill',
					source: {
						type: 'geojson',
						data: geojson
					},
					layout: {},
					paint: {
						'fill-color': '#088',
						'fill-opacity': 0.8
					}
				});
			});
		}
	});
});

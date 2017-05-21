define([ 'dojo/_base/declare' ], function (declare) {
	return declare([], {
		map: null,

		constructor: function (options, node) {
			var map = L.map(node, {
				center: [options.center[1], options.center[0]],
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

		getCenter: function () {
			return this.map.then(function (map) {
				var center = map.getCenter();
				return [ center[1], center[0] ];
			});
		},

		_addShape: function (id, geojson) {
			return this.map.then(function (map) {
				var layer = L.geoJSON(geojson);
				layer.addTo(map);
				return layer;
			});
		},

		_removeShape: function (layer) {
			return this.map.then(function (map) {
				map.removeLayer(layer);
			});
		},

		_fitBounds: function (bounds) {
			return this.map.then(function (map) {
				map.flyToBounds([ [ bounds[0][1], bounds[0][0] ], [ bounds[1][1], bounds[1][0] ] ], {
					paddingTopLeft: [ 20, 20 ],
					paddingBottomRight: [ 20, 20 ]
				});
			});
		}
	});
});


define([ './_Map' ], function (_Map) {
	return _Map.createSubclass([], {
		map: null,

		constructor: function (options, node) {
			this.map = new Promise(function (resolve) {
				/* global mapConfig */
				var key = mapConfig.googleMapsKey;

				require([ 'https://maps.googleapis.com/maps/api/js?key=' + key ], function () {
					/* global google */
					if (typeof node === 'string') {
						node = document.getElementById(node);
					}
					var map = new google.maps.Map(node, {
						center: { lng: options.center[0], lat: options.center[1] },
						zoom: options.zoom,
						mapTypeId: 'terrain',
						streetViewControl: false
					});
					resolve(map);
				});
			});
		},

		getCenter: function () {
			return this.map.then(function (map) {
				var center = map.getCenter();
				return [ center.lng(), center.lat() ];
			});
		},

		_addShape: function (id, geojson) {
			return this.map.then(function (map) {
				return geojson.geometry.coordinates.map(function (coordinates) {
					var shape = new google.maps.Polygon({
						paths: coordinates.map(function (coord) {
							return { lng: coord[0], lat: coord[1] };
						}),
						fillColor: '#ff0000',
						fillOpacity: 0.8,
						strokeColor: '#aa4400',
						strokeOpacity: 0.8
					});
					shape.setMap(map);
					return shape;
				});
			});
		},

		_removeShape: function (layer) {
			layer.forEach(function (shape) {
				shape.setMap(null);
			});
		},

		_fitBounds: function (bounds) {
			return this.map.then(function (map) {
				map.fitBounds({
					east: Math.max(bounds[0][0], bounds[1][0]),
					west: Math.min(bounds[0][0], bounds[1][0]),
					north: Math.max(bounds[0][1], bounds[1][1]),
					south: Math.min(bounds[0][1], bounds[1][1])
				}, 20);
			});
		}
	});
});

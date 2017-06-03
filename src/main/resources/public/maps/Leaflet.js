define([ './_Map' ], function (_Map) {
	var initialized;
	var leaflet;

	return _Map.createSubclass([], {
		map: null,

		constructor: function (options, node) {
			this.map = new Promise(function (resolve) {
				if (!initialized) {
					initialized = new Promise(function (resolve) {
						require([
							'dojo/dom-construct',
							'dojo/text!https://unpkg.com/leaflet@1.0.3/dist/leaflet.css',
							'https://unpkg.com/leaflet@1.0.3/dist/leaflet.js'
						], function (domConstruct, styles, leaflet) {
							domConstruct.create('style', { innerHTML: styles }, document.head);
							resolve(leaflet);
						});
					});
				}

				initialized.then(function (_leaflet) {
					leaflet = _leaflet;

					var map = leaflet.map(node, {
						center: [options.center[1], options.center[0]],
						zoom: options.zoom
					});

					map.addEventListener('moveend', function () {
						this.emit('center-change');
					}.bind(this));

					map.addEventListener('zoomend', function () {
						this.emit('bounds-change');
					}.bind(this));

					if (options.tiles === 'thunderforest' || !options.token) {
						leaflet.tileLayer('http://{s}.tile.thunderforest.com/landscape/{z}/{x}/{y}.png', {
							detectRetina: true,
						}).addTo(map);
					}
					else {
						leaflet.tileLayer('https://api.mapbox.com/styles/v1/mapbox/outdoors-v10/' +
							'tiles/256/{z}/{x}/{y}?access_token=' + options.token, {
								detectRetina: true
							}).addTo(map);
					}

					resolve(map);
				}.bind(this));
			}.bind(this));
		},

		getCenter: function () {
			return this.map.then(function (map) {
				var center = map.getCenter();
				return [ center.lng, center.lat ];
			});
		},

		_addShape: function (id, geojson) {
			return this.map.then(function (map) {
				var layer = leaflet.geoJSON(geojson);
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


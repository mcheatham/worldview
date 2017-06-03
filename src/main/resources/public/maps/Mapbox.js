define([ './_Map' ], function (_Map) {
	var initialized;

	return _Map.createSubclass([], {
		map: null,

		constructor: function (options, node) {
			this.map = new Promise(function (resolve) {
				if (!initialized) {
					initialized = new Promise(function (resolve) {
						require([
							'dojo/dom-construct',
							'dojo/text!https://api.mapbox.com/mapbox-gl-js/v0.37.0/mapbox-gl.css',
							'https://api.mapbox.com/mapbox-gl-js/v0.37.0/mapbox-gl.js'
						], function (domConstruct, styles, mapbox) {
							domConstruct.create('style', { innerHTML: styles }, 'head');
							resolve(mapbox);
						});
					});
				}

				initialized.then(function (mapbox) {
					/* global mapConfig */
					mapbox.accessToken = mapConfig.mapboxToken;

					var map = new mapbox.Map({
						container: node,
						style: 'mapbox://styles/mapbox/outdoors-v10',
						center: options.center,
						zoom: options.zoom
					});

					map.on('load', function () {
						map.addSource('contours', {
							type: 'vector',
							url: 'mapbox://mapbox.mapbox-terrain-v2'
						});
						resolve(map);
					});

					map.on('zoomend', function () {
						this.emit('bounds-change', {});
					}.bind(this));

					map.on('moveend', function () {
						this.emit('center-change', {});
					}.bind(this));
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
				map.addLayer({
					id: id,
					source: {
						type: 'geojson',
						data: geojson
					},
					type: 'fill',
					paint: {
						'fill-color': this.fillColor,
						'fill-opacity': this.fillOpacity,
						'fill-outline-color': this.strokeColor
					}
				});
				return id;
			}.bind(this));
		},

		_removeShape: function (id) {
			return this.map.then(function (map) {
				map.removeSource(id);
				map.removeLayer(id);
			});
		},

		_fitBounds: function (bounds) {
			return this.map.then(function (map) {
				map.fitBounds(bounds, {
					padding: { top: 20, left: 20, bottom: 20, right: 20 }
				});
			});
		}
	});
});

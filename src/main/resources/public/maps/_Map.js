define([
	'dojo/_base/declare'
], function (declare) {
	return declare([], {
		_shapes: null,

		fillColor: '#ff0000',
		fillOpacity: 0,
		strokeColor: '#aa4400',
		strokeOpacity: 0.8,

		constructor: function () {
			this._shapes = [];
		},

		fitBounds: function (coords) {
			var minLng = coords.reduce(function (min, coord) {
				return Math.min(min, coord[0]);
			}, coords[0][0]);
			var minLat = coords.reduce(function (min, coord) {
				return Math.min(min, coord[1]);
			}, coords[0][1]);
			var maxLng = coords.reduce(function (max, coord) {
				return Math.max(max, coord[0]);
			}, coords[0][0]);
			var maxLat = coords.reduce(function (max, coord) {
				return Math.max(max, coord[1]);
			}, coords[0][1]);

			return this._fitBounds([ [ minLng, minLat ], [ maxLng, maxLat ] ]);
		},

		addShape: function (id, geojson) {
			var self = this;
			return this._addShape(id, geojson).then(function (shape) {
				self._shapes.push(shape);
			});
		},

		clearShapes: function () {
			var promises = [];
			while (this._shapes.length > 0) {
				promises.push(this._removeShape(this._shapes.pop()));
			}
			return Promise.all(promises);
		}
	});
});

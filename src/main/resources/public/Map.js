define([
	'./maps/Leaflet',
	'./maps/Mapbox'
], function (Leaflet, Mapbox) {
	if (typeof mapboxgl !== 'undefined' && mapboxToken && mapboxToken.indexOf('{mapboxToken}') === -1) {
		return Mapbox;
	}
	else {
		return Leaflet;
	}
});

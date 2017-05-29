require([
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/Memory',
	'dstore/Trackable',
	'dojo/store/Memory',
	'dijit/form/Select',
	'dojo/request',
	'worldview/Map',
	'dojo/domReady!'
], function (
	List,
	Selection,
	Memory,
	Trackable,
	MemoryStore,
	Select,
	request,
	Map
) {
	function getRelatedClasses() {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var cls1 = Object.keys(classes1.selection).filter(function (id) {
			return classes1.selection[id];
		})[0];

		if (cls1 && ont1 && ont2) {
			return request.get('/relatedClasses', {
				query: {
					ontology1: ont1,
					ontology2: ont2,
					class: cls1,
					syn: 0.3,
					sem: 0.3,
					struct: 0.4
				}
			}).then(function (data) {
				data = JSON.parse(data);
				classes2.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
			});
		}
	}

	function getAxioms() {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var cls1 = Object.keys(classes1.selection).filter(function (id) {
			return classes1.selection[id];
		})[0];

		clearClassMarkers();

		if (cls1 && ont1 && ont2) {
			return request.get('/axioms', {
				query: { ontology1: ont1, ontology2: ont2, class: cls1 }
			}).then(function (data) {
				data = JSON.parse(data);
				axioms.set('collection', new TrackableMemory({
					data: data,
					idProperty: 'owl'
				}));
			});
		}
	}

	function showOverlay(/*promise1, promise2, ...*/) {
		wrapper.classList.add('busy');
		Promise.all(Array.prototype.slice.call(arguments)).catch(showError).then(function () {
			wrapper.classList.remove('busy');
		});
	}

	function showError(error) {
		console.error(error);
		alert(error.message);
	}

	function clearClassMarkers() {
		var collection = classes2.get('collection');
		collection.fetchSync({ marked: true }).forEach(function (item) {
			item.marked = false;
			collection.putSync(item);
		});
	}

	var TrackableMemory = Memory.createSubclass([ Trackable ]);

	var ListClass = List.createSubclass([ Selection ], {
		selectionMode: 'single',
		renderRow: function (item) {
			var div = document.createElement('div');
			div.textContent = item[this.label || 'label'];
			if (item.marked) {
				div.classList.add('marked');
			}
			return div;
		},
	});

	var SelectStore = MemoryStore.createSubclass([], {
		getLabel: function (item) {
			return item.label;
		}
	});

	var wrapper = document.getElementById('wrapper');

	var ontology1 = new Select({
		store: new SelectStore()
	}, 'ontology1');

	var classes1 = new ListClass({
		collection: new TrackableMemory()
	}, 'classes1');

	var ontology2 = new Select({
		store: new SelectStore()
	}, 'ontology2');

	var classes2 = new ListClass({
		collection: new TrackableMemory()
	}, 'classes2');

	var axioms = new ListClass({
		collection: new TrackableMemory(),
		label: 'text'
	}, 'axioms');

	ontology1.on('change', function (newValue) {
		showOverlay(request.get('/classes', {
			query: { ontology: newValue }
		}).then(function (data) {
			data = JSON.parse(data);
			classes1.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
			axioms.set('collection', new TrackableMemory());
		}));
	});

	classes1.on('dgrid-select', function () {
		showOverlay(getRelatedClasses().then(function () {
			return getAxioms();
		}));
	});

	ontology2.on('change', function (newValue) {
		showOverlay(
			request.get('/classes', {
				query: { ontology: newValue }
			}).then(function (data) {
				data = JSON.parse(data);
				classes2.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
			}),
			getAxioms()
		);
	});

	axioms.on('dgrid-select', function (event) {
		var collection = axioms.get('collection');
		var axiom = event.rows[0].data;
		var axiomId = collection.getIdentity(axiom);
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');

		// Axioms should relate cls1 to classes int ont2. Highlight those classes.
		var classes2Collection = classes2.get('collection');
		axiom.entities.forEach(function (ent) {
			// Currently, axiom classes aren't properly attributed to their source ontology. Ignore the ontology
			// contained in the class for now and just check if classes2 contains each class.
			var _class = classes2Collection.getSync(ent.URI);
			if (_class) {
				_class.marked = true;
				classes2Collection.putSync(_class);
			}
		});

		map.getCenter().then(function (center) {
			showOverlay(request.get('/coordinates', {
				query: { axiom: axiomId, ontology1: ont1, ontology2: ont2, lng: center[0], lat: center[1] }
			}).then(function (data) {
				data = JSON.parse(data);
				map.clearShapes();

				var allCoords = [];

				Object.keys(data).forEach(function (classUri) {
					data[classUri].forEach(function (shape, index) {
						var coords = shape.points.map(function (point) {
							return [ point.lng, point.lat ];
						});
						allCoords = allCoords.concat(coords);
						map.addShape(classUri + '-' + index, {
							type: 'Feature',
							geometry: {
								type: 'Polygon',
								coordinates: [ coords ]
							}
						});
					});
				});

				if (allCoords.length > 0) {
					map.fitBounds(allCoords);
				}
			}));
		});
	});

	var map = new Map({
		center: [-68.13734351262877, 45.137451890638886],
		zoom: 5
	}, 'map');

	request.get('/ontologies').then(function (data) {
		data = JSON.parse(data);
		var store = new SelectStore({
			data: [ { identifier: '', label: ' ' } ].concat(data),
			idProperty: 'identifier'
		});
		ontology1.set('store', store);
		ontology1.getOptions('').disabled = true;

		store = new SelectStore({
			data: data.filter(function (item) {
				return item.label === 'USGS';
			}),
			idProperty: 'identifier'
		});
		ontology2.set('store', store);
		ontology2.set('value', store.getIdentity(store.data[0]));
	}).otherwise(showError).then(function () {
		wrapper.classList.remove('loading');
	});
});

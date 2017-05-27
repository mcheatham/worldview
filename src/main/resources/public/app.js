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
	function getRelatedEntities() {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var ent1 = Object.keys(entities1.selection).filter(function (id) {
			return entities1.selection[id];
		})[0];

		if (ent1 && ont1 && ont2) {
			return request.get('/relatedEntities', {
				handleAs: 'json',
				query: { ontology1: ont1, ontology2: ont2, entity: ent1 }
			}).then(function (data) {
				entities2.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
			});
		}
	}

	function getAxioms() {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var ent1 = Object.keys(entities1.selection).filter(function (id) {
			return entities1.selection[id];
		})[0];

		clearEntityMarkers();

		if (ent1 && ont1 && ont2) {
			return request.get('/axioms', {
				handleAs: 'json',
				query: { ontology1: ont1, ontology2: ont2, entity: ent1 }
			}).then(function (data) {
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

	function clearEntityMarkers() {
		var collection = entities2.get('collection');
		collection.fetchSync({ marked: true }).forEach(function (item) {
			item.marked = false;
			collection.putSync(item);
		});
	}

	var TrackableMemory = Memory.createSubclass([ Trackable ]);

	var ListClass = List.createSubclass([ Selection ], {
		selectionMode: 'single',
		renderRow: function (item, options) {
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

	var entities1 = new ListClass({
		collection: new TrackableMemory()
	}, 'entities1');

	var ontology2 = new Select({
		store: new SelectStore()
	}, 'ontology2');

	var entities2 = new ListClass({
		collection: new TrackableMemory()
	}, 'entities2');

	var axioms = new ListClass({
		collection: new TrackableMemory(),
		label: 'text'
	}, 'axioms');

	ontology1.on('change', function (newValue) {
		showOverlay(request.get('/entities', {
			handleAs: 'json',
			query: { ontology: newValue }
		}).then(function (data) {
			entities1.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
			axioms.set('collection', new TrackableMemory());
		}));
	});

	entities1.on('dgrid-select', function () {
		showOverlay(getAxioms());
	});

	ontology2.on('change', function (newValue) {
		showOverlay(
			request.get('/entities', {
				handleAs: 'json',
				query: { ontology: newValue }
			}).then(function (data) {
				entities2.set('collection', new TrackableMemory({ data: data, idProperty: 'URI' }));
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

		// Axioms should relate ent1 to entities int ont2. Highlight those entities.
		var addedEntities = {};
		var entities2Collection = entities2.get('collection');
		axiom.entities.forEach(function (entity) {
			// Currently, axiom entities aren't properly attributed to their source ontology. Ignore the ontology
			// contained in the entity for now and just check if entities2 contains each entity.
			var _entity = entities2Collection.getSync(entity.URI);
			if (_entity) {
				_entity.marked = true;
				entities2Collection.putSync(_entity);
			}
		});

		map.getCenter().then(function (center) {
			console.log('map center:', center);
			showOverlay(request.get('/coordinates', {
				handleAs: 'json',
				query: { axiom: axiomId, ontology1: ont1, ontology2: ont2, lng: center[0], lat: center[1] }
			}).then(function (data) {
				map.clearShapes();

				var allCoords = [];

				Object.keys(data).forEach(function (entityUri) {
					data[entityUri].forEach(function (shape, index) {
						var coords = shape.points.map(function (point) {
							return [ point.lng, point.lat ];
						});
						allCoords = allCoords.concat(coords);
						map.addShape(entityUri + '-' + index, {
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

	request.get('/ontologies', { handleAs: 'json' }).then(function (data) {
		var store = new SelectStore({
			data: [ { identifier: '', label: ' ' } ].concat(data),
			idProperty: 'identifier'
		})
		ontology1.set('store', store);
		ontology1.getOptions('').disabled = true;

		store = new SelectStore({
			data: data.filter(function (item) {
				return item.label === 'USGS';
			}),
			idProperty: 'identifier'
		})
		ontology2.set('store', store);
		ontology2.set('value', store.getIdentity(store.data[0]));
	}).otherwise(showError).then(function () {
		wrapper.classList.remove('loading');
	});
});

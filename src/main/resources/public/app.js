require([
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/Memory',
	'dojo/store/Memory',
	'dijit/form/Select',
	'dojo/request',
	'worldview/Map',
	'dojo/domReady!'
], function (
	List,
	Selection,
	Memory,
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
			request.get('/relatedEntities', {
				handleAs: 'json',
				query: { ontology1: ont1, ontology2: ont2, entity: ent1 }
			}).then(function (data) {
				entities2.set('collection', new Memory({ data: data, idProperty: 'uri' }));
			});
		}
	}

	function getAxioms() {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var ent1 = Object.keys(entities1.selection).filter(function (id) {
			return entities1.selection[id];
		})[0];

		if (ent1 && ont1 && ont2) {
			request.get('/axioms', {
				handleAs: 'json',
				query: { ontology1: ont1, ontology2: ont2, entity: ent1 }
			}).then(function (data) {
				axioms.set('collection', new Memory({ data: data }));
			});
		}
	}

	var ListClass = List.createSubclass([ Selection ], {
		selectionMode: 'single',
		renderRow: function (object, options) {
			var div = document.createElement('div');
			div.textContent = object.label;
			return div;
		},
	});

	var SelectStore = MemoryStore.createSubclass([], {
		getLabel: function (item) {
			return item.label;
		}
	});

	var ontology1 = new Select({
		store: new SelectStore()
	}, 'ontology1');

	var entities1 = new ListClass({
		collection: new Memory()
	}, 'entities1');

	var ontology2 = new Select({
		store: new SelectStore()
	}, 'ontology2');

	var entities2 = new ListClass({
		collection: new Memory()
	}, 'entities2');

	var axioms = new ListClass({
		collection: new Memory()
	}, 'axioms');

	request.get('/ontologies', { handleAs: 'json' }).then(function (data) {
		data = [ { id: '', label: ' ' } ].concat(data);
		var store = new SelectStore({ data: data })
		ontology1.set('store', store);
		ontology2.set('store', store);
	});

	ontology1.on('change', function (newValue) {
		request.get('/entities', {
			handleAs: 'json',
			query: { ontology: newValue }
		}).then(function (data) {
			entities1.set('collection', new Memory({ data: data, idProperty: 'uri' }));
		});
		getRelatedEntities();
		getAxioms();
	});

	entities1.on('dgrid-select', function () {
		getRelatedEntities();
		getAxioms();
	});

	ontology2.on('change', function () {
		getRelatedEntities();
		getAxioms();
	});

	axioms.on('dgrid-select', function (event) {
		var collection = axioms.get('collection');
		var axiom = collection.getIdentity(event.rows[0].data);
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		map.getCenter().then(function (center) {
			request.get('/coordinates', {
				handleAs: 'json',
				query: { axiom: axiom, ontology1: ont1, ontology2: ont2, center: center }
			}).then(function (data) {
				map.clearShapes();
				data.forEach(function (entry) {
					map.addShape(entry.entity.uri, {
						type: 'Feature',
						geometry: {
							type: 'Polygon',
							coordinates: [ entry.coordinates ]
						}
					});
				});
				map.fitBounds(data.reduce(function (allCoords, entry) {
					return allCoords.concat(entry.coordinates);
				}, []));
			});
		});
	});

	var map = new Map({
		center: [-68.13734351262877, 45.137451890638886],
		zoom: 5
	}, 'map');

	document.getElementById('wrapper').classList.remove('loading');
});

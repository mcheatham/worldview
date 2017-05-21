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

	var map = new Map({
		center: { lat: -68.13734351262877, lng: 45.137451890638886 },
		zoom: 5
	}, 'map');

	map.addMarker('maine', {
		type: 'Feature',
		geometry: {
			type: 'Polygon',
			coordinates: [[
				[-67.13734351262877, 45.137451890638886],
				[-66.96466, 44.8097],
				[-68.03252, 44.3252],
				[-69.06, 43.98],
				[-70.11617, 43.68405],
				[-70.64573401557249, 43.090083319667144],
				[-70.75102474636725, 43.08003225358635],
				[-70.79761105007827, 43.21973948828747],
				[-70.98176001655037, 43.36789581966826],
				[-70.94416541205806, 43.46633942318431],
				[-71.08482, 45.3052400000002],
				[-70.6600225491012, 45.46022288673396],
				[-70.30495378282376, 45.914794623389355],
				[-70.00014034695016, 46.69317088478567],
				[-69.23708614772835, 47.44777598732787],
				[-68.90478084987546, 47.184794623394396],
				[-68.23430497910454, 47.35462921812177],
				[-67.79035274928509, 47.066248887716995],
				[-67.79141211614706, 45.702585354182816],
				[-67.13734351262877, 45.137451890638886]
			]]
		}
	});

	document.getElementById('wrapper').classList.remove('loading');
});

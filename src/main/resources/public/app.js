require([
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/Memory',
	'dojo/store/Memory',
	'dijit/form/Select',
	'dojo/request',
	'dojo/domReady!'
], function (
	List,
	Selection,
	Memory,
	MemoryStore,
	Select,
	request
) {
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
			entities1.set('collection', new Memory({ data: data }));
		});
		getRelatedEntities();
	});

	entities1.on('dgrid-select', function () {
		getRelatedEntities();
	});

	ontology2.on('change', function () {
		getRelatedEntities();
	});

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
				entities2.set('collection', new Memory({ data: data }));
			});
		}
	}
});

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
		var store = new SelectStore({
			data: data
		})
		ontology1.set('store', store);
	});

	ontology1.on('change', function (newValue) {
		request.get('/entities', {
			handleAs: 'json',
			query: { ontology: newValue }
		}).then(function (data) {
			var collection = new Memory({
				data: data
			})
			entities1.set('collection', collection);
		});
	});
});

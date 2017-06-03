require([
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/Memory',
	'dstore/Trackable',
	'dstore/legacy/DstoreAdapter',
	'dijit/form/Select',
	'dijit/registry',
	'dojo/request',
	'worldview/AxiomEditor',
	'dojo/domReady!'
], function (
	List,
	Selection,
	Memory,
	Trackable,
	DstoreAdapter,
	Select,
	registry,
	request,
	AxiomEditor
) {
	var ONTOLOGY2_ID = 'USGS.owl';

	var TrackableMemory = Memory.createSubclass([ Trackable ], {
		setItems: function (data) {
			var current = this.fetchSync().slice();
			current.forEach(function (item) {
				this.removeSync(this.getIdentity(item));
			}, this);
			data.forEach(function (item) {
				this.putSync(item);
			}, this);
		}
	});

	var ontologyStore = new TrackableMemory({ idProperty: 'identifier' });
	var classStore = new TrackableMemory({ idProperty: 'URI' });
	var axiomStore = new TrackableMemory({ idProperty: 'owl' });

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

	// The wrapper element contains the entire UI and is used to manage the loading overlay
	var wrapper = document.getElementById('wrapper');

	var ontology1 = new Select({
		store: new DstoreAdapter(ontologyStore),
		labelAttr: 'label'
	}, 'ontology1');

	var classes1 = new ListClass({
		collection: classStore.filter({ ontology: '' })
	}, 'classes1');

	var ontology2 = new Select({
		store: new DstoreAdapter(ontologyStore.filter({ identifier: ONTOLOGY2_ID })),
		labelAttr: 'label'
	}, 'ontology2');

	var classes2 = new ListClass({
		collection: classStore.filter({ ontology: '' })
	}, 'classes2');

	var axioms = new ListClass({
		collection: axiomStore,
		label: 'text'
	}, 'axioms');

	ontology1.on('change', function (newValue) {
		handleOntologyChange('ontology1', classes1, newValue);
	});

	classes1.on('dgrid-select', function () {
		showOverlay(getRelatedClasses().then(function () {
			return getAxioms();
		}));
	});

	ontology2.on('change', function (newValue) {
		handleOntologyChange('ontology2', classes2, newValue);
	});

	axioms.on('dgrid-select', function (event) {
		var collection = axioms.get('collection');
		var axiom = event.rows[0].data;
		var axiomId = collection.getIdentity(axiom);
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');

		// Axioms should relate cls1 to classes int ont2. Highlight those classes.
		axiom.entities.filter(function (ent) {
			return ent.ontology === ont2;
		}).forEach(function (ent) {
			// Currently, axiom classes aren't properly attributed to their source ontology. Ignore the ontology
			// contained in the class for now and just check if classes2 contains each class.
			var cls = classStore.getSync(ent.URI);
			if (cls) {
				cls.marked = true;
				classStore.putSync(cls);
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

	var ontologyLoad = request.get('/ontologies').then(function (data) {
		data = JSON.parse(data);
		ontologyStore.setItems([ { identifier: '', label: ' ' } ].concat(data));

		ontology1.set('store', new DstoreAdapter(ontologyStore));
		ontology1.getOptions('').disabled = true;

		ontology2.set('store', new DstoreAdapter(ontologyStore.filter({ label: 'USGS' })));
		var store = ontology2.get('store').store;
		ontology2.set('value', store.getIdentity(store.storage.fullData[0]));
	});

	var map;

	/* global mapConfig */
	var mapLoad = new Promise(function (resolve, reject) {
		require([ 'worldview/maps/' + mapConfig.provider ], function (MapClass) {
			try {
				map = new MapClass({
					center: [-68.13734351262877, 45.137451890638886],
					zoom: 5
				}, 'map');
				resolve();
			}
			catch (error) {
				reject(error);
			}
		});
	});

	Promise.all([ ontologyLoad, mapLoad ]).catch(showError).then(function () {
		wrapper.classList.remove('loading');
	});

	var axiomEditor = new AxiomEditor({
		ontologyStore: ontologyStore,
		classStore: classStore,
		axiomStore: axiomStore
	});
	var axiomEntryNode = document.getElementById('axiom-entry');
	axiomEditor.placeAt(axiomEntryNode);

	axiomEditor.on('submit', function () {
		console.log('created axiom: ' + axiomEditor.get('value'));
	});

	// Start all the widgets
	registry.findWidgets(wrapper).forEach(function (widget) {
		widget.startup();
	});

	// support functions ----------------------------------------------------------

	function clearClassMarkers() {
		var collection = classes2.get('collection');
		collection.fetchSync({ marked: true }).forEach(function (item) {
			item.marked = false;
			collection.putSync(item);
		});
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
				axiomStore.setItems(data);
			});
		}
	}

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
				data.forEach(function (item) {
					classStore.putSync(item);
				});
			});
		}
	}

	function handleOntologyChange(ontology, list, newValue) {
		// Add or remove an 'ontologyX-selected' class from the main wrapper div
		wrapper.classList[newValue ? 'add' : 'remove'](ontology + '-selected');

		// If the store already has entries for the given ontology, just update the list
		if (classStore.filter({ ontology: newValue }).fetchSync().length > 0) {
			updateUI();
		}
		// If not, request them, then update the list
		else {
			showOverlay(request.get('/classes', {
				query: { ontology: newValue }
			}).then(function (data) {
				data = JSON.parse(data);
				data.forEach(function (item) {
					classStore.putSync(item);
				});
				updateUI();
			}));
		}

		function updateUI() {
			list.set('collection', classStore.filter({ ontology: newValue }).sort('label'));
			axiomStore.setItems([]);

			axiomEditor.set('selectedOntologies', [
				ontology1.get('value'),
				ontology2.get('value')
			]);
		}
	}

	function showError(error) {
		console.error(error);
		alert(error.message);
	}

	function showOverlay(/*promise1, promise2, ...*/) {
		wrapper.classList.add('busy');
		Promise.all(Array.prototype.slice.call(arguments)).catch(showError).then(function () {
			wrapper.classList.remove('busy');
		});
	}
});

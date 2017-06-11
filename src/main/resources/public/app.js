require([
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/Memory',
	'dstore/Trackable',
	'dstore/legacy/DstoreAdapter',
	'dijit/form/Select',
	'dijit/registry',
	'dojo/request',
	'dojo/debounce',
	'dojo/on',
	'dojo/dom-construct',
	'worldview/AxiomEditor',
	'dojo/query',
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
	debounce,
	on,
	domConstruct,
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
	var propertyStore = new TrackableMemory({ idProperty: 'URI' });
	var axiomStore = new TrackableMemory({ idProperty: 'owl' });

	var EntityListClass = List.createSubclass([ Selection ], {
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

	var AxiomListClass = EntityListClass.createSubclass([ Selection ], {
		selectionMode: 'none',
		renderRow: function () {
			var div = this.inherited(arguments);
			domConstruct.create('button', { className: 'remove-axiom', innerHTML: 'Remove' }, div);
			return div;
		},
	});

	// The wrapper element contains the entire UI and is used to manage the loading overlay
	var wrapper = document.getElementById('wrapper');

	var ontology1 = new Select({
		store: new DstoreAdapter(ontologyStore),
		labelAttr: 'label'
	}, 'ontology1');

	var classes1 = new EntityListClass({
		collection: classStore.filter({ ontology: '' })
	}, 'classes1');

	var ontology2 = new Select({
		store: new DstoreAdapter(ontologyStore.filter({ identifier: ONTOLOGY2_ID })),
		labelAttr: 'label'
	}, 'ontology2');

	var classes2 = new EntityListClass({
		collection: classStore.filter({ ontology: '' })
	}, 'classes2');

	var axioms = new AxiomListClass({
		collection: axiomStore,
		label: 'text'
	}, 'axioms');

	axioms.on('.remove-axiom:click', function (event) {
		var row = axioms.row(event);
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var axiom = row.id;

		if (confirm('Are you sure you want to delete this axiom?')) {
			showOverlay(request.del('/axioms', {
				query: { ontology1: ont1, ontology2: ont2 },
				data: axiom
			}).then(function () {
				axiomStore.removeSync(axiom);
			}));
		}
	});

	axioms.on('.dgrid-row:click', function (event) {
		if (event.target.classList.contains('remove-axiom')) {
			return;
		}
		axioms.select(axioms.row(event));
	});

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

	var configLoad = request.get('/config').then(function (data) {
		data = JSON.parse(data);
		console.log('Configuring with:', data);

		return new Promise(function (resolve, reject) {
			var provider = 'Leaflet';
			var key;

			if (data.googleMapsKey) {
				key = data.googleMapsKey;
				provider = 'Google';
			}
			else if (data.mapboxToken) {
				key = data.mapboxToken;
				provider = 'Mapbox';
			}

			var initialLng = Number(data.initialLng);
			if (isNaN(initialLng)) {
				initialLng = -68.13734351262877;
			}
			var initialLat = Number(data.initialLat);
			if (isNaN(initialLat)) {
				initialLat = -68.13734351262877;
			}

			require([ 'worldview/maps/' + provider ], function (MapClass) {
				try {
					map = new MapClass({
						center: [initialLng, initialLat],
						zoom: 5,
						key: key
					}, 'map');
					map.startup();

					map.on('bounds-change', debounce(function () {
						axioms.clearSelection();
					}, 500));

					map.on('center-change', debounce(function () {
						axioms.clearSelection();
					}, 500));

					resolve();
				}
				catch (error) {
					reject(error);
				}
			});
		});
	});

	Promise.all([ ontologyLoad, configLoad ]).catch(showError).then(function () {
		wrapper.classList.remove('loading');
	});

	var axiomEntryNode = document.getElementById('axiom-entry');

	var axiomEditor = new AxiomEditor({
		ontologyStore: ontologyStore,
		classStore: classStore,
		axiomStore: axiomStore,
		propertyStore: propertyStore
	});
	axiomEditor.placeAt(axiomEntryNode);

	var saveAxiom = domConstruct.create('button', {
		id: 'save-axiom',
		disabled: true,
		innerHTML: 'Save'
	}, axiomEntryNode);

	axiomEditor.on('axiom-change', function () {
		if (axiomEditor.get('value')) {
			saveAxiom.removeAttribute('disabled');
		}
		else {
			saveAxiom.setAttribute('disabled', 'disabled');
		}
	});

	on(saveAxiom, 'click', function () {
		var axiom = axiomEditor.get('value');
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');

		console.log(axiom);

		showOverlay(request.post('/axioms', {
			query: { ontology1: ont1, ontology2: ont2 },
			data: axiom
		}).then(function (axiom) {
			return getAxioms(axiom);
		}));
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

	function getAxioms(axiomOwl) {
		var ont1 = ontology1.get('value');
		var ont2 = ontology2.get('value');
		var query = {
			ontology1: ont1,
			ontology2: ont2
		};

		if (axiomOwl) {
			query.axiom = axiomOwl;
		}
		else {
			query.class = Object.keys(classes1.selection).filter(function (id) {
				return classes1.selection[id];
			})[0];
		}

		clearClassMarkers();

		if ((query.cls1 || query.axiom) && ont1 && ont2) {
			return request.get('/axioms', {
				query: query
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
					var storeItem = classStore.getSync(classStore.getIdentity(item.ent));
					storeItem.sim = item.sim;
					classStore.putSync(storeItem);
				});

				classes2.set('collection', classStore.filter({ ontology: ont2 }).sort('sim', true));
			}.bind(this));
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
			showOverlay(Promise.all([
				request.get('/classes', {
					query: { ontology: newValue }
				}).then(function (data) {
					data = JSON.parse(data);
					data.forEach(function (item) {
						classStore.putSync(item);
					});
					updateUI();
				}),
				request.get('/properties', {
					query: { ontology: newValue }
				}).then(function (data) {
					data = JSON.parse(data);
					data.forEach(function (item) {
						propertyStore.putSync(item);
					});
				})
			]).then(updateUI));
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

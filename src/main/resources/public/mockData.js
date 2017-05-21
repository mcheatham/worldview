require([
	'dojo/request/registry',
	'dojo/io-query',
	'dojo/when'
], function (registry, ioQuery, when) {
	function logRequest(url, options) {
		console.log(url + ' - [' + options.method + '] - ' + ioQuery.objectToQuery(options.query));
	}

	function queryParam(query, name) {
		if (query[name] == null) {
			console.error('Missing required query param "' + name + '"');
		}
		return query[name];
	}

	var ontologies = [
		{
			id: 'ont1',
			label: 'Ontology 1',
			entities: [
				{
					uri: 'http://geo.com/ent1-1',
					label: 'O1 Entity 1'
				},
				{
					uri: 'http://geo.com/ent1-2',
					label: 'O1 Entity 2'
				},
				{
					uri: 'http://geo.com/ent1-3',
					label: 'O1 Entity 3'
				}
			]
		},
		{
			id: 'ont2',
			label: 'Ontology 2',
			entities: [
				{
					uri: 'http://geo.com/ent2-1',
					label: 'O2 Entity 1'
				},
				{
					uri: 'http://geo.com/ent2-2',
					label: 'O2 Entity 2'
				},
				{
					uri: 'http://geo.com/ent2-3',
					label: 'O2 Entity 3'
				}
			]
		}
	];

	var axioms = [
	]

	registry.register('/ontologies', function (url, options) {
		logRequest(url, options);
		return when(ontologies.map(function (ontology) {
			return { id: ontology.id, label: ontology.label };
		}));
	});

	registry.register('/entities', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var ontology = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology');
		});
		return when(ontology.entities);
	});

	registry.register('/relatedEntities', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var ont1 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology1');
		});
		var ont2 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology2');
		});
		var ent1 = ont1.entities.find(function (entity) {
			return entity.id === queryParam(query, 'entity');
		});
		return when(ont2.entities);
	});

	registry.register('/axioms', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var ont1 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology1');
		});
		var ont2 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology2');
		});
		var ent1 = ont1.entities.find(function (entity) {
			return entity.uri === queryParam(query, 'entity');
		});
		return when(ont1.entities.map(function (entity) {
			var ont2Entities = ont2.entities.slice();
			var ent1 = ont2Entities.splice(Math.floor(Math.random() * ont2Entities.length), 1)[0];
			var ent2 = ont2Entities.splice(Math.floor(Math.random() * ont2Entities.length), 1)[0];

			return {
				id: '<xml><' + Math.random() + '/></xml>',
				label: 'A ' + entity.label + ' is a subclass of ' + ent1.label + ' that has the property ' + ent2.label,
				entities: [ ent1, ent2 ]
			};
		}));
	});

	registry.register('/coordinates', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var ont1 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology1');
		});
		var ont2 = ontologies.find(function (ontology) {
			return ontology.id === queryParam(query, 'ontology2');
		});
		var ent1 = ont1.entities.find(function (entity) {
			return entity.uri === queryParam(query, 'entity');
		});
		return when(ont1.entities.map(function (entity) {
			return {
				id: '<xml><' + Math.random() + '/></xml>',
				label: 'A ' + entity.label + ' is a subclass of ' + + ' that has the property ' + '',
				entities: [
				]
			};
		}));
	});
});

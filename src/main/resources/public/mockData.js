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

	var coordinates = [
		{
			id: 'maine',
			coordinates: [
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
			]
		},
		{
			id: 'bermuda triangle',
			coordinates: [
				[-64.73, 32.31],
				[-80.19, 25.76],
				[-66.09, 18.43],
				[-64.73, 32.31]
			]
		},
		{
			id: 'flemish diamond',
			coordinates:[
				[3.55, 51.08],
				[4.36, 50.73],
				[4.84, 50.85],
				[4.45, 51.30],
				[3.55, 51.08]
			]
		},
		{
			id: 'research triangle',
			coordinates: [
				[-78.93, 36.00],
				[-78.67, 35.78],
				[-79.04, 35.90],
				[-78.93, 36.00]
			]
		}
	];

	// registry.register('/ontologies', function (url, options) {
	// 	logRequest(url, options);
	// 	return when(ontologies.map(function (ontology) {
	// 		return { id: ontology.id, label: ontology.label };
	// 	}));
	// });

	// registry.register('/entities', function (url, options) {
	// 	logRequest(url, options);
	// 	var query = options.query;
	// 	var ontology = ontologies.find(function (ontology) {
	// 		return ontology.id === queryParam(query, 'ontology');
	// 	});
	// 	return when(ontology.entities);
	// });

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

	// registry.register('/axioms', function (url, options) {
	// 	logRequest(url, options);
	// 	var query = options.query;
	// 	var ont1 = ontologies.find(function (ontology) {
	// 		return ontology.id === queryParam(query, 'ontology1');
	// 	});
	// 	var ont2 = ontologies.find(function (ontology) {
	// 		return ontology.id === queryParam(query, 'ontology2');
	// 	});
	// 	var ent1 = ont1.entities.find(function (entity) {
	// 		return entity.uri === queryParam(query, 'entity');
	// 	});
	// 	return when(ont1.entities.map(function (entity) {
	// 		var ont2Entities = ont2.entities.slice();
	// 		var ent1 = ont2Entities.splice(Math.floor(Math.random() * ont2Entities.length), 1)[0];
	// 		var ent2 = ont2Entities.splice(Math.floor(Math.random() * ont2Entities.length), 1)[0];

	// 		return {
	// 			id: '<xml><' + Math.random() + '/></xml>',
	// 			label: 'A ' + entity.label + ' is a subclass of ' + ent1.label + ' that has the property ' + ent2.label,
	// 			entities: [ ent1, ent2 ]
	// 		};
	// 	}));
	// });

	// registry.register('/coordinates', function (url, options) {
	// 	logRequest(url, options);
	// 	var query = options.query;
	// 	var axiom = queryParam(query, 'axiom');
	// 	var lat = queryParam(query, 'lat');
	// 	var lng = queryParam(query, 'lng');
	// 	var ont1 = ontologies.find(function (ontology) {
	// 		return ontology.id === queryParam(query, 'ontology1');
	// 	});
	// 	var ont2 = ontologies.find(function (ontology) {
	// 		return ontology.id === queryParam(query, 'ontology2');
	// 	});
	// 	var coords = coordinates[Math.floor(Math.random() * coordinates.length)];
	// 	return when([
	// 		// {
	// 		// 	entity: ont1.entities[0],
	// 		// 	coordinates: coords.coordinates
	// 		// }
	// 	]);
	// });
});

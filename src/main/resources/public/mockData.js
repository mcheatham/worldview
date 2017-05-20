require([
	'dojo/request/registry',
	'dojo/io-query',
	'dojo/when'
], function (registry, ioQuery, when) {
	function logRequest(url, options) {
		console.log(url + ' - [' + options.method + '] - ' + ioQuery.objectToQuery(options.query));
	}

	registry.register('/ontologies', function (url, options) {
		logRequest(url, options);
		return when([
			{ id: 'foo', label: 'Foo' },
			{ id: 'bar', label: 'Bar' }
		]);
	});

	registry.register('/entities', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var data;

		switch (query.ontology) {
		case 'foo':
			data = [
				{ id: 'foo1', label: 'Foo1' },
				{ id: 'foo2', label: 'Foo2' }
			];
			break;

		case 'bar':
			data = [
				{ id: 'bar1', label: 'Bar1' },
				{ id: 'bar2', label: 'Bar2' }
			];
			break;
		}

		return when(data);
	});

	registry.register('/relatedEntities', function (url, options) {
		logRequest(url, options);
		var query = options.query;
		var data;

		switch (query.entity) {
		case 'foo1':
			data = [
				{ id: 'floof1', label: 'Floof1' },
				{ id: 'floof2', label: 'Floof2' }
			];
			break;

		case 'foo2':
			data = [
				{ id: 'barf1', label: 'Barf1' },
				{ id: 'barf2', label: 'Barf2' }
			];
			break;
		}

		return when(data);
	});
});

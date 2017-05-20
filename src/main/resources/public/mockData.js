require([
	'dojo/request/registry',
	'dojo/when'
], function (registry, when) {
	registry.register('/ontologies', function () {
		return when([
			{ id: 'foo', label: 'Foo' },
			{ id: 'bar', label: 'Bar' }
		]);
	});

	registry.register('/entities', function (url, options) {
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
		var query = options.query;
		var data;
	});
});

define([
	'dijit/_WidgetBase',
	'dijit/_FocusMixin',
	'dijit/form/_FormValueMixin',
	'dijit/form/FilteringSelect',
	'dijit/registry',
	'dojo/dom-construct',
	'dojo/store/Memory'
], function (
	_WidgetBase,
	_FocusMixin,
	_FormValueMixin,
	FilteringSelect,
	registry,
	domConstruct,
	Memory
) {
	/**
	 * ClassExpression :=
	 *     Class |
	 *     ObjectIntersectionOf |
	 *     ObjectUnionOf |
	 *     ObjectComplementOf |
	 *     ObjectSomeValuesFrom |
	 *     ObjectAllValuesFrom |
	 *     ObjectMinCardinality |
	 *     ObjectMaxCardinality |
	 *     ObjectExactCardinality
	 *
	 * ObjectPropertyExpression :=
	 *     ObjectProperty |
	 *     InverseObjectProperty
	 *
	 * {x} := 0 or more x's
	 *
	 * [x] := 0 or 1 x's
	 *
	 * <x> := argument of type x
	 *
	 * ----------------------
	 *
	 * EquivalentClasses(<ClassExpression> <ClassExpression> { <ClassExpression> })
	 *
	 * Ex) EquivalentClasses(Pond, Lake)
	 *
	 * ----------------------
	 *
	 * SubClassOf(<subClassExpression> <superClassExpression>)
	 *
	 * subClassExpression := ClassExpression
	 * superClassExpression := ClassExpression
	 *
	 * Ex) SubClassOf(Pond, Waterbody)
	 *
	 * ----------------------
	 *
	 * DisjointClasses(<ClassExpression> <ClassExpression> { <ClassExpression> })
	 *
	 * Ex) DisjointClasses(Pond, River)
	 *
	 * ----------------------
	 *
	 * ObjectIntersectionOf(<ClassExpression> <ClassExpression> { <ClassExpression> })
	 *
	 * Ex) EquivalentClasses(Reservoir, ObjectIntersectionOf(Waterbody, Freshwater))
	 *
	 * ----------------------
	 *
	 * ObjectUnionOf(<ClassExpression> <ClassExpression> { <ClassExpression>, ... })
	 *
	 * Ex) EquivalentClasses(Wetland, ObjectUnionOf(Swamp, Marsh))
	 *
	 * ----------------------
	 *
	 * ObjectComplementOf(<ClassExpression>)
	 *
	 * Ex) EquivalentClasses(Freshwater, ObjectComplementOf(Saltwater))
	 *
	 * ----------------------
	 *
	 * ObjectSomeValuesFrom(<ObjectPropertyExpression> <ClassExpression>)
	 *
	 * Ex) SubClassOf(FreshwaterPond, ObjectSomeValuesFrom(Contains Freshwater))
	 *
	 * ----------------------
	 *
	 * ObjectAllValuesFrom(<ObjectPropertyExpression> <ClassExpression>)
	 *
	 * Ex) SubClassOf(FreshwaterPond, ObjectAllValuesFrom(Contains Freshwater))
	 *
	 * ----------------------
	 *
	 * ObjectMinCardinality(<nonNegativeInteger> <ObjectPropertyExpression> [ ClassExpression ])
	 *
	 * Ex) DisjointClasses(Landlocked, ObjectMinCardinality(1 hasCoastline))
	 *
	 * ----------------------
	 *
	 * ObjectMaxCardinality(<nonNegativeInteger> <ObjectPropertyExpression> [ ClassExpression ])
	 *
	 * Ex) EquivalentClasses(Dam, ObjectMaxCardinality(2 bordersWaterbody))
	 *
	 * ----------------------
	 *
	 * ObjectExactCardinality(<nonNegativeInteger> <ObjectPropertyExpression> [ ClassExpression ])
	 *
	 * Ex) SubClassOf(Channel, ObjectExactCardinality(2 isConnectedToWaterbody))
	 *
	 * ----------------------
	 *
	 * ObjectInverseOf(<ObjectProperty>)
	 *
	 * Ex) EquivalentProperty(FlowsInto ObjectInverseOf (FlowsOutOf))
	 */
	var relationships = {
		EquivalentClasses: [ 'class', 'class', '{ class }' ],
		SubClassOf: [ 'class (Sub-class)', 'class (Super-class)', ],
		DisjointClasses: [ 'class', 'class', 'class*' ],
		ObjectIntersectionOf: [ 'class', 'class', 'class*' ],
		ObjectUnionOf: [ 'class', 'class', 'class*' ],
		ObjectComplementOf: [ 'class' ],
		ObjectSomeValuesFrom: [ 'property', 'class' ],
		ObjectAllValuesFrom: [ 'property', 'class' ],
		ObjectMinCardinality: [ 'count', 'property', 'class?' ],
		ObjectMaxCardinality: [ 'count', 'property', 'class?' ],
		ObjectExactCardinality: [ 'count', 'property', 'class?' ],
		ObjectInverseOf: [ 'property' ]
	};

	var RelationshipField = _WidgetBase.createSubclass([ _FormValueMixin ], {
		baseClass: 'relationship-editor',

		buildRendering: function () {
			this.inherited(arguments);
			this.focusNode = this.domNode;

			var left = domConstruct.create('div', { className: 'relationship-select' }, this.domNode);
			this.members = domConstruct.create('div', { className: 'relationship-members' }, this.domNode);

			this.select = new FilteringSelect({
				labelAttr: 'name',
				store: new Memory({
					data: Object.keys(relationships).map(function (relationship) {
						return { id: relationship, name: relationship };
					})
				})
			});
			this.select.placeAt(left);

			this.display = domConstruct.create('span', { className: 'relationship-name' }, left);
			this.display.textContent = 'Select a relationship...';
		},

		postCreate: function () {
			this.inherited(arguments);

			this.select.on('change', function (value) {
				registry.findWidgets(this.members).forEach(function (widget) {
					widget.destroy();
				});

				if (value) {
					this.display.textContent = value;
					this.display.classList.add('has-value');

					var rel1 = new RelationshipField();
					rel1.placeAt(this.members);
					var rel2 = new RelationshipField();
					rel2.placeAt(this.members);

					setTimeout(function () {
						rel1.focus();
					}.bind(this));
				}
				else {
					this.display.textContent = 'Select a relationship...';
					this.display.classList.remove('has-value');
					setTimeout(function () {
						this.domNode.focus();
					}.bind(this));
				}
			}.bind(this));

			this.on('click', function (event) {
				if (event.target === this.display) {
					this._enableSelect();
				}
			}.bind(this));

			this.on('keydown', function (event) {
				if (event.key === 'Escape') {
					event.stopPropagation();
					this._disableSelect();
				}
				else if (!this.domNode.classList.contains('focused')) {
					if (event.key === 'Enter') {
						event.stopPropagation();
						this._enableSelect();
					}
					else if (event.key === 'ArrowDown') {
						event.stopPropagation();
						this._enableSelect();
						setTimeout(function () {
							this.select.loadDropDown();
						}.bind(this), 100);
					}
				}
			}.bind(this));

			this.on('focusout', function (event) {
				if (!this.select.domNode.contains(event.relatedTarget)) {
					this.domNode.classList.remove('focused');
				}
			}.bind(this));
		},

		_disableSelect: function () {
			this.domNode.classList.remove('focused');
			setTimeout(function () {
				this.domNode.focus();
			}.bind(this));
		},

		_enableSelect: function () {
			this.domNode.classList.add('focused');
			setTimeout(function () {
				this.select.focus();
			}.bind(this));
		}
	});
                                
	return _WidgetBase.createSubclass([ _FormValueMixin ], {
		baseClass: 'axiom-editor',

		buildRendering: function () {
			this.inherited(arguments);
			this.focusNode = this.domNode;
			this.containerNode = this.domNode;

			this.contentNode = domConstruct.create('div', { className: 'editor-content' }, this.domNode);

			this.relationship = new RelationshipField();
			this.contentNode.appendChild(this.relationship.domNode);
		}
	});
});

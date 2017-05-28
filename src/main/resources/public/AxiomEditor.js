define([
	'dijit/_WidgetBase',
	'dijit/_FocusMixin',
	'dijit/form/_FormValueMixin',
	'dijit/form/FilteringSelect',
	'dijit/form/NumberTextBox',
	'dijit/registry',
	'dojo/dom-construct',
	'dojo/store/Memory'
], function (
	_WidgetBase,
	_FocusMixin,
	_FormValueMixin,
	FilteringSelect,
	NumberTextBox,
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
		EquivalentClasses: [ 'ClassExpression', 'ClassExpression', '{ ClassExpression }' ],
		SubClassOf: [ 'ClassExpression (sub)', 'ClassExpression (super)', ],
		DisjointClasses: [ 'ClassExpression', 'ClassExpression', '{ ClassExpression }' ],
		ObjectIntersectionOf: [ 'ClassExpression', 'ClassExpression', '{ ClassExpression }' ],
		ObjectUnionOf: [ 'ClassExpression', 'ClassExpression', '{ ClassExpression }' ],
		ObjectComplementOf: [ 'ClassExpression' ],
		ObjectSomeValuesFrom: [ 'ObjectPropertyExpression', 'ClassExpression' ],
		ObjectAllValuesFrom: [ 'ObjectPropertyExpression', 'ClassExpression' ],
		ObjectMinCardinality: [ 'nonNegativeInteger', 'ObjectPropertyExpression', '[ ClassExpression ]' ],
		ObjectMaxCardinality: [ 'nonNegativeInteger', 'ObjectPropertyExpression', '[ ClassExpression ]' ],
		ObjectExactCardinality: [ 'nonNegativeInteger', 'ObjectPropertyExpression', '[ ClassExpression ]' ],
		ObjectInverseOf: [ 'ObjectProperty' ],
	};

	var expressions = {
		ObjectPropertyExpression: [ 'ObjectProperty', 'InverseObjectProperty' ],
		InverseObjectProperty: [ 'ObjectInverseOf' ],
		ClassExpression: [ 'Class', 'ObjectIntersectionOf', 'ObjectUnionOf', 'ObjectComplementOf',
			'ObjectSomeValuesFrom', 'ObjectAllValuesFrom', 'ObjectMinCardinality', 'ObjectMaxCardinality',
			'ObjectExactCardinality' ]
	};

	var RelationshipField = _WidgetBase.createSubclass([ _FormValueMixin ], {
		baseClass: 'relationship-editor',
		type: 'Relationship',

		buildRendering: function () {
			this.inherited(arguments);
			this.focusNode = this.domNode;

			this.selectBox = domConstruct.create('div', { className: 'relationship-select' }, this.domNode);

			this.display = domConstruct.create('span', { className: 'relationship-name' }, this.selectBox);
			this.display.textContent = this._getPlaceholderText();

			this.members = domConstruct.create('div', { className: 'relationship-members' }, this.domNode);
		},

		postCreate: function () {
			this.inherited(arguments);

			this.on('change', function (value) {
				console.log('changed to ' + value);

				registry.findWidgets(this.members).forEach(function (widget) {
					widget.destroy();
				});

				this.display.classList.remove('has-value');
				this.display.classList.remove('has-children');

				if (value) {
					this.display.textContent = value;
					this.display.classList.add('has-value');

					var args = relationships[value];
					if (args) {
						this.display.classList.add('has-children');

						var children = args.map(function (arg) {
							var field = new RelationshipField({ type: arg });
							field.placeAt(this.members);
							return field;
						}.bind(this));

						setTimeout(function () {
							children[0].focus();
						}.bind(this));
					}
				}
				else {
					this.display.textContent = this._getPlaceholderText();
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

						if (this.type !== 'nonNegativeInteger') {
							setTimeout(function () {
								this.field.loadDropDown();
							}.bind(this), 100);
						}
					}
				}
				else if (event.key === 'Enter') {
					event.stopPropagation();
					this.domNode.focus();
				}
			}.bind(this));

			this.on('focusout', function (event) {
				if (!this.field.domNode.contains(event.relatedTarget)) {
					this.domNode.classList.remove('focused');
				}
			}.bind(this));
		},

		_setTypeAttr: function (type) {
			this.type = type;

			if (this.field) {
				this.field.destroy();
			}

			if (type === 'nonNegativeInteger') {
				this.field = new NumberTextBox({
					required: false,
					constraints: { min: 0, places: 0 }
				});
			}
			else {
				this.field = new FilteringSelect({
					labelAttr: 'name',
					required: false,
					store: new Memory({ data: this._getItems(type) })
				});
			}

			this.field.placeAt(this.selectBox);
			this.field.domNode.classList.add('relationship-input');
			this.field.on('change', function (newValue) {
				this._handleOnChange(newValue);
			}.bind(this));
		},

		_getPlaceholderText: function () {
			return this.type[0].toUpperCase() + this.type.slice(1);
		},

		_getItems: function (selector) {
			if (selector === 'Relationship') {
				return Object.keys(relationships).map(function (relationship) {
					return { id: relationship, name: relationship };
				});
			}

			var list = expressions[selector];
			if (list == null) {
				return [];
			}

			return list.map(function (item) {
				return { id: item, name: item };
			});
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
				this.field.focus();
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

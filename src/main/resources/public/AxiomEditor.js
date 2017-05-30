define([
	'dijit/_WidgetBase',
	'dijit/form/_ComboBoxMenu',
	'dijit/form/_FormValueMixin',
	'dijit/form/FilteringSelect',
	'dijit/form/NumberTextBox',
	'dojo/dom-construct',
	'dojo/on',
	'dojo/store/Memory'
], function (
	_WidgetBase,
	_ComboBoxMenu,
	_FormValueMixin,
	FilteringSelect,
	NumberTextBox,
	domConstruct,
	on,
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
		// One class from each ontology
		EquivalentClasses: {
			terms: [
				{ name: 'ClassExpression' },
				{ name: 'ClassExpression' }
			],
			sourceRestriction: 'different'
		},
		SubClassOf: {
			terms: [
				{ name: 'ClassExpression', note: '(sub)' },
				{ name: 'ClassExpression', note: '(super)' }
			],
			sourceRestriction: 'different'
		},
		DisjointClasses: {
			terms: [
				{ name: 'ClassExpression' },
				{ name: 'ClassExpression' }
			],
			sourceRestriction: 'different'
		},

		// All classes from the same ontology
		ObjectIntersectionOf: {
			terms: [
				{ name: 'ClassExpression' },
				{ name: 'ClassExpression' },
				{ name: '{ ClassExpression }' }
			],
			sourceRestriction: 'same'
		},
		ObjectUnionOf: {
			terms: [
				{ name: 'ClassExpression' },
				{ name: 'ClassExpression' },
				{ name: '{ ClassExpression }' }
			],
			sourceRestriction: 'same'
		},

		ObjectComplementOf: {
			terms: [ { name: 'ClassExpression' } ]
		},
		ObjectSomeValuesFrom: {
			terms: [ 
				{ name: 'ObjectPropertyExpression' },
				{ name: 'ClassExpression' }
			]
		},
		ObjectAllValuesFrom: {
			terms: [
				{ name: 'ObjectPropertyExpression' },
				{ name: 'ClassExpression' }
			]
		},

		// Class and property from the same ontology
		ObjectMinCardinality: {
			terms: [
				{ name: 'nonNegativeInteger' },
				{ name: 'ObjectPropertyExpression' },
				{ name: '[ ClassExpression ]' }
			],
			sourceRestriction: 'same'
		},
		ObjectMaxCardinality: {
			terms: [
				{ name: 'nonNegativeInteger' },
				{ name: 'ObjectPropertyExpression' },
				{ name: '[ ClassExpression ]' }
			],
			sourceRestriction: 'same'
		},
		ObjectExactCardinality: {
			terms: [
				{ name: 'nonNegativeInteger' },
				{ name: 'ObjectPropertyExpression' },
				{ name: '[ ClassExpression ]' }
			],
			sourceRestriction: 'same'
		},

		ObjectInverseOf: {
			terms: [ { name: 'ObjectProperty' } ]
		}
	};

	var expressions = {
		ObjectPropertyExpression: [ 'ObjectProperty', 'ObjectInverseOf' ],
		ClassExpression: [ 'Class', 'ObjectIntersectionOf', 'ObjectUnionOf', 'ObjectComplementOf',
			'ObjectSomeValuesFrom', 'ObjectAllValuesFrom', 'ObjectMinCardinality', 'ObjectMaxCardinality',
			'ObjectExactCardinality' ]
	};

	var SelectorMenu = _ComboBoxMenu.createSubclass([], {
		_createOption: function (item) {
			var option = this.inherited(arguments);

			if (!item.entity) {
				option.classList.add('relationship');
			}

			return option;
		}
	});

	var Selector = FilteringSelect.createSubclass([], {
		dropDownClass: SelectorMenu,

		/**
		 * Override dijit/popup's behavior of trying to copy the popup widget's border style onto the popup wrapper
		 */
		openDropDown: function () {
			var retVal = this.inherited(arguments);
			this.dropDown._popupWrapper.style.border = '';
			return retVal;
		}
	});

	var RelationshipField = _WidgetBase.createSubclass([ _FormValueMixin ], {
		baseClass: 'relationship-editor',
		term: null,
		field: null,
		operands: null,
		sourceRestriction: null,
		ontologyStore: null,
		classStore: null,

		constructor: function () {
			this.operands = [];
			this.term = { name: 'Relationship' };
		},

		buildRendering: function () {
			this.inherited(arguments);
			this.focusNode = this.domNode;

			this.selectBox = domConstruct.create('div', { className: 'relationship-select' }, this.domNode);
			this.operandsNode = domConstruct.create('div', { className: 'relationship-operands' }, this.domNode);
		},

		postCreate: function () {
			this.inherited(arguments);

			this.on('change', function (value) {
				if (this.term.name === 'nonNegativeInteger') {
					return;
				}

				var store = this.editor.get('store');
				var item = store.get(value);

				this._createTemplate(value);

				this.emit('expression-change', {
					detail: {
						item: item,
						widget: this
					}
				});
			}.bind(this));

			on(this.operandsNode, 'expression-change', function (event) {
				// We don't care about expression changes here unless expression selector sources have a relationship
				if (this.sourceRestriction == null) {
					return;
				}

				// this.set('firstSource', event.detail.item.entity.ontology);
				this._updateExpressionOptions(event.detail.widget, event.detail.item.entity.ontology);
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
								this.editor.loadDropDown();
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
				if (!this.editor.domNode.contains(event.relatedTarget)) {
					this.domNode.classList.remove('focused');
				}
			}.bind(this));
		},

		_createTemplate: function (value) {
			while (this.operands.length > 0) {
				this.operands.pop().destroy();
			}

			if (value) {
				var info = relationships[value];
				if (info) {
					this.sourceRestriction = info.sourceRestriction;

					this.operands = info.terms.map(function (term) {
						var editor = new RelationshipField({
							term: term,
							ontologyStore: this.ontologyStore,
							classStore: this.classStore
						});
						editor.placeAt(this.operandsNode);
						return editor;
					}.bind(this));

					setTimeout(function () {
						this.operands[0].focus();
					}.bind(this));
				}
			}
			else {
				// this.display.textContent = this._getPlaceholderText();
				setTimeout(function () {
					this.domNode.focus();
				}.bind(this));
			}
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
				this.editor.focus();
			}.bind(this));
		},

		_getItems: function () {
			// A selector may have other information, e.g., "ClassExpression (sub)"
			var items;
			var name = this.term.name;

			if (name === 'Relationship') {
				items = [ 'EquivalentClasses', 'SubClassOf', 'DisjointClasses' ].map(function (relationship) {
					return { id: relationship, name: relationship };
				});
			}
			else {
				var list = expressions[name];
				if (list == null) {
					items = [];
				}
				else {
					items = list.reduce(function (items, item) {
						if (item === 'Class') {
							var storeItems; 

							if (this.term.store) {
								storeItems = this.term.store.fetchSync();
							}
							else {
								storeItems = this.classStore.fetchSync();
							}

							return items.concat(storeItems.map(function (item) {
								return {
									id: item.URI,
									name: item.label,
									entity: item
								};
							}));
						}
						else {
							return items.concat([{ id: item, name: item }]);
						}
					}.bind(this), []);
				}
			}
				
			return items.sort(function (a, b) {
				if (a.value && !b.value) {
					return 1;
				}
				if (!a.value && b.value) {
					return -1;
				}
				if (a.name < b.name) {
					return -1;
				}
				if (a.name > b.name) {
					return 1;
				}
				return 0;
			});
		},

		_setOntologyStoreAttr: function (store) {
			this.ontologyStore = store;
			this.operands.forEach(function (operand) {
				operand.set('ontologyStore', store);
			});
		},

		_setClassStoreAttr: function (store) {
			this.classStore = store;
			this.operands.forEach(function (operand) {
				operand.set('classStore', store);
			});
		},

		/**
		 * Update the option lists for the expressions in this relationship based the just-set value for a given widget
		 */
		_updateExpressionOptions: function (widget, source) {
			if (this.sourceRestriction === 'same') {
				this.operands.filter(function (operand) {
					return operand !== widget;
				}).forEach(function (operand) {
					var term = operand.get('term');
					term.store = this.classStore.filter({ ontology: source });
					term.value = operand.get('value');
					operand.set('term', term);
				}.bind(this));
			}
			else if (this.sourceRestriction === 'different') {
				this.operands.filter(function (operand) {
					return operand !== widget;
				}).forEach(function (operand) {
					var term = operand.get('term');
					term.store = this.classStore.filter(new this.classStore.Filter().ne('ontology', source));
					term.value = operand.get('value');
					operand.set('term', term);
				}.bind(this));
			}
		},

		_setTermAttr: function (term) {
			this.term = term;

			if (this.editor) {
				this.editor.destroy();
			}

			if (term.name === 'nonNegativeInteger') {
				this.editor = new NumberTextBox({
					required: false,
					constraints: { min: 0, places: 0 }
				});
			}
			else {
				this.editor = new Selector({
					labelAttr: 'name',
					required: false,
					store: new Memory({ data: this._getItems() })
				});
			}

			if (term.value) {
				this.editor.set('value', term.value);
			}

			this.editor.set('placeHolder', term.name);
			this.editor.placeAt(this.selectBox);
			this.editor.domNode.classList.add('relationship-input');
			this.editor.on('change', function (newValue) {
				this._handleOnChange(newValue);
			}.bind(this));
		}
	});
                                
	return _WidgetBase.createSubclass([ _FormValueMixin ], {
		baseClass: 'axiom-editor',

		ontologyStore: null,
		classStore: null,
		axiomStore: null,

		buildRendering: function () {
			this.inherited(arguments);
			this.focusNode = this.domNode;
			this.containerNode = this.domNode;

			this.contentNode = domConstruct.create('div', { className: 'editor-content' }, this.domNode);

			this.relationship = new RelationshipField();
			this.contentNode.appendChild(this.relationship.domNode);
		},

		_setOntologyStoreAttr: function (store) {
			this.ontologyStore = store;
			this.relationship.set('ontologyStore', store);
		},

		_setClassStoreAttr: function (store) {
			this.classStore = store;
			this.relationship.set('classStore', store);
		},

		_setAxiomStoreAttr: function (store) {
			this.axiomStore = store;
		}
	});
});

import _ from 'lodash';
import flatMap from '../lib/flatMap';
import SearchOperator from './search-operator';
import SearchSuggestion from './search-suggestion';
import { stringToCurrency } from '../lib/format-currency';

const operators = {
  currency: [
    { operator: 'eq', display: '=' },
    { operator: 'neq', display: '<>' },
    { operator: 'gt', display: '>' },
    { operator: 'gte', display: '>=' },
    { operator: 'lt', display: '<' },
    { operator: 'lte', display: '<=' }
  ],
  date: [
    { operator: 'eq', display: '=' },
    { operator: 'neq', display: '<>' },
    { operator: 'gt', display: '>' },
    { operator: 'gte', display: '>=' },
    { operator: 'lt', display: '<' },
    { operator: 'lte', display: '<=' }
  ],
  enum: [
    { operator: 'eq', display: ':' }
  ],
  number: [
    { operator: 'eq', display: '=' },
    { operator: 'neq', display: '<>' },
    { operator: 'gt', display: '>' },
    { operator: 'gte', display: '>=' },
    { operator: 'lt', display: '<' },
    { operator: 'lte', display: '<=' }
  ],
  string: [
    { operator: 'eq', display: ':' }
  ],
  'string-term': [
    { operator: 'eq', display: ':' }
  ],
  'term': [
    { operator: 'eq', display: ':' }
  ],
};

const boolSuggestions = [
  { display: 'True', operator: 'eq', value: true },
  { display: 'False', operator: 'eq', value: false },
];

export function getInputMask(searchTerm) {
  const type = _.get(searchTerm, 'type', '');

  switch(type) {
    case 'currency':
      return '99.99';
    case 'date':
      return '99/99/9999';
    default:
      return '';
  }
}

/**
 * SearchTerm represents a single term in the context of either the Live Search
 * or the Dynamic Customer Group.
 */
export default class SearchTerm {
  static potentialTerms(searchTerms, str) {
    return flatMap(searchTerms, term => term.applicableTerms(str));
  }

  static normalizeSearchTerm(searchTerm) {
    return _.reduce(searchTerm.split(':'), (result, term) => {
      const sep = result !== '' ? ' : ' : '';
      return `${result}${sep}${term.trim().toLowerCase()}`;
    }, '');
  }

  constructor(searchTermJson, parentTitle = '') {
    if (_.isEmpty(parentTitle)) {
      this._title = searchTermJson.title;
    } else {
      this._title = `${parentTitle} : ${searchTermJson.title}`;
    }

    this._type = searchTermJson.type;
    this._term = searchTermJson.term;

    if (!_.isEmpty(searchTermJson.options)) {
      this._options = searchTermJson.options.map(o => new SearchTerm(o, this._title));
    } else if (!_.isEmpty(searchTermJson.suggestions)) {
      this._suggestions = searchTermJson.suggestions;
    }

    // Predefined suggestions or options for some types
    switch (this._type) {
      case 'bool':
        this._suggestions = boolSuggestions;
        break;
    }
  }

  get children() {
    if (!_.isEmpty(this._options)) {
      return this._options;
    } else if (!_.isEmpty(this.operators) && this.operators.length > 1) {
      return this.operators.map(o => new SearchOperator(o, this));
    } else if (!_.isEmpty(this.suggestions) && this.suggestions.length > 1) {
      return this.suggestions.map(s => new SearchSuggestion(s, this));
    } else {
      return [];
    }
  }

  get displayTerm() {
    return this._title;
  }

  get displayAction() {
    if (this._type != 'value') {
      return ' : Search';
    } else {
      return '';
    }
  }

  get operators() {
    return operators[this._type];
  }

  get selectionValue() {
    if (this._type == 'value') {
      return this.displayTerm;
    } else {
      return `${this.displayTerm} : `;
    }
  }

  get suggestions() {
    return this._suggestions;
  }

  get type() {
    return this._type;
  }

  /**
   * Finds the search terms that are applicable based on a given search value.
   * It may either be the current term, or one if its child options/suggestions.
   * @param {string} search The search term so search against.
   * @return {Array} An array of search terms that should be visible. An empty
   *                 list if none are found.
   */
  applicableTerms(search) {
    const nSearch = SearchTerm.normalizeSearchTerm(search);
    const nTerm = this.displayTerm.toLowerCase();
    let terms = [];

    if (this.matchesSearchTerm(search)) {
      terms.push(this);
    } else if (_.startsWith(nSearch, nTerm) && !_.isEmpty(this.children)) {
      terms = flatMap(this.children, child => child.applicableTerms(search));
    }

    return terms;
  }

  /**
   * Checks to see a specific search term is a match for a given search string.
   * It does not check to see if any child terms match.
   * @param {string} search The search string to search against.
   * @returns {bool} True if the term matches, false otherwise.
   */
  matchesSearchTerm(search) {
    const nSearch = SearchTerm.normalizeSearchTerm(search).toLowerCase();
    const nTerm = this.displayTerm.toLowerCase();

    if (_.startsWith(nTerm, nSearch)) {
      return true;
    } else if (_.isEmpty(this._options) && _.isEmpty(this._suggestions)) {
      return nSearch.length && removeValue(nSearch) == nTerm;
    }
  }

  /**
   * Checks to see if the search term can currently be selected.
   * @param {string} search The search term that's being searched.
   * @return {Boolean} True if it can be selected, false otherwise.
   */
  selectTerm(search) {
    // Eliminate a hanging colon, we don't want to think an empty string
    // is the search term.
    const nSearch = _.trim(SearchTerm.normalizeSearchTerm(search), ': ');
    const nTerm = this.displayTerm.toLowerCase();

    if (this._type == 'value') {
      return nSearch === nTerm;
    } else if (_.isEmpty(this._options)) {
      return nSearch.length > nTerm.length && removeValue(nSearch) === nTerm;
    }

    return false;
  }

  /**
   * Converts the SearchTerm into the metadata needed to make a query against
   * ElasticSearch. Meaning: the actual term that gets searched, the operator
   * to search with, the value being searched, and a string needed to display
   * the search in the UI.
   * @param {string} search The final search term.
   * @return {object} Representation of the search.
   */
  toFilter(search) {
    return {
      display: search,
      term: this._term,
      operator: _.get(operators, [this._type, 0, 'operator'], ''),
      value: {
        type: this._type,
        value: getValue(search, this._type)
      }
    };
  }
}

function getValue(searchTerm, type) {
  const lastIdx = searchTerm.lastIndexOf(':');
  const value = lastIdx > -1 ? searchTerm.slice(lastIdx + 1).trim() : '';
  return type == 'currency' ? _.trim(stringToCurrency(value), ' $') : value;
}

function removeValue(searchTerm) {
  const lastIdx = searchTerm.lastIndexOf(':');
  return lastIdx > -1 ? searchTerm.slice(0, lastIdx).trim() : searchTerm;
}

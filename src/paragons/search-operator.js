import _ from 'lodash';
import SearchTerm from './search-term';

export default class SearchOperator {
  constructor(operator, searchTerm) {
    this._operator = operator;
    this._searchTerm = searchTerm;

    this._title = `${searchTerm.selectionValue}${operator.display}`;
  }

  get children() {
    if (_.get(this, ['_searchTerm', 'suggestions', 'length'], 0)) {
      return this._searchTerm.suggestions;
    } else {
      return [];
    }
  }

  get displayTerm() {
    return this._title;
  }

  get displayAction() {
    return this._searchTerm.displayAction;
  }

  get selectionValue() {
    return `${this.displayTerm} : `;
  }

  get type() {
    return this._searchTerm.type;
  }

  applicableTerms(search) {
    return this.matchesSearchTerm(search) ? [this] : [];
  }

  matchesSearchTerm(search) {
    const opIndex = this.operatorIndex(search);
    const nSearch = SearchTerm.normalizeSearchTerm(search).toLowerCase();
    const nTerm = this.displayTerm.toLowerCase();

    if (opIndex == -1) {
      return _.startsWith(nTerm, nSearch);
    } else {
      return this.removeValue(nSearch) == nTerm;
    }
  }

  operatorIndex(search) {
    const op = this._operator.display;
    return search.lastIndexOf(op);
  }

  removeValue(searchTerm) {
    const opIdx = searchTerm.lastIndexOf(':');
    return opIdx > -1
      ? searchTerm.slice(0, opIdx).trim()
      : searchTerm;
  }

  selectTerm(search) {
    return this.matchesSearchTerm(search) && search.trim().length > this.selectionValue.length;
  }

  toFilter(search) {
    return {
      ...this._searchTerm.toFilter(search),
      selectedOperator: this._operator.operator
    };
  }
}

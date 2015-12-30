import _ from 'lodash';
import util from 'util';

const flatMap = _.compose(_.flatten, _.map);

/**
 * SearchTerm represents a single term in the context of either the Live Search
 * or the Dynamic Customer Group.
 */
export default class SearchTerm {
  static potentialTerms(searchTerms, str) {
    return flatMap(searchTerms, term => term.applicableTerms(str));
  }

  constructor(searchTermJson, parentTitle = '') {
    if (_.isEmpty(parentTitle)) {
      this._title = searchTermJson.title;
    } else {
      this._title = `${parentTitle} : ${searchTermJson.title}`;
    }

    this._type = searchTermJson.type;

    if (!_.isEmpty(searchTermJson.options)) {
      this._options = searchTermJson.options.map(o => new SearchTerm(o, this._title));
    } else if (!_.isEmpty(searchTermJson.suggestions)) {
      this._options = searchTermJson.suggestions.map(s => {
        const suggestion = { options: [], suggestions: [], title: s, type: 'value' };
        return new SearchTerm(suggestion, this._title);
      });
    }
  }

  get displayTerm() {
    return this._title;
  }

  get displayAction() {
    if (this._type != 'value') {
      return 'Search';
    } else {
      return '';
    }
  }

  get selectionValue() {
    if (this._type == 'value') {
      return this.displayTerm;
    } else {
      return `${this.displayTerm} : `;
    }
  }

  /**
   * Finds the search terms that are applicable based on a given search value.
   * It may either be the current term, or one if its child options/suggestions.
   * @param {string} search The search term so search against.
   * @return {Array} An array of search terms that should be visible. An empty
   *                 list if none are found.
   */
  applicableTerms(search) {
    const nSearch = normalizeSearchTerm(search);
    const nTerm = this.displayTerm.toLowerCase();
    let terms = [];

    if (this.matchesSearchTerm(search)) {
      terms.push(this);
    } else if (_.startsWith(nSearch, nTerm) && !_.isEmpty(this._options)) {
      terms = flatMap(this._options, option => option.applicableTerms(search));
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
    const nSearch = normalizeSearchTerm(search).toLowerCase();
    const nTerm = this.displayTerm.toLowerCase();

    if (_.startsWith(nTerm, nSearch)) {
      return true;
    } else if (_.isEmpty(this._options) && this._type != 'value') {
      return nSearch.length && removeValue(nSearch) == nTerm;
    }
  }


  selectTerm(search) {
    // Eliminate a hanging colon, we don't want to think an empty string
    // is the search term.
    const nSearch = _.trim(normalizeSearchTerm(search), ': ');
    const nTerm = this.displayTerm.toLowerCase();

    if (this._type == 'value') {
      return nSearch === nTerm;
    } else if (_.isEmpty(this._options)) {
      return nSearch.length > nTerm.length && removeValue(nSearch) === nTerm;
    }

    return false;
  }
}

function normalizeSearchTerm(searchTerm) {
  return _.reduce(searchTerm.split(':'), (result, term) => {
    const sep = result !== '' ? ' : ' : '';
    return `${result}${sep}${term.trim().toLowerCase()}`;
  }, '');
}

function removeValue(searchTerm) {
  // Find the last colon and extract everything after that.
  const lastIdx = searchTerm.lastIndexOf(':');
  return lastIdx > -1 ? searchTerm.slice(0, lastIdx).trim() : searchTerm;
}

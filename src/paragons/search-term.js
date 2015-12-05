import _ from 'lodash';
import util from 'util';

/**
 * SearchTerm represents a single term in the context of either the Live Search
 * or the Dynamic Customer Group.
 */ 
export default class SearchTerm {
  constructor(searchTermJson, parentTitle = '') {
    if (_.isEmpty(parentTitle)) {
      this._term = searchTermJson.term;
    } else {
      this._term = `${parentTitle} : ${searchTermJson.term}`;
    }
    
    this._type = searchTermJson.type;

    if (!_.isEmpty(searchTermJson.options)) {
      this._options = searchTermJson.options.map(o => new SearchTerm(o, this._term));
    }

    if (!_.isEmpty(searchTermJson.suggestions)) {
      this._suggestions = searchTermJson.suggestions.map(s => {
        const suggestion = {
          options: [],
          suggestions: [],
          term: s,
          type: 'value'
        };
        return new SearchTerm(suggestion, this._term);
      });
    }
  }

  get displayTerm() {
    return this._term;
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
   * @return {array} An array of search terms that should be visible. An empty
   *                 list if none are found.
   */
  applicableTerms(search) {
    const nSearch = normalizeSearchTerm(search);
    const nTerm = this.displayTerm.toLowerCase();

    if (_.startsWith(nTerm, nSearch)) {
      return [this];
    } else if (_.startsWith(nSearch, nTerm)) {
      if (!_.isEmpty(this._options)) {
        return _.transform(this._options, (result, option) => {
          _.forEach(option.applicableTerms(search), term => result.push(term));
        });
      } else if (!_.isEmpty(this._suggestions)) {
        return _.transform(this._suggestions, (result, suggestion) => {
          _.forEach(suggestion.applicableTerms(search), term => result.push(term));
        });
      } else if (this._type != 'value' && nSearch.length > nTerm.length) {
        // Truncate the search term because of the effect on the value.
        // Don't do this for value options, because we always want exact matches.
        if (removeValue(nSearch) == nTerm) {
          return [this];
        }
      }
    }

    return [];
  }

  selectTerm(search) {
    const nSearch = normalizeSearchTerm(search);
    const nTerm = this.displayTerm.toLowerCase();

    if (this._type == 'value') {
      return nSearch === nTerm;
    } else {
      if (_.isEmpty(this._options) && _.isEmpty(this._suggestions)) {
        return nSearch.length > nTerm.length && removeValue(nSearch) === nTerm;
      }
    }

    return false;
  }
}

function normalizeSearchTerm(searchTerm) {
  const comps = searchTerm.split(':');
  let ret = '';
  for(let i = 0; i < comps.length; i++) {
    if (i != 0) {
      ret += ' : ';
    }
    ret += comps[i].trim().toLowerCase();
  }
  return ret;
}

function removeValue(searchTerm) {
  // Find the last color and extract everything after that.
  const lastIdx = searchTerm.lastIndexOf(':');
  if (lastIdx > -1) {
    const term = searchTerm.slice(0, lastIdx).trim();
    return term;
  } else {
    return searchTerm;
  }
}

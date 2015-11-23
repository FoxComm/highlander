import React from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';

import QueryBuilder from './query-builder';
import ordersSearchTerms from './orders-search-terms';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    const options = flattenOptions(ordersSearchTerms);
    super(props, context);
    this.state = {
      value: '',
      queryBuilder: {
        show: false,
        selectedIndex: -1,
        searchOptions: options,
        visibleSearchOptions: visibleOptions(options)
      },
      searches: []
    };
  }

  @autobind
  updateSearch(searchTerm) {
    const visibleSearchOptions = visibleOptions(this.state.queryBuilder.searchOptions, searchTerm);
    const showQB = !_.isEmpty(visibleSearchOptions);

    this.setState({
      ...this.state,
      value: searchTerm,
      queryBuilder: {
        ...this.state.queryBuilder,
        show: showQB,
        selectedIndex: -1,
        visibleSearchOptions: visibleSearchOptions
      }
    });
  }

  @autobind
  onChange({target}) {
    this.updateSearch(target.value);
  } 

  @autobind
  inputFocus() {
    const searchTerm = this.state.value;
    const searchOptions = this.state.queryBuilder.searchOptions;
    const visibleSearchOptions = visibleOptions(searchOptions, searchTerm);

    this.setState({
      ...this.state,
      queryBuilder: {
        ...this.state.queryBuilder,
        visibleSearchOptions: visibleSearchOptions,
        show: true
      }
    });
  }

  @autobind
  keyDown(event) {
    let selectedIndex = this.state.queryBuilder.selectedIndex;
    const visibleSearchOptions = this.state.queryBuilder.visibleSearchOptions;
    
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault(); 
        if (!this.state.queryBuilder.show) {
          this.inputFocus();
          return;
        } else {
          selectedIndex = Math.min(selectedIndex + 1, visibleSearchOptions.length - 1);
        }
        break;
      case 38:
        // Up arrow
        event.preventDefault(); 
        selectedIndex = Math.max(selectedIndex - 1, -1);
        break;
      case 13:
        // Enter
        event.preventDefault();
        if (visibleSearchOptions.length == 1) {
          if (readyToSubmit(visibleSearchOptions[0], this.state.value)) {
            let newSearches = this.state.searches;
            newSearches.push(this.state.value);

            this.setState({
              ...this.state,
              value: '',
              searches: newSearches,
              queryBuilder: {
                ...this.state.queryBuilder,
                show: false
              }
            });
          }
        } else {
          this.selectOption(selectedIndex);
        }
        return;
      default:
        // Untracked action.
        return;
    };

    this.setState({
      ...this.state,
      queryBuilder: {
        ...this.state.queryBuilder,
        selectedIndex: selectedIndex,
      }
    });
  }

  @autobind
  goBack() {
    this.updateSearch(backSearchTerm(this.state.value));
  }

  @autobind
  selectOption(idx) {
    const visibleSearchOptions = this.state.queryBuilder.visibleSearchOptions;
    if (idx > -1) {
      const inputValue = `${visibleSearchOptions[idx].term} : `;
      this.updateSearch(inputValue);
    }
  }

  get searchPills() {
    return this.state.searches.map((search, idx) => {
      return (
        <div className='fc-live-search-pill' key={`search-${idx}`}>
          <i className='icon-search fc-live-search-pill-icon'></i>
          {search}
          <div className='fc-live-search-pill-close'>
            &times;
          </div>
        </div>
      );
    });
  }

  render() {
    return (
      <div className="fc-col-md-1-1 fc-live-search">
        <div>
          <form>
            <div className='fc-live-search-meta-container'>
              <div className="fc-live-search-input-container">
                <div className='fc-live-search-pills'>
                  {this.searchPills}
                </div>
                <div className="fc-live-search-icon-wrapper">
                  <i className="icon-search"></i>
                </div>
                <div className="fc-live-search-input-wrapper">
                  <input 
                    className="fc-live-search-input-field"
                    type="text" 
                    placeholder="Add another filter or keyword search"
                    onChange={this.onChange}
                    onKeyDown={this.keyDown}
                    onFocus={this.inputFocus}
                    value={this.state.value}
                  />
                </div>
              </div>
              <div className='fc-live-search-btn-container'>
                <button className='fc-btn fc-live-search-btn'>Save Search</button>
              </div>
            </div>
          </form>
        </div>
        { this.state.queryBuilder.show &&
          <QueryBuilder 
            selectOption={this.selectOption}
            selectedIndex={this.state.queryBuilder.selectedIndex} 
            searchOptions={this.state.queryBuilder.visibleSearchOptions}
            onGoBack={this.goBack} /> }
      </div>
    );
  }
}

const flattenOptions = (options, term = '') => {
  const allOptions = options.map(option => {
    const termPrefix = _.isEmpty(term) ? '' : `${term} : `;
    let returnOptions = [{
      term: `${termPrefix}${option.term}`,
      type: option.type,
      suggestions: option.suggestions,
      exactMatch: _.isEmpty(option.options),
    }];
    
    if (!_.isEmpty(option.options)) {
      returnOptions = _.union(returnOptions, flattenOptions(option.options, returnOptions[0].term));
      returnOptions = _.flatten(returnOptions);
    }    

    return returnOptions;
  });

  return _.flatten(allOptions);
}

/**
 * Implementation of the algorithm used for showing options in the QueryBuilder
 * dropdown. The algorithm should be:
 *
 * TODO: Consider moving all of this logic into the child scope.
 */
const visibleOptions = (optionsList, term = '') => {
  const searchTerm = term.trim().toLowerCase();

  let visibleSearchOptions = _.transform(optionsList, (result, option) => {
    if (optionShouldBeVisible(option, searchTerm)) {
      result.push(option);
    }
  });

  return visibleSearchOptions;
};

const optionShouldBeVisible = (option, term) => {
  const optionTerm = option.term.toLowerCase();

  let searchTerm = term;
  if (option.exactMatch) {
    const truncatedLength = Math.min(option.term.length, term.length);
    searchTerm = term.slice(0, truncatedLength);
  }

  const colonsInSearchTerm = (searchTerm.match(/:/g) || []).length;
  const colonsInOption = (option.term.match(/:/g) || []).length;

  return colonsInSearchTerm == colonsInOption && _.startsWith(optionTerm, searchTerm);
};

const readyToSubmit = (option, term) => {
  const optionTerm = option.term.toLowerCase();
  const searchTerm = _.trim(term, ': ').toLowerCase();

  if (option.exactMatch) {
    return searchTerm.length > optionTerm.length && _.startsWith(searchTerm, optionTerm);
  } else {
    return false;
  }
};

const backSearchTerm = searchTerm => {
  const lastIdx = _.trim(searchTerm, ' :').lastIndexOf(':');
  if (lastIdx > 0) {
    return `${searchTerm.slice(0, lastIdx - 1)} : `;
  } else {
    return '';
  }
};


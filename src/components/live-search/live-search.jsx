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
      }
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
        selectedIndex = Math.min(selectedIndex + 1, visibleSearchOptions.length - 1);
        break;
      case 38:
        // Up arrow
        event.preventDefault(); 
        selectedIndex = Math.max(selectedIndex - 1, -1);
        break;
      case 13:
        // Enter
        event.preventDefault();
        if (selectedIndex > -1) {
          this.updateSearch(visibleSearchOptions[selectedIndex].term);
          return;
        }
        break;
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

  render() {
    return (
      <div className="fc-col-md-1-1 fc-live-search">
        <div className="fc-col-md-1-1 fc-search-bar">
          <form>
            <div className="fc-search-input-container">
              <div className="fc-search-input-wrapper fc-form-field">
                <input 
                  className="fc-search-input-field"
                  type="text" 
                  placeholder="Add another filter or keyword search"
                  onChange={this.onChange}
                  onKeyDown={this.keyDown}
                  onFocus={this.inputFocus}
                  value={this.state.value}
                />
              </div>
              <div className="fc-search-icon-wrapper">
                <i className="icon-search"></i>
              </div>
            </div>
            <div className="fc-search-btn-container">
              <button className="fc-btn fc-btn-search">Save Search</button>
            </div>
          </form>
        </div>
        { this.state.queryBuilder.show &&
          <QueryBuilder 
            selectedIndex={this.state.queryBuilder.selectedIndex} 
            searchOptions={this.state.queryBuilder.visibleSearchOptions}
            onGoBack={this.goBack} /> }
      </div>
    );
  }
}

const flattenOptions = (options, term = '') => {
  const allOptions = options.map(option => {
    const termPrefix = _.isEmpty(term) ? '' : `${term} `;
    let returnOptions = [{
      term: `${termPrefix}${option.term} :`,
      type: option.type,
      suggestions: option.suggestions
    }];
    
    if (!_.isEmpty(option.options)) {
      returnOptions = _.union(returnOptions, flattenOptions(option.options, returnOptions[0].term));
      returnOptions = _.flatten(returnOptions);
    }    

    return returnOptions;
  });

  return _.flatten(allOptions);
}

const visibleOptions = (optionsList, term = '') => {
  const searchTerm = term.trim().toLowerCase();
  const colonsInSearchTerm = (searchTerm.match(/:/g) || []).length;
  let visibleSearchOptions = _.filter(optionsList, option => {
    const colonsInOption = (option.term.match(/:/g) || []).length;
    return _.startsWith(option.term.toLowerCase(), searchTerm) && (colonsInSearchTerm + 1 == colonsInOption);
  }); 

  return visibleSearchOptions;
};

const backSearchTerm = searchTerm => {
  const lastIdx = _.trim(searchTerm, ' :').lastIndexOf(':');
  if (lastIdx > 0) {
    return searchTerm.slice(0, lastIdx - 1);
  } else {
    return '';
  }
};


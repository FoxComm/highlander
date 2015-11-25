import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import SearchOption from './search-option';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      value: '',
      showQueryBuilder: false,
      selectedIndex: -1,
      visibleSearchOptions: [],
      searches: []
    };
  }

  static propTypes = {
    searchOptions: PropTypes.array
  };


  @autobind
  onChange({target}) {
    this.updateSearch(target.value);
  } 

  @autobind
  inputFocus() {
    this.updateSearch(this.state.value);
  }

  @autobind
  blur() {
    this.setState({
      ...this.state,
      showQueryBuilder: false
    });
  }

  @autobind
  keyDown(event) {
    console.log(event.keyCode);
    let selectedIndex = this.state.selectedIndex;
    const visibleSearchOptions = this.state.visibleSearchOptions;
    
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault(); 
        if (!this.state.showQueryBuilder) {
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
          if (readyToFilter(visibleSearchOptions[0], this.state.value)) {
            let newSearches = this.state.searches;
            newSearches.push(this.state.value);

            this.setState({
              ...this.state,
              value: '',
              searches: newSearches,
              showQueryBuilder: false
            });
          }
        } else {
          const newSearchTerm = `${visibleSearchOptions[selectedIndex].display} : `;
          this.updateSearch(newSearchTerm);
        }
        return;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.value) && !_.isEmpty(this.state.searches)) {
          event.preventDefault();
          this.deleteSearchPill(this.state.searches.length - 1);
          return;
        }
         
      default:
        // Untracked action.
        return;
    };

    this.setState({
      ...this.state,
      selectedIndex: selectedIndex
    });
  }

  @autobind
  updateSearch(searchTerm) {
    const visibleSearchOptions = visibleOptions(this.props.searchOptions, searchTerm);
    const showQB = !_.isEmpty(visibleSearchOptions);

    this.setState({
      ...this.state,
      value: searchTerm,
      showQueryBuilder: showQB,
      selectedIndex: -1,
      visibleSearchOptions: visibleSearchOptions
    });
  }


  @autobind
  goBack() {
    this.updateSearch(backSearchTerm(this.state.value));
  }

  @autobind
  deleteSearchPill(idx) {
    this.setState({
      ...this.state,
      searches: _.without(this.state.searches, this.state.searches[idx])
    });
  }

  get searchPills() {
    return this.state.searches.map((search, idx) => {
      return (
        <div className='fc-live-search-pill' key={`search-${idx}`}>
          <i className='icon-search fc-live-search-pill-icon'></i>
          {search}
          <a className='fc-live-search-pill-close' onClick={() => this.deleteSearchPill(idx)}>
            &times;
          </a>
        </div>
      );
    });
  }

  get searchOptionsMenu() {
    const searchOptions = this.state.visibleSearchOptions.map((option, idx) => {
      const key = `${option.term}-${idx}`;
      const klass = classNames({
        'is-active': this.state.selectedIndex == idx,
        'is-first': idx == 0
      });

      return (
        <SearchOption 
          className={klass}
          key={key}
          option={option}
          onClick={() => this.updateSearch(option.display)} />
      );
    });

    return (
      <Menu>
        {searchOptions}
        <MenuItem className='fc-search-option-back' onClick={this.goBack}>
          Back
        </MenuItem>
      </Menu>
    );
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
                    onBlur={this.blur}
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
        { this.state.showQueryBuilder && this.searchOptionsMenu }
      </div>
    );
  }
}

/**
 * Implementation of that algorithm that determines what search options
 * should be shown in this control.
 * @param {array} optionsList The complete set of options that could be shown.
 * @param {string} term The search term to filter by.
 */
const visibleOptions = (optionsList, term, prefix = '') => {
  if (!_.isEmpty(prefix)) {
    prefix = `${prefix} : `;
  }

  const opts = _.transform(optionsList, (result, option) => {
    // Normalize the search terms
    const nSearchTerm = term.toLowerCase();
    const optionTerm = `${prefix}${option.term}`;
    const nOptionTerm = optionTerm.toLowerCase();

    if (nSearchTerm <= nOptionTerm) {
      if (_.startsWith(nOptionTerm, nSearchTerm)) {
        result.push({...option, display: optionTerm});
      }
    } else if (_.startsWith(nSearchTerm, nOptionTerm)) {
      if (!_.isEmpty(option.options)) {
        const nestedOptions = visibleOptions(option.options, term, optionTerm);
        _.forEach(nestedOptions, option => {
          result.push({
            ...option,
            display: `${prefix}${option.display}`
          });
        });
      } else if (option.type == "enum") {
        _.forEach(option.suggestions, suggestion => {
          result.push({
            ...option,
            display: optionTerm,
            action: suggestion
          });
        });
      } else {
        result.push({...option, display: optionTerm});
      }
    }
  });

  return opts;
};

/**
 * Check to see if the contents of the search box are ready to be turned into a filter.
 * @param {object} option The currently matched option.
 * @param {string} term The search team in the filter field.
 * @return {boolean} True if ready, false otherwise.
 */
const readyToFilter = (option, term) => {
  const optionTerm = option.display.toLowerCase();
  const searchTerm = _.trim(term, ': ').toLowerCase();

  if (_.isEmpty(option.options)) {
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


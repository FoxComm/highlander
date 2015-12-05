import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import SearchOption from './search-option';
import PilledSearch from '../pilled-search/pilled-search';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    updateSearch: PropTypes.func.isRequired,
    submitFilter: PropTypes.func.isRequired,
    searchOptions: PropTypes.array,
    state: PropTypes.object.isRequired
  };

  @autobind
  renderSearchOption(option, searchTerm, idx, selectionIndex) {
    if (_.startsWith(option.displayTerm, searchTerm)) {
      const key = `search-option-${idx}`;
      const klass = classNames({
        'is-active': selectionIndex == idx,
        'is-first': idx == 0,
        'is-last': idx == selectionIndex - 1
      });

      return ( 
        <SearchOption
          className={klass}
          key={key}  
          option={option} 
          clickAction={this.props.submitSearch} />
      );
    }
  }

  render() {
    return (
      <PilledSearch
        className="fc-live-search fc-col-md-1-1"
        placeholder="Add another filter or keyword search"
        searchButton={<button className="fc-btn">Save Search</button>}

        searchValue={this.props.state.displayValue}
        onSubmit={this.props.submitFilter}
        pills={this.props.state.searches}
        deletePill={this.props.deleteSearchFilter}
        searchOptions={this.props.state.currentOptions}
        renderSearchOption={this.renderSearchOption}
      />
    );
  }
}

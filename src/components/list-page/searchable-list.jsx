
import React, { PropTypes } from 'react';
import _ from 'lodash';

import LiveSearch from '../live-search/live-search';
import MultiSelectTable from '../table/multi-select-table';

export default class SearchableList extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      sortOrder: 'asc',
      sortBy: null,
    };
  }

  static propTypes = {
    emptyResultMessage: PropTypes.string,
    list: PropTypes.object,
    renderRow: PropTypes.func.isRequired,
    tableColumns: PropTypes.array.isRequired,
    searchActions: PropTypes.shape({
      addSearchFilter: PropTypes.func.isRequired,
      deleteSearch: PropTypes.func.isRequired,
      fetch: PropTypes.func.isRequired,
      fetchSearches: PropTypes.func.isRequired,
      saveSearch: PropTypes.func.isRequired,
      selectSearch: PropTypes.func.isRequired,
      submitFilters: PropTypes.func.isRequired,
      updateSearch: PropTypes.func.isRequired
    }).isRequired,
    searchOptions: PropTypes.shape({
      singleSearch: PropTypes.bool,
      initialFilters: PropTypes.array,
    }),
    title: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  };

  static defaultProps = {
    emptyResultMessage: 'No results found.',
    searchOptions: {
      singleSearch: false,
      initialFilters: [],
    },
  };

  render() {
    const props = this.props;

    const selectedSearch = props.list.selectedSearch;
    const results = props.list.savedSearches[selectedSearch].results;

    const filter = searchTerms => props.searchActions.addSearchFilter(props.url, searchTerms);
    const selectSearch = idx => props.searchActions.selectSearch(props.url, idx);

    const setState = params => {
      if (params.sortBy) {
        const sort = {};
        const newState = {sortBy: params.sortBy};

        let sortOrder = this.state.sortOrder;

        if (params.sortBy == this.state.sortBy) {
          sortOrder = newState['sortOrder'] = sortOrder == 'asc' ? 'desc' : 'asc';
        }

        sort[params.sortBy] = {order: sortOrder};
        props.searchActions.fetch(props.url, {sort: [sort]});
        this.setState(newState);
      }
    };

    return (
      <LiveSearch
        fetchSearches={props.searchActions.fetchSearches}
        saveSearch={props.searchActions.saveSearch}
        {...props.searchOptions}
        selectSavedSearch={selectSearch}
        submitFilters={filter}
        searches={props.list}
        deleteSearch={props.searchActions.deleteSearch}
        updateSearch={props.searchActions.updateSearch}
      >
        <MultiSelectTable
          columns={props.tableColumns}
          data={results}
          renderRow={props.renderRow}
          setState={setState}
          showEmptyMessage={true}
          emptyMessage={props.emptyResultMessage} />
      </LiveSearch>
    );
  };
}

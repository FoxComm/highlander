
import React, { PropTypes } from 'react';
import _ from 'lodash';

import LiveSearch from '../live-search/live-search';
import MultiSelectTable from '../table/multi-select-table';

export default class SearchableList extends React.Component {

  static propTypes = {
    emptyResultMessage: PropTypes.string,
    list: PropTypes.object,
    renderRow: PropTypes.func.isRequired,
    tableColumns: PropTypes.array.isRequired,
    searchActions: PropTypes.shape({
      addSearchFilters: PropTypes.func.isRequired,
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
    }),
    processRows: PropTypes.func,
    noGutter: PropTypes.bool,
  };

  static defaultProps = {
    emptyResultMessage: 'No results found.',
    searchOptions: {
      singleSearch: false,
    },
    noGutter: false
  };

  render() {
    const props = this.props;

    const selectedSearch = props.list.selectedSearch;
    const results = props.list.savedSearches[selectedSearch].results;

    return (
      <LiveSearch
        fetchSearches={props.searchActions.fetchSearches}
        saveSearch={props.searchActions.saveSearch}
        {...props.searchOptions}
        selectSavedSearch={props.searchActions.selectSearch}
        submitFilters={props.searchActions.addSearchFilters}
        searches={props.list}
        deleteSearch={props.searchActions.deleteSearch}
        updateSearch={props.searchActions.updateSearch}
        noGutter={props.noGutter} >
        <MultiSelectTable
          columns={props.tableColumns}
          data={results}
          renderRow={props.renderRow}
          processRows={props.processRows}
          setState={props.searchActions.updateStateAndFetch}
          emptyMessage={props.emptyResultMessage} />
      </LiveSearch>
    );
  };
}

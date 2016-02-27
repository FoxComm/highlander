
import React, { PropTypes } from 'react';

import LiveSearchAdapter from '../live-search/live-search-adapter';
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
      submitPhrase: PropTypes.func.isRequired,
      updateSearch: PropTypes.func.isRequired
    }).isRequired,
    searchOptions: PropTypes.shape({
      singleSearch: PropTypes.bool,
    }),
    processRows: PropTypes.func,
    noGutter: PropTypes.bool,
    bulkActions: PropTypes.arrayOf(PropTypes.array),
    predicate: PropTypes.func,
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

    const results = props.list.currentSearch().results;

    return (
      <LiveSearchAdapter
        {...props.searchOptions}
        searchActions={props.searchActions}
        searches={props.list}
        noGutter={props.noGutter}
        >
        <MultiSelectTable
          columns={props.tableColumns}
          data={results}
          renderRow={props.renderRow}
          processRows={props.processRows}
          setState={props.searchActions.updateStateAndFetch}
          bulkActions={props.bulkActions}
          predicate={props.predicate}
          showEmptyMessage={true}
          isLoading={results.isFetching}
          emptyMessage={props.emptyResultMessage} />
      </LiveSearchAdapter>
    );
  };
}

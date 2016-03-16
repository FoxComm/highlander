import React, { PropTypes } from 'react';

import LiveSearchAdapter from '../live-search/live-search-adapter';
import MultiSelectTable from '../table/multi-select-table';

export default class SearchableList extends React.Component {

  static propTypes = {
    emptyResultMessage: PropTypes.string,
    errorMessage: PropTypes.string,
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
      updateSearch: PropTypes.func.isRequired,
      updateStateAndFetch: PropTypes.func.isRequired,
      suggestAssociations: PropTypes.func.isRequired,
      fetchAssociations: PropTypes.func.isRequired,
      associateSearch: PropTypes.func.isRequired,
      dissociateSearch: PropTypes.func.isRequired,
      selectItem: PropTypes.func.isRequired,
      deselectItem: PropTypes.func.isRequired,
      setTerm: PropTypes.func.isRequired,
    }).isRequired,
    searchOptions: PropTypes.shape({
      singleSearch: PropTypes.bool,
    }),
    processRows: PropTypes.func,
    noGutter: PropTypes.bool,
    bulkActions: PropTypes.arrayOf(PropTypes.array),
    predicate: PropTypes.func,
    toggleColumnPresent: PropTypes.bool,
  };

  static defaultProps = {
    emptyResultMessage: 'No results found.',
    errorMessage: 'An error occurred. Try again later.',
    searchOptions: {
      singleSearch: false,
    },
    noGutter: false,
    toggleColumnPresent: true
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
          toggleColumnPresent={props.toggleColumnPresent}
          isLoading={results.isFetching}
          failed={results.failed}
          emptyMessage={props.emptyResultMessage}
          errorMessage={props.errorMessage}/>
      </LiveSearchAdapter>
    );
  };
}

import React, { PropTypes } from 'react';

import LiveSearchAdapter from '../live-search/live-search-adapter';
import MultiSelectTable from '../table/multi-select-table';

export default class SelectableSearchList extends React.Component {

  static propTypes = {
    entity: PropTypes.string.isRequired,
    emptyMessage: PropTypes.string,
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
      fetchAssociations: PropTypes.func.isRequired,
      associateSearch: PropTypes.func.isRequired,
      dissociateSearch: PropTypes.func.isRequired,
      refresh: PropTypes.func.isRequired,
    }).isRequired,
    searchOptions: PropTypes.shape({
      placeholder: PropTypes.string,
      singleSearch: PropTypes.bool,
    }),
    processRows: PropTypes.func,
    noGutter: PropTypes.bool,
    bulkActions: PropTypes.arrayOf(PropTypes.array),
    predicate: PropTypes.func,
    hasActionsColumn: PropTypes.bool,
    autoRefresh: PropTypes.bool,
    tbodyId: PropTypes.string,
  };

  static defaultProps = {
    emptyMessage: 'No results found.',
    errorMessage: 'An error occurred. Try again later.',
    searchOptions: {
      singleSearch: false,
    },
    noGutter: false,
    hasActionsColumn: true,
    autoRefresh: false,
  };

  componentDidMount() {
    if (this.props.autoRefresh) {
      this.autoFetcher = this.autoFetcher || setInterval(this.props.searchActions.refresh, 1500);
    }
  }

  componentWillUnmount() {
    clearInterval(this.autoFetcher);
  }

  render() {
    const props = this.props;

    const results = props.list.currentSearch().results;

    return (
      <LiveSearchAdapter
        {...props.searchOptions}
        entity={props.entity}
        searchActions={props.searchActions}
        searches={props.list}
        noGutter={props.noGutter}
      >
        <MultiSelectTable
          tbodyId={props.tbodyId}
          columns={props.tableColumns}
          data={results}
          renderRow={props.renderRow}
          processRows={props.processRows}
          setState={props.searchActions.updateStateAndFetch}
          bulkActions={props.bulkActions}
          predicate={props.predicate}
          hasActionsColumn={props.hasActionsColumn}
          isLoading={results.isFetching}
          failed={results.failed}
          emptyMessage={props.emptyMessage}
          errorMessage={props.errorMessage}
          key={props.list.currentSearch().title} />
      </LiveSearchAdapter>
    );
  };
}

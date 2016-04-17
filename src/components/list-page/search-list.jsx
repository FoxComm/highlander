import React, { PropTypes } from 'react';

import LiveSearchAdapter from '../live-search/live-search-adapter';
import TableView from '../table/tableview.jsx';

export default class SearchList extends React.Component {

  static propTypes = {
    emptyMessage: PropTypes.string,
    errorMessage: PropTypes.string,
    list: PropTypes.object,
    renderRow: PropTypes.func.isRequired,
    tableColumns: PropTypes.array.isRequired,
    searchActions: PropTypes.shape({
      addSearchFilters: PropTypes.func.isRequired,
      addSearchPhrase: PropTypes.func.isRequired,
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
    noGutter: PropTypes.bool,
    autoRefresh: PropTypes.bool,
  };

  static defaultProps = {
    emptyMessage: 'No results found.',
    errorMessage: 'An error occurred. Try again later.',
    searchOptions: {
      singleSearch: false,
    },
    noGutter: false,
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
        searchActions={props.searchActions}
        searches={props.list}
        noGutter={props.noGutter}
      >
        <TableView
          {...props}
          data={results}
          columns={props.tableColumns}
          isLoading={results.isFetching}
          failed={results.failed} />
      </LiveSearchAdapter>
    );
  };
}

import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import localStorage from 'localStorage';

import LiveSearchAdapter from '../live-search/live-search-adapter';
import TableView from '../table/tableview';

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
      updateSearch: PropTypes.func.isRequired,
      refresh: PropTypes.func.isRequired,
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

  state = {
    columns: this.selectedColumns
  };

  componentDidMount() {
    if (this.props.autoRefresh) {
      this.autoFetcher = this.autoFetcher || setInterval(this.props.searchActions.refresh, 1500);
    }
  }

  componentWillUnmount() {
    clearInterval(this.autoFetcher);
  }

  @autobind
  setColumnSelected(columns) {
    this.setState({ columns });
  }

  get tableIdentifier() {
    if (!this.props.identifier) {
      return this.props.tableColumns.map(item => {
        return item.text;
      }).toString();
    }
    return this.props.identifier;
  }

  get selectedColumns() {
    const tableName = this.tableIdentifier;
    const savedColumns = localStorage.getItem('columns');

    if (!savedColumns) {
      return this.props.tableColumns;
    }

    const columns = JSON.parse(savedColumns);

    if (!columns[tableName]) {
      return this.props.tableColumns;
    }

    return _.filter(columns[tableName], { isVisible: true });
  }

  @autobind
  renderRow(row, index, isNew) {
    return this.props.renderRow(row, index, this.state.columns, { isNew });
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
          dataTable={true}
          columns={this.state.columns}
          renderRow={this.renderRow}
          selectableColumns={props.tableColumns}
          setColumnSelected={this.setColumnSelected}
          tableIdentifier={this.tableIdentifier}
          isLoading={results.isFetching}
          failed={results.failed}
        />
      </LiveSearchAdapter>
    );
  };
}

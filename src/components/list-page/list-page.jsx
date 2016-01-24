import React, { PropTypes } from 'react';

import ListPageContainer from './list-page-container';
import SearchableList from './searchable-list';

const ListPage = props => {
  return (
    <ListPageContainer {...props}>
      <SearchableList {...props} />
    </ListPageContainer>
  );
};

ListPage.propTypes = {
  ...SearchableList.propTypes,
  ...ListPageContainer.propTypes,
};

<<<<<<< HEAD
export default ListPage;
=======
  static propTypes = {
    addTitle: PropTypes.string,
    emptyResultMessage: PropTypes.string,
    handleAddAction: PropTypes.func,
    list: PropTypes.object,
    navLinks: PropTypes.array,
    renderRow: PropTypes.func.isRequired,
    tableColumns: PropTypes.array.isRequired,
    searchActions: PropTypes.shape({
      addSearchFilter: PropTypes.func.isRequired,
      fetch: PropTypes.func.isRequired,
      fetchSearches: PropTypes.func.isRequired,
      saveSearch: PropTypes.func.isRequired,
      selectSearch: PropTypes.func.isRequired,
      submitFilters: PropTypes.func.isRequired,
      updateSearch: PropTypes.func.isRequired
    }).isRequired,
    title: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  };

  static defaultProps = {
    addTitle: '',
    emptyResultMessage: 'No results found.',
    handleAddAction: _.noop,
    navLinks: []
  };

  render() {
    const props = this.props;
    const localNav = !_.isEmpty(props.navLinks) ? (
      <LocalNav>
        {props.navLinks.map(l => <IndexLink key={l.to + l.title} to={l.to}>{l.title}</IndexLink>)}
      </LocalNav>
    ) : null;

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
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle
            title={props.title}
            subtitle={results.total}
            onAddClick={props.handleAddAction}
            addTitle={props.addTitle} />
          {localNav}
        </div>
        <LiveSearch
          fetchSearches={props.searchActions.fetchSearches}
          saveSearch={props.searchActions.saveSearch}
          selectSavedSearch={selectSearch}
          submitFilters={filter}
          searches={props.list}
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
      </div>
    );
  };
}
>>>>>>> After updating search name, update on the API


import React, { PropTypes } from 'react';
import LiveSearch from './live-search';

const LiveSearchAdapter = props => {
  const {searchActions, ...rest} = props;

  return (
    <LiveSearch
      {...rest}
      fetchSearches={searchActions.fetchSearches}
      saveSearch={searchActions.saveSearch}
      selectSavedSearch={searchActions.selectSearch}
      submitFilters={searchActions.addSearchFilters}
      submitPhrase={searchActions.addSearchPhrase}
      deleteSearch={searchActions.deleteSearch}
      updateSearch={searchActions.updateSearch}
      suggestAssociations={searchActions.suggestAssociations}
      fetchAssociations={searchActions.fetchAssociations}
      associateSearch={searchActions.associateSearch}
      dissociateSearch={searchActions.dissociateSearch}
      selectItem={searchActions.selectItem}
      deselectItem={searchActions.deselectItem}
      setTerm={searchActions.setTerm}
    >
      {props.children}
    </LiveSearch>
  );
};

LiveSearchAdapter.propTypes = {
  searchActions: PropTypes.shape({
    fetchSearches: PropTypes.func,
    saveSearch: PropTypes.func,
    selectSearch: PropTypes.func,
    addSearchFilters: PropTypes.func,
    addSearchPhrase: PropTypes.func,
    deleteSearch: PropTypes.func,
    updateSearch: PropTypes.func,
    suggestAssociations: PropTypes.func,
    fetchAssociations: PropTypes.func,
    associateSearch: PropTypes.func,
    dissociateSearch: PropTypes.func,
    selectItem: PropTypes.func,
    deselectItem: PropTypes.func,
    setTerm: PropTypes.func,
  }),
  searches: PropTypes.object,
};

export default LiveSearchAdapter;

import React, { Component } from 'react';
import SearchResult from './SearchResult';

import './SearchResults.css';

class SearchResults extends Component {
  render() {
    const { results } = this.props;

    let content = null;

    if (results && results.length === 0)
      content = <div className="empty-message">No results returned.</div>;
    else if (results && results.length > 0)
      content = results.map(r => <SearchResult result={r} />);

    return <div className="search-results">{content}</div>;
  }
}

export default SearchResults;
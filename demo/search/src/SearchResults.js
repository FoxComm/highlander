import React, { Component } from 'react';
import SearchResult from './SearchResult';

import './SearchResults.css';

class SearchResults extends Component {
  render() {
    const { results } = this.props;
    
    const content = results.length === 0
      ? <div className="empty-message">No results returned.</div>
      : results.map(r => <SearchResult result={r} />);

    return <div className="search-results">{content}</div>;
  }
}

export default SearchResults;
import React, { Component } from 'react';

class SearchResults extends Component {
  render() {
    const { results } = this.props;
    if (results.length == 0) {
      return <div>No results returned.</div>;
    }

    return (
      <div>
        Some results returned.
      </div>
    );
  }
}

export default SearchResults;
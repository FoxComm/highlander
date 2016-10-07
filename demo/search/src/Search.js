import React, { Component } from 'react';

import SearchResults from './SearchResults';

class Search extends Component {
  state = { search: '', results: [] };

  handleKeyDown = (event) => {
    if (event.keyCode !== 13) {
      return;
    }

    event.preventDefault();
    this.setState({ results: [1] });
  };

  handleSearchUpdate = ({target}) => {
    this.setState({ search: target.value });
  };

  render() {
    const { search, results } = this.state;

    return (
      <div>
        <input type="text" 
               onChange={this.handleSearchUpdate.bind(this)}
               onKeyDown={this.handleKeyDown.bind(this)}
               value={search} />
        <SearchResults results={results} />
      </div>
    );
  }
}

export default Search;
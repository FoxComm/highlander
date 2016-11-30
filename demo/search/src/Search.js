import React, { Component } from 'react';

import SearchResults from './SearchResults';

import './Search.css';

const createQuery = term => {
  const query = {
    query: {
      bool: {
        filter: [
          {
            missing: {
              field: "archivedAt",
            },
          },
        ],
        must: [
          {
            match: {
              '_all': {
                query: term,
                analyzer: 'standard',
                operator: 'and',
              },
            },
          },
        ],
      },
    },
  };

  return query;
};

class Search extends Component {
  state = { search: '', results: null };

  handleKeyDown = (event) => {
    if (event.keyCode !== 13) {
      return;
    }

    event.preventDefault();

    const options = {
      method: 'post',
      body: JSON.stringify(createQuery(this.state.search)),
    };

    fetch('/api/search/public/products_catalog_view/_search', options)
      .then(response => response.json())
      .then(json => {
        const results = json.result;
        let products = [];
        for (let i = 0; i < results.length; i++) {
          const title = results[i].title;
          const image = results[i].albums.length > 0 && results[i].albums[0].images && results[i].albums[0].images.length > 0
            ? results[i].albums[0].images[0].src
            : '';
          const price = results[i].salePrice
            ? parseInt(results[i].salePrice, 10)
            : 0;

          products = [...products, { title, price, image }];
        }
         
	this.setState({ results: products });
      });
  };

  handleSearchUpdate = ({target}) => {
    this.setState({ search: target.value });
  };

  render() {
    const { search, results } = this.state;

    return (
      <div className="container">
        <input type="text" 
               className="search-input"
               onChange={this.handleSearchUpdate.bind(this)}
               onKeyDown={this.handleKeyDown.bind(this)}
               placeholder="Search"
               value={search} />
        <SearchResults results={results} />
      </div>
    );
  }
}

export default Search;

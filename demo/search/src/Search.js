import React, { Component } from 'react';

import SearchResults from './SearchResults';

import './Search.css';

const sampleProducts = [
  {
    title: 'Donkey',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg',
  },
  {
    title: 'Shark',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg',
  },
  {
    title: 'Fox',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg',
  },
  {
    title: 'Chicken',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Round_Readers_Top_Front.jpg',
  },
  {
    title: 'Wolf',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Emma_Top_Front.jpg',
  },
  {
    title: 'Duck',
    price: 999,
    image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Granger_Three_Quarter.jpg',
  },
]

class Search extends Component {
  state = { search: '', results: null };

  handleKeyDown = (event) => {
    if (event.keyCode !== 13) {
      return;
    }

    event.preventDefault();
    this.setState({ results: sampleProducts });
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
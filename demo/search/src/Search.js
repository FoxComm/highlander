import React, { Component } from 'react';
import superagent from 'superagent';

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

    const options = { method: 'get', mode: 'no-cors' };
    fetch('https://tgt.foxcommerce.com/api/search/public/products_catalog_view/_search', options)
      .then(response => response.json())
      .then(json => {
        console.log(json);
      });

    // url (required), options (optional)
    // fetch('https://tgt.foxcommerce.com/api/search/public/products_catalog_view/_search', {
    //   method: 'get',
    //   mode: 'no-cors',
    // }).then(res => {
    //   return res.json();
    //   // if (res.ok) {
    //   //   res.json().then(function(data) {
    //   //     console.log(data);
    //   //   });
    //   // } else {
    //   //   console.log("Looks like the response wasn't perfect, got status", res.status);
    //   // }
    // }).then(resJSON => {
    //   console.log(resJSON);
    // }).catch(function(err) {
    //   console.error('Error has occurred:');
    //   console.error(err);
    // });

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
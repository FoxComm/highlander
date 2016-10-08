import React, { Component } from 'react';
import './SearchResult.css';

class SearchResult extends Component {
  render() {
    const { image, price, title } = this.props.result;
    const formattedPrice = `$${price / 100}`;

    return (
      <div className="search-result">
        <div>
          <img className="search-image" src={image} alt={title} />
        </div>
        <div>{title}</div>
        <div>{formattedPrice}</div>
      </div>
    );
  }
}

export default SearchResult;
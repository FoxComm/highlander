import React, { Component } from 'react';

class SearchResult extends Component {
  render() {
    const { image, price, title } = this.props;
    return (
      <div>
        <div>{image}</div>
        <div>{title}</div>
        <div>{price}</div>
      </div>
    );
  }
}
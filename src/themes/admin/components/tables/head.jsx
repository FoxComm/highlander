'use strict';

import React from 'react';

export default class TableHead extends React.Component {
  render() {
    let createColumn = function(column, idx) {
      return <th key={`${idx}-${column.field}`}>{column.text}</th>;
    };
    return <thead><tr>{this.props.columns.map(createColumn)}</tr></thead>;
  }
}

TableHead.propTypes = {
  columns: React.PropTypes.array
};

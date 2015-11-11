import React, { PropTypes } from 'react';
import classNames from 'classnames';

export default class TableHead extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  // onHeaderItemClick(field, event) {
  //   event.preventDefault();
  //   this.setState({
  //     sortingField: field,
  //     sortingOrder: (field === this.state.sortingField) ? !this.state.sortingOrder : true
  //   }, () => {
  //     if (this.props.setSorting) {
  //       this.props.setSorting(this.state.sortingField, this.state.sortingOrder);
  //     }
  //   });
  // }

  render() {
    let createColumn = (column, idx) => {
      // TODO: Re-enable sorting functionality
      // let classnames = classNames({
      //   'fc-table-th': true,
      //   'sorting': this.props.setSorting,
      //   'sorting-desc': this.props.setSorting && (this.state.sortingField === column.field) && this.state.sortingOrder,
      //   'sorting-asc': this.props.setSorting && (this.state.sortingField === column.field) && !this.state.sortingOrder
      // });
      // return (
      //   <th className={classnames} key={`${idx}-${column.field}`}
      //       onClick={this.onHeaderItemClick.bind(this, column.field)}>
      //     {column.text}
      //   </th>
      // );

      return(
        <th key={`${idx}-${column.field}`}>
          {column.text}
        </th>
      );
    };
    return <thead><tr>{this.props.columns.map(createColumn)}</tr></thead>;
  }
}

TableHead.propTypes = {
  columns: PropTypes.array
};

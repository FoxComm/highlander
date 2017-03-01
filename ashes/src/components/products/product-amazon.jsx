/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as productActions from 'modules/products/details';

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(productActions, dispatch),
  };
}

function mapStateToProps(state) {
  return {
    state
  };
}

class ProductAmazon extends Component {

  componentDidMount() {
    console.log('this.props', this.props);
    this.props.actions.clearFetchErrors();
    this.props.actions.fetchSchema('product');
    this.fetchEntity();
  }

  render() {
    return <div>Amazon</div>;
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductAmazon);

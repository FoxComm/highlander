import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import * as actions from './actions';

const mapStateToProps = (state) => {
  return {
    count: state.aaa.count,
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class SomeComponent extends Component {
  @autobind
  addAction() {
    transitionTo('promotion-coupon-new', {promotionId: this.props.object.id});
  };

  render() {
    return (
      <div>
        {this.props.count}
        <button onClick={this.props.actions.addCount}>+</button>
      </div>
    );
  }
};

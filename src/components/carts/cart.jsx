// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

// components
import { PageTitle } from 'components/section-title';
import SubNav from './sub-nav';
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as cartActions from 'modules/carts/details';

const refNum = props => {
  return props.params.cart;
};

const mapStateToProps = (state) => {
  return {
    details: state.carts.details,
    isFetching: _.get(state.asyncActions, 'fetchCart.inProgress', false),
  };
};

const mapDispatchToProps = { ...cartActions };

@connect(mapStateToProps, mapDispatchToProps)
export default class Cart extends Component {
  componentDidMount() {
    this.props.fetchCart(this.refNum);
  }

  componentWillReceiveProps(nextProps) {
    if (this.refNum != refNum(nextProps)) {
      this.props.fetchCart(refNum(nextProps));
    }
  }

  get refNum() {
    return refNum(this.props);
  }

  get cart() {
    return this.props.details.cart;
  }

  get details() {
    const details = React.cloneElement(this.props.children, { ...this.props, entity: this.cart });
    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          {details}
        </div>
      </div>
    );
  }

  get subNav() {
    return <SubNav cart={this.cart} />;
  }

  render() {
    const cart = this.cart;
    const className = 'fc-order fc-cart';

    if (this.props.isFetching || _.isEmpty(cart)) {
      return <div className={className}><WaitAnimation /></div>;
    }

    const title = `Cart ${this.refNum}`;

    return (
      <div className={className}>
        <PageTitle title={title} />
        <div>
          {this.subNav}
          {this.details}
        </div>
      </div>
    );
  }
}

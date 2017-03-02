// @flow
import React, { Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

// components
import { PageTitle } from 'components/section-title';
import SubNav from './sub-nav';
import WaitAnimation from 'components/common/wait-animation';
import Error from 'components/errors/error';

// redux
import * as cartActions from 'modules/carts/details';

// types
import type { TCart } from 'paragons/order';

type RouteParams = {
  cart: string,
}

type CartDetails = {
  cart?: TCart,
}

type Props = {
  fetchCart: (refNum: string) => Promise<*>,
  clearFetchCartErrors: () => void,
  params: RouteParams,
  details: CartDetails,
  children: Element<*>,
  isFetching: boolean|null,
  fetchError: ?Object,
}

const refNum = (props: Props): string => {
  return props.params.cart;
};

const mapStateToProps = (state) => {
  return {
    details: state.carts.details,
    isFetching: _.get(state.asyncActions, 'fetchCart.inProgress', null),
    fetchError: _.get(state.asyncActions, 'fetchCart.err', null),
  };
};

const mapDispatchToProps = { ...cartActions };

class Cart extends Component {
  props: Props;

  componentDidMount() {
    this.props.clearFetchCartErrors();
    this.props.fetchCart(this.refNum);
  }

  componentWillReceiveProps(nextProps) {
    if (this.refNum != refNum(nextProps)) {
      this.props.fetchCart(refNum(nextProps));
    }
  }

  get refNum(): string {
    return refNum(this.props);
  }

  get cart(): ?TCart {
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
    if (this.cart) {
      return <SubNav cart={this.cart} />;
    }
  }

  render() {
    const cart = this.cart;
    const className = 'fc-order fc-cart';

    if (this.props.isFetching !== false) {
      return <div className={className}><WaitAnimation /></div>;
    }

    if (_.isEmpty(this.cart)) {
      return <Error err={this.props.fetchError} notFound={`There is no cart with reference number ${this.refNum}`} />;
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

export default connect(mapStateToProps, mapDispatchToProps)(Cart);

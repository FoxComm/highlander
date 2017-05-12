/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// i18n
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// modules
import { fetchRelatedProducts, clearRelatedProducts } from 'modules/cross-sell';

// components
import RelatedProductsList,
  { LoadingBehaviors } from 'components/related-products-list/related-products-list';

// types
import type { RelatedProductResponse } from 'modules/cross-sell';
import type { RoutesParams } from '../../../types';

type Actions = {
  fetchRelatedProducts: Function,
  clearRelatedProducts: Function,
};

type Props = Localized & RoutesParams & {
  id: number,
  actions: Actions,
  relatedProducts: ?RelatedProductResponse,
};

type State = {
  error?: any,
};

const mapStateToProps = (state) => {
  const relatedProducts = state.crossSell.relatedProducts;

  return {
    relatedProducts,
    isRelatedProductsLoading: _.get(state.asyncActions, ['relatedProducts', 'inProgress'], false),
  };
};

const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators({
    fetchRelatedProducts,
    clearRelatedProducts,
  }, dispatch),
});

class RelatedProducts extends Component {
  props: Props;

  state: State = {
  };

  componentDidMount() {
    const { id, isRelatedProductsLoading, actions } = this.props;
    if (!isRelatedProductsLoading) {
      actions.fetchRelatedProducts(id, 1).catch(_.noop);
    }
  }

  render(): ?Element<any> {
    const { relatedProducts, isRelatedProductsLoading } = this.props;

    return (
      <RelatedProductsList
        title="You Might Also Like"
        list={relatedProducts.products}
        isLoading={isRelatedProductsLoading}
        loadingBehavior={LoadingBehaviors.ShowWrapper}
      />
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(RelatedProducts));

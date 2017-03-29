// @flow

// libs
import { get, flow, noop, sortedUniqBy } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import classNames from 'classnames';

// components
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import HierarchicalTaxonomyListWidget from '../taxons/hierarchical-taxonomy-widget';
import FlatTaxonomyListWidget from '../taxons/flat-taxonomy-widget';
import RoundedPill from 'components/rounded-pill/rounded-pill';
import { bindActionCreators } from 'redux';

// actions
import { fetchTaxonomyInternal as fetchTaxonomy } from 'modules/taxonomies/details';
import { deleteProductCurried as unlinkProduct } from 'modules/taxons/details/taxon';

// style
import styles from './taxonomy-widget.css';

type Props = {
  title: string,
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  unlinkState: AsyncState,
  fetchTaxonomy: (id: number | string) => Promise<*>,
  unlinkProduct: (taxonId: number | string) => Promise<*>,
  onChange: Function,
  linkedTaxonomy: Array<LinkedTaxonomy>
};

class TaxonomyWidget extends Component {
  props: Props;

  state = {
    isFocused: false,
    inputOpened: false
  };

  componentDidMount() {
    const { taxonomyId, context } = this.props;
    this.props.fetchTaxonomy(taxonomyId, context);
  }

  @autobind
  handleCloseClick(taxonId) {
    this.props.unlinkProduct(taxonId)
      .then(response => {
        this.props.onChange(response);
      });
  }

  @autobind
  handleAddButton() {
    this.setState({ inputOpened: !this.state.inputOpened });
  }

  get linkedTaxonomy() {
    const { linkedTaxonomy, unlinkState } = this.props;

    if (!linkedTaxonomy || !linkedTaxonomy.taxons) {
      return null;
    }

    // temporary hack for hierarchical taxonomies
    if (linkedTaxonomy.hierarchical) {
      const sortedTaxons = sortedUniqBy(taxons, (item) => item.id);

      return sortedTaxons.map((taxon) => {
        return (
          <RoundedPill
            text={taxon.attributes.name.v}
            onClose={this.handleCloseClick}
            value={taxon.id}
            styleName="pill"
            inProgress={unlinkState.inProgress}
            key={taxon.id}
          />
        );
      });
    }

    return linkedTaxonomy.taxons.map((taxon) => {
      return (
        <RoundedPill
          text={taxon.attributes.name.v}
          onClose={this.handleCloseClick}
          value={taxon.id}
          styleName="pill"
          inProgress={unlinkState.inProgress}
          key={taxon.id}
        />
      );
    });
  }

  get content() {
    const { taxonomy, fetchState } = this.props;

    if (!taxonomy || fetchState.inProgress && !fetchState.err) {
      return <WaitAnimation className={styles.waiting} />;
    }

    const opened = this.state.inputOpened;
    const inputClass = classNames(styles.input, { [styles.opened]: opened });

    return (
      <div>
        <div className={inputClass}>
        </div>
        {this.linkedTaxonomy}
      </div>
    );
  }

  render() {
    const iconClassName = this.state.inputOpened ? 'icon-close' : 'icon-add';

    return (
      <div styleName="root">
        <div styleName="header">
          <span styleName="title">
            {this.props.title}
          </span>
          <span styleName="button">
            <i className={iconClassName} onClick={this.handleAddButton} />
          </span>
        </div>
        {this.content}
      </div>
    );
  }
}

/*
 * Local redux store
 */

const reducer = createReducer({
  [fetchTaxonomy.succeeded]: (state, response) => ({ ...state, taxonomy: response }),
});

const mapState = state => ({
  taxonomy: state.taxonomy,
  fetchState: get(state.asyncActions, 'fetchTaxonomy', {}),
  unlinkState: get(state.asyncActions, 'taxonDeleteProduct', {}),
});

const mapActions = (dispatch, props) => ({
  fetchTaxonomy: bindActionCreators(fetchTaxonomy.perform, dispatch),
  unlinkProduct: bindActionCreators(unlinkProduct(props.productId, props.context), dispatch)
});

export default flow(
  connect(mapState, mapActions),
  makeLocalStore(addAsyncReducer(reducer), { taxonomy: null }),
)(TaxonomyWidget);

// @flow

// libs
import { get, flow, sortedUniqBy } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import classNames from 'classnames';

// components
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import HierarchicalTaxonomyListWidget from './taxons/hierarchical-taxonomy-widget';
import FlatTaxonomyListWidget from './taxons/flat-taxonomy-widget';
import RoundedPill from 'components/rounded-pill/rounded-pill';
import { bindActionCreators } from 'redux';

// actions
import { fetchTaxonomyInternal as fetchTaxonomy } from 'modules/taxonomies/details';
import { deleteProductCurried } from 'modules/taxons/details/taxon';

// style
import styles from './taxonomy-widget.css';

type Props = {
  title: string,
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  fetchTaxonomy: (id: number | string) => Promise<*>,
  deleteProductCurried: (taxonId: number | string) => Promise<*>,
  onChange: Function,
  addedTaxons: Array<Taxonomy>
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
    this.props.deleteProductCurried(taxonId)
      .then(response => {
        this.props.onChange(response);
      });
  }

  @autobind
  handleAddButton() {
    this.setState({ inputOpened: !this.state.inputOpened });
  }

  get addedTaxons() {
    const taxons = get(this.props.addedTaxons, '0.taxons');

    if (!taxons) {
      return null
    }

    // temporary hack for hierarchical taxonomies
    if (this.props.addedTaxons[0].hierarchical) {
      const sortedTaxons = sortedUniqBy(taxons, (item) => item.id);

      return sortedTaxons.map((taxon) => {
        return (
          <RoundedPill
            text={taxon.attributes.name.v}
            onClose={this.handleCloseClick}
            value={taxon.id}
            styleName="pill"
            key={taxon.id}
          />
        );
      });
    }

    return taxons.map((taxon) => {
        return (
          <RoundedPill
            text={taxon.attributes.name.v}
            onClose={this.handleCloseClick}
            value={taxon.id}
            styleName="pill"
            key={taxon.id}
          />
        );
    });
  }

  render() {
    const { taxonomy, fetchState, title } = this.props;

    if (!taxonomy || fetchState.inProgress && !fetchState.err) {
      return <div><WaitAnimation /></div>;
    }

    const opened = this.state.inputOpened;
    const inputClass = classNames(styles.input, { [styles.opened]: opened });
    const iconClassName = this.state.inputOpened ? 'icon-close' : 'icon-add';

    return (
      <div styleName="root">
        <div styleName="header">
          <span styleName="title">
            {title}
          </span>
          <span styleName="button">
            <i className={iconClassName} onClick={this.handleAddButton} />
          </span>
        </div>
        <div className={inputClass}>
          <input onChange={() => console.log('display search')} />
        </div>
        {this.addedTaxons}
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
});

const mapActions = (dispatch, props) => ({
  fetchTaxonomy: bindActionCreators(fetchTaxonomy.perform, dispatch),
  deleteProductCurried: bindActionCreators(deleteProductCurried(props.productId, props.context), dispatch)
});

export default flow(
  connect(mapState, mapActions),
  makeLocalStore(addAsyncReducer(reducer), { taxonomy: null }),
)(TaxonomyWidget);

// @flow

// libs
import { get, sortedUniqBy } from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';

// components
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import RoundedPill from 'components/rounded-pill/rounded-pill';
import { withTaxonomy } from '../hoc';

// actions
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
  unlinkProduct: (taxonId: number | string) => Promise<*>,
  onChange: Function,
  linkedTaxonomy: LinkedTaxonomy,
};

const getName = (obj: any) => get(obj, 'attributes.name.v');

class TaxonomyWidget extends Component {
  props: Props;

  state = {
    isFocused: false,
    inputOpened: false
  };

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
    const taxons = linkedTaxonomy.hierarchical ?
      sortedUniqBy(linkedTaxonomy.taxons, (item) => item.id) : linkedTaxonomy.taxons;

    return taxons.map((taxon: Taxon) => {
      return (
        <RoundedPill
          text={getName(taxon)}
          onClose={this.handleCloseClick}
          value={String(taxon.id)}
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
    const iconClassName = classNames({
      'icon-close': this.state.inputOpened,
      'icon-add': !this.state.inputOpened,
    });

    return (
      <div styleName="root">
        <div styleName="header">
          <span styleName="title">
            {this.props.title}
          </span>
        </div>
        {this.content}
      </div>
    );
  }
}

const mapState = state => ({
  unlinkState: get(state.asyncActions, 'taxonDeleteProduct', {}),
});

const mapActions = (dispatch, props) => ({
  unlinkProduct: bindActionCreators(unlinkProduct(props.productId, props.context), dispatch)
});

export default withTaxonomy({ showLoader: false, mapState, mapActions })(TaxonomyWidget);

// @flow

// libs
import { get, sortedUniqBy } from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';

// components
import Transition from 'react-addons-css-transition-group';
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import RoundedPill from 'components/rounded-pill/rounded-pill';
import { withTaxonomy } from '../hoc';
import TaxonomyDropdown from '../taxonomy-dropdown';

// actions
import { deleteProductCurried as unlinkProduct } from 'modules/taxons/details/taxon';
import { addProductCurried as linkProduct } from 'modules/taxons/details/taxon';

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
  linkState: AsyncState,
  unlinkProduct: (taxonId: number | string) => Promise<*>,
  linkProduct: (taxonId: number | string) => Promise<*>,
  onChange: Function,
  linkedTaxonomy: LinkedTaxonomy,
};

const getName = (obj: any) => get(obj, 'attributes.name.v');

class TaxonomyWidget extends Component {
  props: Props;

  state = {
    showInput: false,
  };


  @autobind
  onCloseClick(taxonId) {
    this.props.unlinkProduct(taxonId)
      .then(response => {
        this.props.onChange(response);
      });
  }

  @autobind
  onIconClick() {
    this.setState({ showInput: !this.state.showInput });
  }

  @autobind
  onTaxonClick(taxonId) {
    this.props.linkProduct(taxonId)
      .then(response => {
        this.props.onChange(response);
      });
  }

  get linkedTaxons() {
    const { linkedTaxonomy, unlinkState, linkState } = this.props;

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
          onClose={this.onCloseClick}
          value={String(taxon.id)}
          styleName="pill"
          inProgress={unlinkState.inProgress || linkState.inProgress}
          key={taxon.id}
        />
      );
    });
  }

  get dropdown() {
    const transitionProps = {
      component: 'div',
      transitionName: `dd-transition-show`,
      transitionEnterTimeout: 100,
      transitionLeaveTimeout: 100,
    };

    return (
      <Transition {...transitionProps}>
        {this.state.showInput &&
        <TaxonomyDropdown
          onTaxonClick={this.onTaxonClick}
          taxonomy={this.props.taxonomy}
        />}
      </Transition>
    );
  }

  get content() {
    const { taxonomy, fetchState } = this.props;

    if (!taxonomy || fetchState.inProgress && !fetchState.err) {
      return <WaitAnimation className={styles.waiting} />;
    }

    return [
      this.dropdown,
      this.linkedTaxons
    ];
  }

  render() {
    const cls = classNames(styles.taxonomies, {
      [styles._open]: this.state.showInput,
    });

    return (
      <div className={cls}>
        <div styleName="header">
          <span styleName="title">
            {this.props.title}
          </span>
          <span styleName="button">
            <i className="icon-add" onClick={this.onIconClick} />
          </span>
        </div>
        {this.content}
      </div>
    );
  }
}

const mapState = state => ({
  unlinkState: get(state.asyncActions, 'taxonDeleteProduct', {}),
  linkState: get(state.asyncActions, 'taxonAddProduct', {})
});

const mapActions = (dispatch, props) => ({
  unlinkProduct: bindActionCreators(unlinkProduct(props.productId, props.context), dispatch),
  linkProduct: bindActionCreators(linkProduct(props.productId, props.context), dispatch)
});

export default withTaxonomy({ showLoader: false, mapState, mapActions })(TaxonomyWidget);

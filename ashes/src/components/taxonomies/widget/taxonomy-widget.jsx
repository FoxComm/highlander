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

// helpers
import { transitionToLazy } from 'browserHistory';

// style
import styles from './taxonomy-widget.css';

type Props = {
  title: string,
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  unlinkProduct: (taxonId: number | string) => Promise<*>,
  linkProduct: (taxonId: number | string) => Promise<*>,
  onChange: Function,
  linkedTaxonomy: LinkedTaxonomy,
};

type State = {
  showInput: boolean,
  linkingId: ?number,
  unlinkingId: ?number,
}
const transitionProps = {
  component: 'div',
  transitionName: styles.dropdown,
  transitionEnterTimeout: 100,
  transitionLeaveTimeout: 100,
};

const pillsTransitionProps = {
  component: 'div',
  transitionName: styles.pill,
  transitionAppear: true,
  transitionAppearTimeout: 200,
  transitionEnterTimeout: 200,
  transitionLeaveTimeout: 200,
};

const getName = (obj: any) => get(obj, 'attributes.name.v');

class TaxonomyWidget extends Component {
  props: Props;

  state: State = {
    showInput: false,
    // using local state for async actions as it is used per item in the list
    linkingId: null,
    unlinkingId: null,
  };

  @autobind
  handleDeleteClick(taxonId) {
    this.setState({ unlinkingId: taxonId }, () =>
      this.props.unlinkProduct(taxonId)
        .then(this.props.onChange)
        .then(() => this.setState({ unlinkingId: null }))
    );
  }

  @autobind
  handleShowDropdownClick() {
    this.setState({ showInput: !this.state.showInput });
  }

  @autobind
  handleLinkClick(taxonId) {
    this.setState({ linkingId: taxonId }, () =>
      this.props.linkProduct(taxonId)
        .then(this.props.onChange)
        .then(() => this.setState({ linkingId: null }))
    );
  }

  get linkedTaxons() {
    const { taxonomy, linkedTaxonomy, context } = this.props;

    // temporary hack for hierarchical taxonomies
    const taxons = sortedUniqBy(get(linkedTaxonomy, 'taxons', []), ({ id }) => id);

    return (
      <Transition {...pillsTransitionProps} key="pills">
        {taxons.map((taxon: Taxon) => (
          <RoundedPill
            text={getName(taxon)}
            onClick={transitionToLazy('taxon-details', { context, taxonomyId: taxonomy.id, taxonId: taxon.id })}
            onClose={this.handleDeleteClick}
            value={taxon.id}
            className={styles.pill}
            inProgress={this.state.unlinkingId === taxon.id}
            key={taxon.id}
          />
        ))}
      </Transition>
    );
  }

  get dropdown() {
    return (
      <Transition {...transitionProps} key="dropdown">
        {this.state.showInput &&
        <TaxonomyDropdown
          onTaxonClick={this.handleLinkClick}
          taxonomy={this.props.taxonomy}
          linkedTaxonomy={this.props.linkedTaxonomy}
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
      this.linkedTaxons,
    ];
  }

  shouldComponentUpdate(nextProps: Props, nextState: State) {
    const lengthPath = 'linkedTaxonomy.taxons.length';

    const taxonsChanged = get(nextProps, lengthPath, 0) !== get(this.props, lengthPath);
    const taxonomyChanged = nextProps.taxonomy !== this.props.taxonomy;
    const localStateChanged = nextState !== this.state;

    return taxonomyChanged || taxonsChanged || localStateChanged;
  }

  render() {
    const cls = classNames(styles.taxonomies, {
      [styles._open]: this.state.showInput,
      [styles._loading]: this.state.linkingId,
    });

    return (
      <div className={cls}>
        <div className={styles.header}>
          <span>{this.props.title}</span>
          <button className={styles.button} onClick={this.handleShowDropdownClick}><span>&times;</span></button>
        </div>
        {this.content}
      </div>
    );
  }
}

const mapActions = (dispatch, props) => ({
  unlinkProduct: bindActionCreators(unlinkProduct(props.productId, props.context), dispatch),
  linkProduct: bindActionCreators(linkProduct(props.productId, props.context), dispatch)
});

export default withTaxonomy({ showLoader: false, mapActions })(TaxonomyWidget);

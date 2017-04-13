// @flow

// libs
import { get, isEqual, sortedUniqBy } from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';

// components
import Transition from 'react-addons-css-transition-group';
import WaitAnimation from 'components/common/wait-animation';
import RoundedPill from 'components/rounded-pill/rounded-pill';
import { withTaxonomy } from '../hoc';
import TaxonomyDropdown from '../taxonomy-dropdown';
import NewTaxonModal from '../taxons/new-taxon-modal';

// actions
import { deleteProductCurried as unlinkProduct } from 'modules/taxons/details/taxon';
import { addProductCurried as linkProduct } from 'modules/taxons/details/taxon';
import { create as createTaxon } from 'modules/taxons/details/taxon';

// helpers
import { transitionToLazy } from 'browserHistory';
import { getTransitionProps } from 'lib/react-utils';

// style
import styles from './taxonomy-widget.css';

// types
import type { Value } from 'components/rounded-pill/rounded-pill';

type Props = {
  title: string,
  context: string,
  productId: number,
  taxonomyId: number,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  createTaxonState: AsyncState,
  fetchTaxonomy: (id: number) => Promise<*>,
  unlinkProduct: (taxonId: number | string) => Promise<*>,
  linkProduct: (taxonId: number | string) => Promise<*>,
  createTaxon: (taxon: TaxonDraft, context: string) => Promise<*>,
  onChange: Function,
  linkedTaxonomy: LinkedTaxonomy,
};

type State = {
  showInput: boolean,
  showNewValueModal: boolean,
  linkingId: ?number,
  unlinkingId: ?number,
}

const getName = (obj: any) => get(obj, 'attributes.name.v');

const getTransitions = getTransitionProps(styles);

class TaxonomyWidget extends Component {
  props: Props;

  state: State = {
    showInput: false,
    showNewValueModal: false,
    // using local state for async actions as it is used per item in the list
    linkingId: null,
    unlinkingId: null,
  };

  @autobind
  handleDeleteClick(taxonId: Value) {
    this.setState({ unlinkingId: parseInt(taxonId) }, () => {
      this.props.unlinkProduct(taxonId)

        .then(this.props.onChange)
        .then(() => this.setState({ unlinkingId: null }));
    });
  }

  @autobind
  handleShowDropdownClick() {
    this.setState({ showInput: !this.state.showInput });
  }

  @autobind
  handleLinkClick(taxonId: Value) {
    this.setState({ linkingId: parseInt(taxonId) }, () => {
      this.props.linkProduct(taxonId)
        .then(this.props.onChange)
        .then(() => this.setState({ linkingId: null }));
    });
  }

  @autobind
  toggleShowModal() {
    this.setState({ showNewValueModal: !this.state.showNewValueModal });
  }

  @autobind
  handleSaveTaxon(taxon: TaxonDraft) {
    const { context, taxonomy, createTaxon, linkProduct, fetchTaxonomy } = this.props;

    createTaxon(taxon, context)
      .then((response: Taxon) => linkProduct(response.id))
      .then(this.props.onChange)
      .then(() => fetchTaxonomy(taxonomy.id, context))
      .then(() => this.setState({ showNewValueModal: false }));
  }

  get linkedTaxons() {
    const { taxonomy, linkedTaxonomy, context } = this.props;

    // temporary hack for hierarchical taxonomies
    const taxons = sortedUniqBy(get(linkedTaxonomy, 'taxons', []), ({ id }) => id);

    const transitionProps = getTransitions('pill', 200, true);

    return (
      <Transition {...transitionProps} key="pills">
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
    const transitionProps = getTransitions('dropdown', 100);

    return (
      <Transition {...transitionProps} key="dropdown">
        {this.state.showInput &&
        <TaxonomyDropdown
          onTaxonClick={this.handleLinkClick}
          taxonomy={this.props.taxonomy}
          linkedTaxonomy={this.props.linkedTaxonomy}
          onNewValueClick={this.toggleShowModal}
        />}
      </Transition>
    );
  }

  get newValueModal() {
    const { context, taxonomy, createTaxonState } = this.props;

    return (
      <NewTaxonModal
        isVisible={this.state.showNewValueModal}
        onCancel={this.toggleShowModal}
        onConfirm={this.handleSaveTaxon}
        context={context}
        taxonomy={taxonomy}
        asyncState={createTaxonState}
        key="modal"
      />
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
      this.newValueModal,
    ];
  }

  shouldComponentUpdate(nextProps: Props, nextState: State) {
    const lengthPath = 'linkedTaxonomy.taxons.length';

    const taxonsChanged = get(nextProps, lengthPath, 0) !== get(this.props, lengthPath);
    const taxonomyChanged = nextProps.taxonomy !== this.props.taxonomy;
    const localStateChanged = nextState !== this.state;
    const createTaxonStateChanged = !isEqual(nextProps.createTaxonState, this.props.createTaxonState);

    return taxonomyChanged || taxonsChanged || localStateChanged || createTaxonStateChanged;
  }

  render() {
    const cls = classNames(styles.taxonomies, {
      [styles._open]: this.state.showInput,
      [styles._loading]: this.state.linkingId,
    });

    return (
      <div className={cls}>
        <div className={styles.header}>
          {this.props.title}
          <button className={styles.button} onClick={this.handleShowDropdownClick}>
            <i className="icon-add" />
          </button>
        </div>
        {this.content}
      </div>
    );
  }
}

const mapState = state => {
  const createState = get(state, 'asyncActions.createTaxon', {});
  const linkState = get(state, 'asyncActions.taxonAddProduct', {});

  const createTaxonState = {
    err: createState.err || linkState.err,
    inProgress: createState.inProgress || linkState.inProgress,
    finished: createState.finished && linkState.finished,
  };

  return {
    createTaxonState,
  };
};

const mapActions = (dispatch: Function, props: Props) => ({
  unlinkProduct: bindActionCreators(unlinkProduct(props.productId, props.context), dispatch),
  linkProduct: bindActionCreators(linkProduct(props.productId, props.context), dispatch),
  createTaxon: bindActionCreators(createTaxon(props.taxonomyId), dispatch),
});

export default withTaxonomy({ showLoader: false, mapState, mapActions })(TaxonomyWidget);

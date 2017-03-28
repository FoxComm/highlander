// @flow

// libs
import { get, flow } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';
import classNames from 'classnames';

// components
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import HierarchicalTaxonomyListWidget from './taxons/hierarchical-taxonomy-widget';
import FlatTaxonomyListWidget from './taxons/flat-taxonomy-widget';
import RoundedPill from 'components/rounded-pill/rounded-pill'
import { bindActionCreators } from 'redux';

// actions
import { fetchTaxonomyInternal as fetchTaxonomy } from 'modules/taxonomies/details';
import { deleteProductCurried  } from 'modules/taxons/details/taxon';

// style
import styles from './taxonomy-widget.css';

type Props = {
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  fetchTaxonomy: (id: number | string) => Promise<*>,
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

  // @autobind
  // handleTaxonClick(id: number) {
  //   console.log(id);
  //   this.props.addProduct();
  //   console.log('i am here')
  // }
  //
  // @autobind
  // handleAddValueButton() {
  //   if (this.props.activeTaxonId !== 'new') {
  //     this.transition('new');
  //   }
  // }

  @autobind
  handleCloseClick(taxonId) {
    this.props.deleteProductCurried(taxonId)
    this.props.handleDelete(this.props.taxonomy.id, taxonId);
  }

  @autobind
  handleAddButton() {
    this.setState({ inputOpened: !this.state.inputOpened })
  }

  @autobind
  handleFocus() {
    this.setState({ isFocused: true })
  }

  @autobind
  handleBlur() {
    // this.setState({ isFocused: false })
  }

  // get renderList() {
  //   const { taxonomy, activeTaxonId} = this.props;
  //   const TaxonsWidget = taxonomy.hierarchical ? HierarchicalTaxonomyListWidget : FlatTaxonomyListWidget;
  //   const visible = this.state.isFocused;
  //   const className = classNames(styles.list, { [styles.visible]: visible });
  //
  //   return (
  //     <div className={className}>
  //       <TaxonsWidget
  //         taxons={taxonomy.taxons}
  //         activeTaxonId={activeTaxonId}
  //         handleTaxonClick={this.handleTaxonClick}
  //         getTitle={(node: Taxon) => get(node, 'attributes.name.v')}
  //       />
  //       <div styleName="footer">
  //         <AddButton className="fc-btn-primary" onClick={this.handleAddButton}>
  //           Value
  //         </AddButton>
  //       </div>
  //     </div>
  //   )
  // }

  get addedTaxons() {
    const { addedTaxons } = this.props;


    return addedTaxons.map((item) => {
      return item.taxons.map((taxon) => {
        return (
          <RoundedPill
            text={taxon.attributes.name.v}
            onClose={this.handleCloseClick}
            value={taxon.id}
            styleName="pill"
          />
        )
      });

    })
  }

  render() {
    const { taxonomy, fetchState, title } = this.props;

    if (!taxonomy || fetchState.inProgress && !fetchState.err) {
      return <div><WaitAnimation /></div>;
    }

    const opened = this.state.inputOpened;
    const inputClass = classNames(styles.input, { [styles.opened]: opened });
    const iconClassName = this.state.inputOpened ? "icon-close" : "icon-add";

    return (
      <div styleName="root">
        <div styleName="header">
          <span styleName="title">
            {title}
          </span>
          <span styleName="button">
            <i className={iconClassName} onClick={this.handleAddButton}/>
          </span>
        </div>
        <div className={inputClass}>
          <input
            onFocus={this.handleFocus}
            onBlur={this.handleBlur}
            onChange={() => console.log('display search')}
          />
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

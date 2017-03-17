// @flow

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';

// components
import { AddButton } from 'components/common/buttons';

// actions
import * as TaxonomyActions from 'modules/taxonomies/details';
import * as TaxonActions from 'modules/taxons/details';

// style
import styles from './taxon-list-widget.css';

class TaxonListWidget extends Component {

  componentDidMount() {
    const { id, context } = this.props;
    this.props.fetchTaxonomy(id, context);
  }

  transition(id: number|string) {
    transitionTo('value-details', {
      taxonomyId: this.props.id,
      context: this.props.context,
      taxonId: id
    });
  }

  handleTaxonClick(id: string) {
    const { fetchTaxon, context, currentTaxon } = this.props;

    if (currentTaxon !== id.toString()) {
      this.transition(id);
      fetchTaxon(id, context);
    }
  }

  handleAddButton() {
    if (this.props.currentTaxon !== 'new') {
      this.props.addNewValue();
      this.transition('new');
    }
  }

  renderChildren() {
    const { taxons } = this.props.taxonomy;
    const children = taxons.map((item) => {
      const styleName = this.props.currentTaxon == item.taxon.id ? 'item-current' : 'item';

       return (
         <div
           styleName={styleName}
           onClick={() => this.handleTaxonClick(item.taxon.id)}
           key={item.taxon.id}
         >
           <span styleName="rectangle"></span>
           <div styleName="text">
             {item.taxon.attributes.name.v}
           </div>
         </div>
         );
       }
    );
    return children;
  }

  render () {
    const { taxonomy } = this.props;

    if (!this.props.taxonomy || this.props.taxonomy.hierarchical) {
      return <div></div>;
    }

    return (
      <div styleName="taxon-list">
        <div styleName="header">
          {taxonomy.attributes.name.v}
        </div>
        <div styleName="items">
          {this.renderChildren()}
        </div>
        <div styleName="footer">
          <AddButton className="fc-btn-primary" onClick={this.handleAddButton}>
            Value
          </AddButton>
        </div>
      </div>
    );
  }

}

const mapState = ( {taxonomies: { details } }) => ({
  taxonomy: details.taxonomy
});

const mapDispatch = (dispatch) => ({
    fetchTaxonomy: (id: string, context: string ) => dispatch(TaxonomyActions.fetch(id, context)),
    fetchTaxon: (id: string, context: string) => dispatch(TaxonActions.fetch(id, context)),
    addNewValue: () => dispatch(TaxonActions.reset())
});

export default connect(mapState, mapDispatch)(TaxonListWidget);

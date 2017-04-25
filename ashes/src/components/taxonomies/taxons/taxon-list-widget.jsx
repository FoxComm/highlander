// @flow

// libs
import { get } from 'lodash';
import React, { Component } from 'react';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';

// components
import { AddButton } from 'components/core/button';
import HierarchicalTaxonomyListWidget from './hierarchical-taxonomy-widget';
import FlatTaxonomyListWidget from './flat-taxonomy-widget';
import { withTaxonomy } from '../hoc';

// style
import styles from './taxon-list-widget.css';

type Props = {
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
};

class TaxonListWidget extends Component {
  props: Props;

  transition(id: number | string) {
    transitionTo('taxon-details', {
      taxonomyId: this.props.taxonomy.id,
      context: this.props.context,
      taxonId: id
    });
  }

  @autobind
  handleTaxonClick(id: number) {
    if (this.props.activeTaxonId !== id.toString()) {
      this.transition(id);
    }
  }

  @autobind
  handleAddButton() {
    if (this.props.activeTaxonId !== 'new') {
      this.transition('new');
    }
  }

  render() {
    const { taxonomy, activeTaxonId } = this.props;

    const TaxonsWidget = taxonomy.hierarchical ? HierarchicalTaxonomyListWidget : FlatTaxonomyListWidget;

    return (
      <div styleName="root">
        <div styleName="header">
          {get(taxonomy, 'attributes.name.v')}
        </div>
        <TaxonsWidget
          taxons={taxonomy.taxons}
          activeTaxonId={activeTaxonId}
          onClick={this.handleTaxonClick}
          getTitle={(node: Taxon) => get(node, 'attributes.name.v')}
        />
        <div styleName="footer">
          <AddButton className="fc-btn-primary" onClick={this.handleAddButton}>
            Value
          </AddButton>
        </div>
      </div>
    );
  }
}

export default withTaxonomy()(TaxonListWidget);

// @flow

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import { get } from 'lodash';

// components
import { AddButton } from 'components/common/buttons';
import WaitAnimation from 'components/common/wait-animation';
import HierarchicalTaxonomyListWidget from './hierarchical-taxonomy-widget';
import FlatTaxonomyListWidget from './flat-taxonomy-widget';

// actions
import { fetch as fetchTaxonomy } from 'modules/taxonomies/details';

// style
import styles from './taxon-list-widget.css';

type Props = {
  context: string,
  taxonomyId: string,
  activeTaxonId: string,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  fetchTaxonomy: (id: number | string) => Promise<*>,
};

class TaxonListWidget extends Component {
  props: Props;

  componentDidMount() {
    const { taxonomy, taxonomyId, context } = this.props;

    if (!taxonomy) {
      this.props.fetchTaxonomy(taxonomyId, context);
    }
  }

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
    const { taxonomy, fetchState, activeTaxonId } = this.props;

    if (fetchState.inProgress && !fetchState.err) {
      return <div><WaitAnimation /></div>;
    }

    const TaxonsWidget = taxonomy.hierarchical ? HierarchicalTaxonomyListWidget : FlatTaxonomyListWidget;

    return (
      <div styleName="root">
        <div styleName="header">
          {get(taxonomy, 'attributes.name.v')}
        </div>
        <TaxonsWidget
          taxons={taxonomy.taxons}
          activeTaxonId={activeTaxonId}
          handleTaxonClick={this.handleTaxonClick}
          getTitle={(node: TaxonNode) => get(node, 'attributes.name.v')}
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

const mapState = state => ({
  taxonomy: get(state.taxonomies, 'details.taxonomy', {}),
  fetchState: get(state.asyncActions, 'fetchTaxonomy', {})
});

export default connect(mapState, { fetchTaxonomy })(TaxonListWidget);

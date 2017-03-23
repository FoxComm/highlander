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

class TaxonListWidget extends Component {

  componentDidMount() {
    const { id, context } = this.props;
    this.props.fetchTaxonomy(id, context);
  }

  transition(id: number|string) {
    transitionTo('taxon-details', {
      taxonomyId: this.props.id,
      context: this.props.context,
      taxonId: id
    });
  }

  @autobind
  handleTaxonClick(id: number) {
    const { currentTaxon } = this.props;

    if (currentTaxon !== id.toString()) {
      this.transition(id);
    }
  }

  @autobind
  handleAddButton() {
    if (this.props.currentTaxon !== 'new') {
      this.transition('new');
    }
  }

  get content() {
    const { taxonomy: { taxons, hierarchical }, currentTaxon } = this.props;

    if (!hierarchical) {

      return (
        <FlatTaxonomyListWidget
          taxons={taxons}
          currentTaxon={currentTaxon}
          handleTaxonClick={this.handleTaxonClick}
        />
      );
    }

    if (hierarchical) {

      return (
        <HierarchicalTaxonomyListWidget
          taxons={taxons}
          currentTaxon={currentTaxon}
          handleTaxonClick={this.handleTaxonClick}
        />
      );
    }

  }

  render () {
    const { taxonomy, fetchState } = this.props;

    if (fetchState.inProgress && !fetchState.err) {
      return <div><WaitAnimation /></div>;
    }

    return (
      <div styleName="root">
        <div styleName="header">
          {taxonomy.attributes.name.v}
        </div>
          {this.content}
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

// @flow

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// components
import { AddButton } from 'components/common/buttons';
import TreeNode from './tree-node';

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
  handleTaxonClick(id: number, event) {
    { event && event.stopPropagation(); }

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
    const { taxons } = this.props.taxonomy;

    if (!this.props.taxonomy.hierarchical) {
      return taxons.map((item) => {
          const active = (this.props.currentTaxon === item.taxon.id.toString());
          const className = classNames(styles['item'], { [styles.active]: active });

          return (
            <div
              className={className}
              onClick={() => this.handleTaxonClick(item.taxon.id)}
              key={item.taxon.id}
            >
              {item.taxon.attributes.name.v}
            </div>
          );
        }
      );

    }

    if (this.props.taxonomy.hierarchical) {

      return taxons.map((item) => {
        return (
            <div key={item.taxon.id}>
              <TreeNode
                node={item}
                visible={true}
                depth={20}
                handleClick={this.handleTaxonClick}
                currentObject={this.props.currentTaxon}
              />
            </div>
        );
      });
    }

  }

  render () {
    const { taxonomy } = this.props;

    if (!taxonomy) {
      return null;
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

const mapState = ( {taxonomies: { details } }) => ({
  taxonomy: details.taxonomy
});

export default connect(mapState, { fetchTaxonomy })(TaxonListWidget);
